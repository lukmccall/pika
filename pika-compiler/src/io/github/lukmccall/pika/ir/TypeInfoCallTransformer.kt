@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.lukmccall.pika.ir

import io.github.lukmccall.pika.Identifiers
import io.github.lukmccall.pika.Identifiers.toFq
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * IR transformer that replaces calls to typeDescriptorOf<T>(), introspectionOf(instance),
 * and isIntrospectable<T>() with constructed expressions.
 *
 * For concrete types, each call is rewritten to a PTypeDescriptorRegistry lookup.
 * The registry deduplicates descriptors across modules (identity equality ===).
 *
 * Note: When the type argument is a type parameter (e.g., inside an inline function),
 * we skip transformation and leave the call intact. The JvmIrIntrinsicExtension will
 * handle these calls during codegen after inline functions are inlined and type
 * parameters are substituted with concrete types.
 */
class TypeInfoCallTransformer(
  private val poet: IRPoet,
) : IrTransformer<Nothing?>() {

  override fun visitCall(expression: IrCall, data: Nothing?): IrElement {
    expression.transformChildren(this, data)

    val function = expression.symbol.owner
    val functionName = function.name.asString()

    if (!isPluginCall(function)) {
      return expression
    }

    return when (functionName) {
      Identifiers.TYPE_DESCRIPTOR_OF_FUNCTION_NAME -> {
        val typeArg = expression.typeArguments.getOrNull(0) ?: return expression
        if (isTypeParameter(typeArg)) {
          return expression
        }
        poet.pika.typeDescriptor(typeArg)
      }

      Identifiers.INTROSPECTION_OF_FUNCTION_NAME -> {
        val typeArg = expression.typeArguments.getOrNull(0) ?: return expression
        if (isTypeParameter(typeArg)) {
          return expression
        }
        poet.pika.introspectionOf(typeArg, expression)
      }

      Identifiers.IS_INTROSPECTABLE_FUNCTION_NAME -> {
        val typeArg = expression.typeArguments.getOrNull(0) ?: return expression
        if (isTypeParameter(typeArg)) {
          return expression
        }
        poet.pika.isIntrospectable(typeArg)
      }

      else -> expression
    }
  }

  private fun isTypeParameter(type: IrType): Boolean {
    val simpleType = type as? IrSimpleType ?: return false
    return simpleType.classifier is IrTypeParameterSymbol
  }

  private fun isPluginCall(function: IrSimpleFunction): Boolean {
    if (function.name.asString() !in PLUGIN_FUNCTION_NAMES) {
      return false
    }
    val packageFqName = (function.parent as? IrPackageFragment)?.packageFqName
    return packageFqName == Identifiers.PACKAGE_NAME.toFq()
  }

  companion object {
    private val PLUGIN_FUNCTION_NAMES = setOf(
      Identifiers.TYPE_DESCRIPTOR_OF_FUNCTION_NAME,
      Identifiers.INTROSPECTION_OF_FUNCTION_NAME,
      Identifiers.IS_INTROSPECTABLE_FUNCTION_NAME
    )
  }
}
