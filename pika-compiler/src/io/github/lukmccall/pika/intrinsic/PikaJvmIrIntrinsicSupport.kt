@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.lukmccall.pika.intrinsic

import io.github.lukmccall.pika.Identifiers
import io.github.lukmccall.pika.Identifiers.withPackageName
import io.github.lukmccall.pika.bytecode.BytecodePoet
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.backend.jvm.intrinsics.IntrinsicMethod
import org.jetbrains.kotlin.codegen.extractUsedReifiedParameters
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList

/**
 * JVM intrinsic support for typeInfo<T>() and fullTypeInfo<T>() functions.
 *
 * This is necessary because IrGenerationExtension runs BEFORE inline functions are inlined.
 * When typeInfo<T>() is called through an inline proxy function like:
 *
 * ```kotlin
 * inline fun <reified T> proxy() = typeInfo<T>()
 * fun main() { println(proxy<String>()) }
 * ```
 *
 * At IR generation time, typeInfo<T>() inside proxy still has T as a type parameter.
 * The intrinsic extension runs during codegen AFTER inlining, so it sees the concrete type String.
 */
class PikaJvmIrIntrinsicSupport(
  jvmBackendContext: JvmBackendContext,
  private val irPluginContext: IrPluginContext
) : JvmIrIntrinsicExtension {
  private val typeSystemContext = jvmBackendContext.typeSystem
  private val typeMapper = jvmBackendContext.defaultTypeMapper

  override fun getIntrinsic(symbol: IrFunctionSymbol): IntrinsicMethod? {
    val functionName = isTargetMethod(symbol) ?: return null
    return TypeInfoIntrinsicMethod(functionName)
  }


  override fun rewritePluginDefinedOperationMarker(
    v: InstructionAdapter,
    reifiedInsn: AbstractInsnNode,
    instructions: InsnList,
    type: IrType
  ): Boolean {
    val bytecodePoet = createBytecodePoet(adapter = v)

    val functionName = bytecodePoet.removeReifyMarker(reifiedInsn, instructions) ?: return false
    generateTypeInfo(type, bytecodePoet, functionName)

    return true
  }

  private fun isTargetMethod(symbol: IrFunctionSymbol): String? {
    val function = symbol.owner
    val fqName = function.fqNameWhenAvailable?.asString() ?: return null

    return when (fqName) {
      Identifiers.TYPE_INFO_FUNCTION_NAME.withPackageName() -> Identifiers.TYPE_INFO_FUNCTION_NAME
      Identifiers.FULL_TYPE_INFO_FUNCTION_NAME.withPackageName() -> Identifiers.FULL_TYPE_INFO_FUNCTION_NAME
      "TypeInfoKt.${Identifiers.TYPE_INFO_FUNCTION_NAME}".withPackageName() -> Identifiers.TYPE_INFO_FUNCTION_NAME
      "TypeInfoKt.${Identifiers.FULL_TYPE_INFO_FUNCTION_NAME}".withPackageName() -> Identifiers.FULL_TYPE_INFO_FUNCTION_NAME
      else -> null
    }
  }

  inner class TypeInfoIntrinsicMethod(private val functionName: String) : IntrinsicMethod() {
    override fun invoke(
      expression: IrFunctionAccessExpression,
      codegen: ExpressionCodegen,
      data: BlockInfo
    ): PromisedValue {
      val bytecodePoet = createBytecodePoet(adapter = codegen.mv)

      val typeArgument = expression.typeArguments[0]!!

      with(codegen) {
        expression.markLineNumber(startOffset = true)

        generateTypeInfo(typeArgument, bytecodePoet, functionName)
      }

      codegen.propagateChildReifiedTypeParametersUsages(
        codegen.typeMapper.typeSystem.extractUsedReifiedParameters(typeArgument)
      )

      return MaterialValue(codegen, BytecodePoet.typeInfoType, expression.type)
    }
  }

  private fun generateTypeInfo(type: IrType, bytecodePoet: BytecodePoet, functionName: String) {
    val typeDescriptor = with(typeSystemContext) {
      type.typeConstructor().getTypeParameterClassifier()
    }

    if (typeDescriptor != null) {
      bytecodePoet.reifyMarker(typeSystemContext, type, functionName, typeDescriptor)
      return
    }

    with(bytecodePoet) {
      when (functionName) {
        Identifiers.TYPE_INFO_FUNCTION_NAME -> initTypeInfo(type)
        Identifiers.FULL_TYPE_INFO_FUNCTION_NAME -> throwNotImplementedError("fullTypeInfo<T>() through inline functions is not yet supported")
      }
    }
  }

  private fun createBytecodePoet(adapter: InstructionAdapter): BytecodePoet = BytecodePoet(
    adapter,
    irPluginContext,
    typeMapper
  )
}