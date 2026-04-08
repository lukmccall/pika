@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.lukmccall.pika.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.FqName

/**
 * IR transformer that replaces calls to pTypeDescriptorOf<T>() and fullTypeInfo<T>() with constructed expressions.
 *
 * Note: When the type argument is a type parameter (e.g., inside an inline function),
 * we skip transformation and leave the call intact. The JvmIrIntrinsicExtension will
 * handle these calls during codegen after inline functions are inlined and type
 * parameters are substituted with concrete types.
 */
class TypeInfoCallTransformer(
  private val context: IrPluginContext,
  private val poet: IRPoet
) : IrTransformer<Nothing?>() {

  companion object {
    private val PLUGIN_PACKAGE = FqName("io.github.lukmccall.pika")
    private const val P_TYPE_DESCRIPTOR_OF_FUNCTION_NAME = "pTypeDescriptorOf"
    private const val FULL_TYPE_INFO_FUNCTION_NAME = "fullTypeInfo"
  }

  override fun visitCall(expression: IrCall, data: Nothing?): IrElement {
    expression.transformChildren(this, data)

    val function = expression.symbol.owner
    val functionName = function.name.asString()

    if (!isPluginCall(function)) {
      return expression
    }

    val typeArg = expression.typeArguments.getOrNull(0) ?: return expression

    // If the type argument is a type parameter, skip transformation.
    // The JvmIrIntrinsicExtension will handle this call during codegen
    // after inline functions are inlined and type parameters are substituted.
    if (isTypeParameter(typeArg)) {
      return expression
    }

    return when (functionName) {
      P_TYPE_DESCRIPTOR_OF_FUNCTION_NAME -> poet.pika.pTypeDescriptor(typeArg)
      FULL_TYPE_INFO_FUNCTION_NAME -> poet.pika.fullTypeInfo(typeArg, typeArg.isMarkedNullable())
      else -> expression
    }
  }

  /**
   * Check if the type is a type parameter (not a concrete type).
   */
  private fun isTypeParameter(type: IrType): Boolean {
    val simpleType = type as? IrSimpleType ?: return false
    return simpleType.classifier is IrTypeParameterSymbol
  }

  private fun isPluginCall(function: org.jetbrains.kotlin.ir.declarations.IrSimpleFunction): Boolean {
    val functionName = function.name.asString()
    if (functionName != P_TYPE_DESCRIPTOR_OF_FUNCTION_NAME && functionName != FULL_TYPE_INFO_FUNCTION_NAME) {
      return false
    }
    if (function.typeParameters.size != 1) {
      return false
    }
    val packageFqName = function.parent.let { parent ->
      when (parent) {
        is IrPackageFragment -> parent.packageFqName
        else -> null
      }
    }
    return packageFqName == PLUGIN_PACKAGE
  }

}
