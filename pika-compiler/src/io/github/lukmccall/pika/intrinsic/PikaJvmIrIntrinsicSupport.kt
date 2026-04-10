@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.lukmccall.pika.intrinsic

import io.github.lukmccall.pika.Identifiers
import io.github.lukmccall.pika.Identifiers.withPackageName
import io.github.lukmccall.pika.bytecode.BytecodePoet
import io.github.lukmccall.pika.ir.hasIntrospectableAnnotation
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.backend.jvm.intrinsics.IntrinsicMethod
import org.jetbrains.kotlin.codegen.extractUsedReifiedParameters
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList

/**
 * JVM intrinsic support for pTypeDescriptorOf<T>() and pIsIntrospectable(instance) functions.
 *
 * This is necessary because IrGenerationExtension runs BEFORE inline functions are inlined.
 * When these functions are called through a reified inline proxy like:
 *
 * ```kotlin
 * inline fun <reified T> proxy() = pTypeDescriptorOf<T>()
 * inline fun <reified T: Any> proxyCheck(instance: T) = pIsIntrospectable(instance)
 * ```
 *
 * At IR generation time, the type argument inside the proxy still has T as a type parameter.
 * The intrinsic extension handles two cases:
 *
 * 1. **Concrete type (post-inlining or direct call)**: generates a constant value directly.
 * 2. **Type parameter (inside a reified inline function)**: inserts a reify marker so the
 *    Kotlin inliner substitutes the concrete type when the proxy is called, and then
 *    rewritePluginDefinedOperationMarker generates the constant.
 */
class PikaJvmIrIntrinsicSupport(
  jvmBackendContext: JvmBackendContext,
  private val irPluginContext: IrPluginContext
) : JvmIrIntrinsicExtension {
  private val typeSystemContext = jvmBackendContext.typeSystem
  private val typeMapper = jvmBackendContext.defaultTypeMapper

  override fun getIntrinsic(symbol: IrFunctionSymbol): IntrinsicMethod? {
    val functionName = isTargetMethod(symbol) ?: return null
    return when (functionName) {
      Identifiers.P_IS_INTROSPECTABLE_FUNCTION_NAME -> PIsIntrospectableIntrinsicMethod()
      else -> PTypeDescriptorIntrinsicMethod(functionName)
    }
  }

  override fun rewritePluginDefinedOperationMarker(
    v: InstructionAdapter,
    reifiedInsn: AbstractInsnNode,
    instructions: InsnList,
    type: IrType
  ): Boolean {
    val bytecodePoet = createBytecodePoet(adapter = v)

    val functionName = bytecodePoet.removeReifyMarker(reifiedInsn, instructions) ?: return false

    when (functionName) {
      Identifiers.P_IS_INTROSPECTABLE_FUNCTION_NAME -> {
        val irClass = (type as? IrSimpleType)?.classOrNull?.owner
        val isIntrospectable = irClass?.hasIntrospectableAnnotation() ?: false
        v.iconst(if (isIntrospectable) 1 else 0)
      }

      else -> generatePTypeDescriptor(type, bytecodePoet, functionName)
    }

    return true
  }

  private fun isTargetMethod(symbol: IrFunctionSymbol): String? {
    val function = symbol.owner
    val fqName = function.fqNameWhenAvailable?.asString() ?: return null

    return when (fqName) {
      Identifiers.P_TYPE_DESCRIPTOR_OF_FUNCTION_NAME.withPackageName() -> Identifiers.P_TYPE_DESCRIPTOR_OF_FUNCTION_NAME
      "PTypeDescriptorOfKt.${Identifiers.P_TYPE_DESCRIPTOR_OF_FUNCTION_NAME}".withPackageName() -> Identifiers.P_TYPE_DESCRIPTOR_OF_FUNCTION_NAME
      Identifiers.P_IS_INTROSPECTABLE_FUNCTION_NAME.withPackageName() -> Identifiers.P_IS_INTROSPECTABLE_FUNCTION_NAME
      "PIsIntrospectableKt.${Identifiers.P_IS_INTROSPECTABLE_FUNCTION_NAME}".withPackageName() -> Identifiers.P_IS_INTROSPECTABLE_FUNCTION_NAME
      else -> null
    }
  }

  inner class PIsIntrospectableIntrinsicMethod : IntrinsicMethod() {
    override fun invoke(
      expression: IrFunctionAccessExpression,
      codegen: ExpressionCodegen,
      data: BlockInfo
    ): PromisedValue {
      val bytecodePoet = createBytecodePoet(adapter = codegen.mv)

      val typeArg = expression.typeArguments[0]!!

      with(codegen) {
        expression.markLineNumber(startOffset = true)
      }

      // Check if the type argument is still a type parameter (inside a reified inline proxy body).
      // If so, insert a reify marker — the Kotlin inliner will substitute the concrete type when
      // the proxy is called, and rewritePluginDefinedOperationMarker will emit the constant.
      val typeDescriptor = with(typeSystemContext) {
        typeArg.typeConstructor().getTypeParameterClassifier()
      }

      if (typeDescriptor != null) {
        bytecodePoet.reifyMarker(
          typeSystemContext,
          typeArg,
          Identifiers.P_IS_INTROSPECTABLE_FUNCTION_NAME,
          typeDescriptor,
          "io/github/lukmccall/pika/PIsIntrospectableKt",
          "throwNonReifiedPIsIntrospectableError",
          "()Z"
        )
        codegen.propagateChildReifiedTypeParametersUsages(
          codegen.typeMapper.typeSystem.extractUsedReifiedParameters(typeArg)
        )
        return MaterialValue(codegen, Type.BOOLEAN_TYPE, expression.type)
      }

      // Concrete type: emit constant directly.
      val irClass = (typeArg as? IrSimpleType)?.classOrNull?.owner
      val isIntrospectable = irClass?.hasIntrospectableAnnotation() ?: false
      codegen.mv.iconst(if (isIntrospectable) 1 else 0)
      return MaterialValue(codegen, Type.BOOLEAN_TYPE, expression.type)
    }
  }

  inner class PTypeDescriptorIntrinsicMethod(private val functionName: String) : IntrinsicMethod() {
    override fun invoke(
      expression: IrFunctionAccessExpression,
      codegen: ExpressionCodegen,
      data: BlockInfo
    ): PromisedValue {
      val bytecodePoet = createBytecodePoet(adapter = codegen.mv)

      val typeArgument = expression.typeArguments[0]!!

      with(codegen) {
        expression.markLineNumber(startOffset = true)

        generatePTypeDescriptor(typeArgument, bytecodePoet, functionName)
      }

      codegen.propagateChildReifiedTypeParametersUsages(
        codegen.typeMapper.typeSystem.extractUsedReifiedParameters(typeArgument)
      )

      return MaterialValue(codegen, BytecodePoet.pTypeDescriptorType, expression.type)
    }
  }

  private fun generatePTypeDescriptor(type: IrType, bytecodePoet: BytecodePoet, functionName: String) {
    val typeDescriptor = with(typeSystemContext) {
      type.typeConstructor().getTypeParameterClassifier()
    }

    if (typeDescriptor != null) {
      bytecodePoet.reifyMarker(
        typeSystemContext,
        type,
        functionName,
        typeDescriptor,
        "io/github/lukmccall/pika/PTypeDescriptorOfKt",
        "throwNonReifiedPTypeDescriptorError",
        "()Lio/github/lukmccall/pika/PTypeDescriptor;"
      )
      return
    }

    with(bytecodePoet) {
      when (functionName) {
        Identifiers.P_TYPE_DESCRIPTOR_OF_FUNCTION_NAME -> initPTypeDescriptor(type)
      }
    }
  }

  private fun createBytecodePoet(adapter: InstructionAdapter): BytecodePoet = BytecodePoet(
    adapter,
    irPluginContext,
    typeMapper
  )
}
