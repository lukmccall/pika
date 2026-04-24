@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.lukmccall.pika.bytecode

import io.github.lukmccall.pika.Identifiers
import io.github.lukmccall.pika.Identifiers.removePackageName
import io.github.lukmccall.pika.Identifiers.withPackageName
import io.github.lukmccall.pika.ir.hasIntrospectableAnnotation
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.mapping.IrTypeMapper
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.LdcInsnNode

class BytecodePoet(
  private val adapter: InstructionAdapter,
  private val irPluginContext: IrPluginContext,
  private val typeMapper: IrTypeMapper,
  private val extraAnnotationClassIds: Set<ClassId> = emptySet(),
) {
  // The multi dollar syntax isn't available in kotlin 2.1.20
  @Suppress("CanConvertToMultiDollarString")
  companion object {
    private val pTypeDescriptorConcreteType: Type =
      Type.getObjectType("${Identifiers.PACKAGE_NAME_JAVE_NOTATION}/PTypeDescriptor\$Concrete")
    private val pTypeDescriptorParameterizedType: Type =
      Type.getObjectType("${Identifiers.PACKAGE_NAME_JAVE_NOTATION}/PTypeDescriptor\$Concrete\$Parameterized")
    private val pTypeDescriptorRegistryType: Type =
      Type.getObjectType("${Identifiers.PACKAGE_NAME_JAVE_NOTATION}/${Identifiers.TYPE_DESCRIPTOR_REGISTRY_CLASS}")
    private val pTypeDescriptorStarType: Type =
      Type.getObjectType("${Identifiers.PACKAGE_NAME_JAVE_NOTATION}/PTypeDescriptor\$Star")
    private val javaLangClassType: Type = Type.getObjectType("java/lang/Class")

    val pTypeDescriptorType: Type = Type.getObjectType("${Identifiers.PACKAGE_NAME_JAVE_NOTATION}/PTypeDescriptor")
    val pIntrospectionDataType: Type = Type.getObjectType("${Identifiers.PACKAGE_NAME_JAVE_NOTATION}/PIntrospectionData")
  }

  private fun simpleTypeFieldName(irClass: IrClass, isNullable: Boolean): String? {
    val fqName = irClass.kotlinFqName.asString()
    val (nonNullable, nullable) = Identifiers.SIMPLE_TYPE_FIELD_NAMES[fqName] ?: return null
    return if (isNullable) {
      nullable
    } else {
      nonNullable
    }
  }

  /**
   * PTypeDescriptorRegistry.getOrCreateConcrete({Class<?>}, {isNullable}, {introspection})
   */
  fun initConcretePTypeDescriptor(
    irClass: IrClass,
    isNullable: Boolean
  ) = adapter.apply {
    val fieldName = simpleTypeFieldName(irClass, isNullable)
    if (fieldName != null) {
      getstatic(
        pTypeDescriptorRegistryType.internalName,
        fieldName,
        pTypeDescriptorConcreteType.descriptor
      )
      return@apply
    }

    aconst(irClass.toGeneric())
    iconst(if (isNullable) 1 else 0)
    val pushed = irClass.hasIntrospectableAnnotation(extraAnnotationClassIds) && initPIntrospectionData(irClass)
    if (!pushed) aconst(null)
    invokestatic(
      pTypeDescriptorRegistryType.internalName,
      Identifiers.TYPE_DESCRIPTOR_REGISTRY_GET_OR_CREATE_CONCRETE,
      "(${javaLangClassType.descriptor}Z${pIntrospectionDataType.descriptor})${pTypeDescriptorConcreteType.descriptor}",
      false
    )
  }

  /**
   * PTypeDescriptorRegistry.getOrCreateParameterized({Class<?>}, {isNullable}, [*{parameters}].asList(), {introspection})
   */
  fun initParameterizedPTypeDescriptor(
    irClass: IrClass,
    isNullable: Boolean,
    parameters: List<IrTypeArgument>
  ) = adapter.apply {
    aconst(irClass.toGeneric())
    iconst(if (isNullable) 1 else 0)

    // Build PTypeDescriptor[] then wrap with Arrays.asList - avoids toList() copy in registry
    iconst(parameters.size)
    newarray(pTypeDescriptorType)

    parameters.forEachIndexed { index, arg ->
      dup()
      iconst(index)
      when (arg) {
        is IrTypeProjection -> initPTypeDescriptor(arg.type)
        is IrStarProjection -> getstatic(pTypeDescriptorStarType.internalName, "INSTANCE", pTypeDescriptorStarType.descriptor)
      }
      astore(pTypeDescriptorType)
    }

    invokestatic("java/util/Arrays", "asList", "([Ljava/lang/Object;)Ljava/util/List;", false)

    val pushed = irClass.hasIntrospectableAnnotation(extraAnnotationClassIds) && initPIntrospectionData(irClass)
    if (!pushed) {
      aconst(null)
    }

    invokestatic(
      pTypeDescriptorRegistryType.internalName,
      Identifiers.TYPE_DESCRIPTOR_REGISTRY_GET_OR_CREATE_PARAMETERIZED,
      "(${javaLangClassType.descriptor}ZLjava/util/List;${pIntrospectionDataType.descriptor})${pTypeDescriptorParameterizedType.descriptor}",
      false
    )
  }

  fun initPIntrospectionData(irClass: IrClass): Boolean {
    val fieldDescriptor = pIntrospectionDataType.descriptor
    return if (irClass.isObject && !irClass.isCompanion) {
      val classType = typeMapper.mapTypeCommon(irClass.defaultType, TypeMappingMode.DEFAULT)
      adapter.getstatic(
        classType.internalName,
        Identifiers.INTROSPECTION_DATA_FIELD_NAME,
        fieldDescriptor
      )
      true
    } else {
      val companion = irClass.companionObject() ?: return false
      val companionType = typeMapper.mapTypeCommon(companion.defaultType, TypeMappingMode.DEFAULT)
      adapter.getstatic(
        companionType.internalName,
        Identifiers.INTROSPECTION_DATA_FIELD_NAME,
        fieldDescriptor
      )
      true
    }
  }

  private fun initFallbackPTypeDescriptor() =
    initConcretePTypeDescriptor(irPluginContext.irBuiltIns.anyClass.owner, false)

  /**
   * new PTypeDescriptor.Concrete or PTypeDescriptor.Concrete.Parameterized
   */
  fun initPTypeDescriptor(
    type: IrType
  ) {
    val simpleType = type as? IrSimpleType
    if (simpleType == null) {
      initFallbackPTypeDescriptor()
      return
    }

    val irClass = simpleType.classOrNull?.owner
    if (irClass == null) {
      initFallbackPTypeDescriptor()
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
    if (functionName != Identifiers.TYPE_DESCRIPTOR_OF_FUNCTION_NAME &&
      functionName != Identifiers.IS_INTROSPECTABLE_FUNCTION_NAME &&
      functionName != Identifiers.INTROSPECTION_OF_FUNCTION_NAME
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
