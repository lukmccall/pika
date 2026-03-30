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
import org.jetbrains.kotlin.ir.util.kotlinFqName
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
    private val typeInfoSimpleType: Type = Type.getObjectType("io/github/lukmccall/pika/TypeInfo\$Simple")
    private val typeInfoParameterizedType: Type =
      Type.getObjectType("io/github/lukmccall/pika/TypeInfo\$Parameterized")
    private val typeInfoStarType: Type = Type.getObjectType("io/github/lukmccall/pika/TypeInfo\$Star")
    private val listType: Type = Type.getObjectType("java/util/List")

    val typeInfoType: Type = Type.getObjectType("io/github/lukmccall/pika/TypeInfo")
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
   * new TypeInfo.Simple({typeName}, {kClass}, {isNullable})
   */
  fun initSimpleTypeInfo(
    typeName: String,
    irClass: IrClass,
    isNullable: Boolean
  ) = adapter.apply {
    anew(typeInfoSimpleType)
    dup()
    aconst(typeName)
    aconst(irClass.toGeneric())
    AsmUtil.wrapJavaClassIntoKClass(this)
    iconst(if (isNullable) 1 else 0)
    invokespecial(
      typeInfoSimpleType.internalName,
      "<init>",
      "(Ljava/lang/String;${AsmTypes.K_CLASS_TYPE.descriptor}Z)V",
      false
    )
  }

  /**
   * new TypeInfo.Parameterized({typeName}, {kClass}, {isNullable}, initTypeInfo(*{typeArguments}))
   */
  fun initParameterizedTypeInfo(
    typeName: String,
    irClass: IrClass,
    isNullable: Boolean,
    typeArguments: List<IrTypeArgument>
  ) = adapter.apply {
    anew(typeInfoParameterizedType)
    dup()
    aconst(typeName)
    aconst(irClass.toGeneric())
    AsmUtil.wrapJavaClassIntoKClass(this)
    iconst(if (isNullable) 1 else 0)

    // Create list of type arguments
    iconst(typeArguments.size)
    newarray(typeInfoType)

    typeArguments.forEachIndexed { index, arg ->
      dup()
      iconst(index)
      when (arg) {
        is IrTypeProjection -> initTypeInfo(arg.type)
        is IrStarProjection -> {
          // TypeInfo.Star is an object
          getstatic(typeInfoStarType.internalName, "INSTANCE", typeInfoStarType.descriptor)
        }
      }
      astore(typeInfoType)
    }

    // Call kotlin.collections.ArraysKt.asList()
    invokestatic(
      "kotlin/collections/ArraysKt",
      "asList",
      "([Ljava/lang/Object;)${listType.descriptor}",
      false
    )

    invokespecial(
      typeInfoParameterizedType.internalName,
      "<init>",
      "(Ljava/lang/String;${AsmTypes.K_CLASS_TYPE.descriptor}Z${listType.descriptor})V",
      false
    )
  }

  /**
   * new TypeInfo.Simple or TypeInfo.Parameterized
   */
  fun initTypeInfo(
    type: IrType
  ) {
    val simpleType = type as? IrSimpleType
    if (simpleType == null) {
      initSimpleTypeInfo(
        "Unknown",
        irPluginContext.irBuiltIns.anyClass.owner,
        false
      )
      return
    }

    val irClass = simpleType.classOrNull?.owner
    if (irClass == null) {
      initSimpleTypeInfo(
        "Unknown",
        irPluginContext.irBuiltIns.anyClass.owner,
        false
      )
      return
    }

    val typeName = irClass.kotlinFqName.asString()
    val isNullable = simpleType.isMarkedNullable()

    if (simpleType.arguments.isEmpty()) {

      initSimpleTypeInfo(
        typeName,
        irClass,
        isNullable
      )
    } else {
      initParameterizedTypeInfo(
        typeName,
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
    typeDescriptor: TypeParameterMarker
  ) = adapter.apply {
    // Type is a type parameter - need to emit reified operation marker
    ReifiedTypeInliner.putReifiedOperationMarkerIfNeeded(
      typeDescriptor,
      type.isMarkedNullable(),
      ReifiedTypeInliner.OperationKind.TYPE_OF,
      adapter,
      typeSystem
    )
    // Call throwNonReifiedTypeError() which throws at runtime if not inlined.
    // When inlined, removeReifyMarker() will remove this instruction.
    invokestatic(
      "io/github/lukmccall/pika/TypeInfoKt",
      "throwNonReifiedTypeError",
      "()Lio/github/lukmccall/pika/TypeInfo;",
      false
    )
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
    if (functionName != Identifiers.TYPE_INFO_FUNCTION_NAME && functionName != Identifiers.FULL_TYPE_INFO_FUNCTION_NAME) {
      return null
    }

    // Remove the marker instructions: throw, marker string, voidMagicApiCall
    val throwNonReifiedTypeInsn = reifiedInsn
    val voidMagicCallInsn = markerStringInsn.next

    instructions.remove(voidMagicCallInsn)
    instructions.remove(markerStringInsn)
    instructions.remove(throwNonReifiedTypeInsn)

    return functionName
  }

  private fun IrClass.toGeneric(): Type {
    return typeMapper.mapTypeCommon(defaultType, TypeMappingMode.GENERIC_ARGUMENT)
  }
}