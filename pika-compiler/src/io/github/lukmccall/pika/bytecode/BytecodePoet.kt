@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.lukmccall.pika.bytecode

import io.github.lukmccall.pika.Identifiers
import io.github.lukmccall.pika.Identifiers.removePackageName
import io.github.lukmccall.pika.Identifiers.withPackageName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.mapping.IrTypeMapper
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.LdcInsnNode

class BytecodePoet(
  private val adapter: InstructionAdapter,
  private val irPluginContext: IrPluginContext,
  private val typeMapper: IrTypeMapper
) {
  // The multi dollar syntax isn't available in kotlin 2.1.20
  @Suppress("CanConvertToMultiDollarString")
  companion object {
    private val pTypeType: Type = Type.getObjectType("io/github/lukmccall/pika/PType")
    private val pTypeDescriptorConcreteType: Type =
      Type.getObjectType("io/github/lukmccall/pika/PTypeDescriptor\$Concrete")
    private val pTypeDescriptorParameterizedType: Type =
      Type.getObjectType("io/github/lukmccall/pika/PTypeDescriptor\$Concrete\$Parameterized")
    private val pTypeDescriptorStarType: Type =
      Type.getObjectType("io/github/lukmccall/pika/PTypeDescriptor\$Star")
    private val listType: Type = Type.getObjectType("java/util/List")

    val pTypeDescriptorType: Type = Type.getObjectType("io/github/lukmccall/pika/PTypeDescriptor")
  }

  /**
   * throw NotImplementedError({message})
   */
  fun throwNotImplementedError(
    message: String,
  ) = adapter.apply {
    anew(Type.getObjectType("kotlin/NotImplementedError"))
    dup()
    aconst(message)
    invokespecial(
      "kotlin/NotImplementedError",
      "<init>",
      "(Ljava/lang/String;)V",
      false
    )
    checkcast(Type.getObjectType("java/lang/Throwable"))
    athrow()
  }

  /**
   * new PType({kClass})
   */
  private fun initPType(
    irClass: IrClass
  ) = adapter.apply {
    anew(pTypeType)
    dup()
    aconst(irClass.toGeneric())
    AsmUtil.wrapJavaClassIntoKClass(this)
    invokespecial(
      pTypeType.internalName,
      "<init>",
      "(${AsmTypes.K_CLASS_TYPE.descriptor})V",
      false
    )
  }

  /**
   * new PTypeDescriptor.Concrete({PType}, {isNullable})
   */
  fun initConcretePTypeDescriptor(
    irClass: IrClass,
    isNullable: Boolean
  ) = adapter.apply {
    anew(pTypeDescriptorConcreteType)
    dup()
    initPType(irClass)
    iconst(if (isNullable) 1 else 0)
    invokespecial(
      pTypeDescriptorConcreteType.internalName,
      "<init>",
      "(${pTypeType.descriptor}Z)V",
      false
    )
  }

  /**
   * new PTypeDescriptor.Concrete.Parameterized({PType}, {isNullable}, initPTypeDescriptor(*{argumentsPTypes}))
   */
  fun initParameterizedPTypeDescriptor(
    irClass: IrClass,
    isNullable: Boolean,
    argumentsPTypes: List<IrTypeArgument>
  ) = adapter.apply {
    anew(pTypeDescriptorParameterizedType)
    dup()
    initPType(irClass)
    iconst(if (isNullable) 1 else 0)

    // Create list of type arguments
    iconst(argumentsPTypes.size)
    newarray(pTypeDescriptorType)

    argumentsPTypes.forEachIndexed { index, arg ->
      dup()
      iconst(index)
      when (arg) {
        is IrTypeProjection -> initPTypeDescriptor(arg.type)
        is IrStarProjection -> {
          // PTypeDescriptor.Star is an object
          getstatic(pTypeDescriptorStarType.internalName, "INSTANCE", pTypeDescriptorStarType.descriptor)
        }
      }
      astore(pTypeDescriptorType)
    }

    // Call kotlin.collections.ArraysKt.asList()
    invokestatic(
      "kotlin/collections/ArraysKt",
      "asList",
      "([Ljava/lang/Object;)${listType.descriptor}",
      false
    )

    invokespecial(
      pTypeDescriptorParameterizedType.internalName,
      "<init>",
      "(${pTypeType.descriptor}Z${listType.descriptor})V",
      false
    )
  }

  /**
   * new PTypeDescriptor.Concrete or PTypeDescriptor.Concrete.Parameterized
   */
  fun initPTypeDescriptor(
    type: IrType
  ) {
    val simpleType = type as? IrSimpleType
    if (simpleType == null) {
      initConcretePTypeDescriptor(
        irPluginContext.irBuiltIns.anyClass.owner,
        false
      )
      return
    }

    val irClass = simpleType.classOrNull?.owner
    if (irClass == null) {
      initConcretePTypeDescriptor(
        irPluginContext.irBuiltIns.anyClass.owner,
        false
      )
      return
    }

    val isNullable = simpleType.isMarkedNullable()

    if (simpleType.arguments.isEmpty()) {
      initConcretePTypeDescriptor(
        irClass,
        isNullable
      )
    } else {
      initParameterizedPTypeDescriptor(
        irClass,
        isNullable,
        simpleType.arguments
      )
    }
  }

  fun reifyMarker(
    typeSystem: IrTypeSystemContext,
    type: IrType,
    functionName: String,
    typeDescriptor: TypeParameterMarker,
    throwOwner: String,
    throwMethod: String,
    throwDescriptor: String
  ) = adapter.apply {
    // Type is a type parameter - need to emit reified operation marker
    ReifiedTypeInliner.putReifiedOperationMarkerIfNeeded(
      typeDescriptor,
      type.isMarkedNullable(),
      ReifiedTypeInliner.OperationKind.TYPE_OF,
      adapter,
      typeSystem
    )
    // Call the throw function which throws at runtime if not inlined.
    // When inlined, removeReifyMarker() will remove this instruction.
    invokestatic(throwOwner, throwMethod, throwDescriptor, false)
    // Emit plugin-specific marker so rewritePluginDefinedOperationMarker knows to handle this
    // (this is dead code if not inlined, but the inliner uses it to find our marker)
    aconst(functionName.withPackageName())
    invokestatic(
      ReifiedTypeInliner.pluginIntrinsicsMarkerOwner,
      ReifiedTypeInliner.pluginIntrinsicsMarkerMethod,
      ReifiedTypeInliner.pluginIntrinsicsMarkerSignature,
      false
    )
  }

  fun removeReifyMarker(
    reifiedInsn: AbstractInsnNode,
    instructions: InsnList
  ): String? {
    val markerStringInsn = reifiedInsn.next as? LdcInsnNode ?: return null
    val markerString = markerStringInsn.cst as? String ?: return null
    if (!markerString.startsWith(Identifiers.PACKAGE_NAME)) {
      return null
    }

    val functionName = markerString.removePackageName()
    if (functionName != Identifiers.P_TYPE_DESCRIPTOR_OF_FUNCTION_NAME &&
      functionName != Identifiers.P_IS_INTROSPECTABLE_FUNCTION_NAME
    ) {
      return null
    }

    // Remove the marker instructions: throw, marker string, voidMagicApiCall
    val throwNonReifiedPTypeDescriptorInsn = reifiedInsn
    val voidMagicCallInsn = markerStringInsn.next

    instructions.remove(voidMagicCallInsn)
    instructions.remove(markerStringInsn)
    instructions.remove(throwNonReifiedPTypeDescriptorInsn)

    return functionName
  }

  private fun IrClass.toGeneric(): Type {
    return typeMapper.mapTypeCommon(defaultType, TypeMappingMode.GENERIC_ARGUMENT)
  }
}
