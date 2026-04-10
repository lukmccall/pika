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
 * IR transformer that replaces calls to pTypeDescriptorOf<T>()
 * and pIntrospectionOf(instance) with constructed expressions.
 *
 * Note: When the type argument is a type parameter (e.g., inside an inline function),
 * we skip transformation and leave the call intact. The JvmIrIntrinsicExtension will
 * handle these calls during codegen after inline functions are inlined and type
 * parameters are substituted with concrete types.
 */
class TypeInfoCallTransformer(
  private val poet: IRPoet
) : IrTransformer<Nothing?>() {

  override fun visitCall(expression: IrCall, data: Nothing?): IrElement {
    expression.transformChildren(this, data)

    val function = expression.symbol.owner
    val functionName = function.name.asString()

    if (!isPluginCall(function)) {
      return expression
    }

    return when (functionName) {
      Identifiers.P_TYPE_DESCRIPTOR_OF_FUNCTION_NAME -> {
        val typeArg = expression.typeArguments.getOrNull(0) ?: return expression

        // If the type argument is a type parameter, skip transformation.
        if (isTypeParameter(typeArg)) {
          return expression
        }

        poet.pika.pTypeDescriptor(typeArg)
      }

      Identifiers.P_INTROSPECTION_OF_FUNCTION_NAME -> {
        val instanceArg = expression.arguments[0]
          ?: return expression
        poet.pika.pIntrospectionOf(instanceArg, expression)
      }

      Identifiers.P_IS_INTROSPECTABLE_FUNCTION_NAME -> {
        val typeArg = expression.typeArguments.getOrNull(0) ?: return expression

        // If the type argument is a type parameter, skip transformation.
        if (isTypeParameter(typeArg)) {
          return expression
        }

        poet.pika.pIsIntrospectable(typeArg)
      }

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

  private fun isPluginCall(function: IrSimpleFunction): Boolean {
    val functionName = function.name.asString()
    if (functionName != Identifiers.P_TYPE_DESCRIPTOR_OF_FUNCTION_NAME &&
      functionName != Identifiers.P_INTROSPECTION_OF_FUNCTION_NAME &&
      functionName != Identifiers.P_IS_INTROSPECTABLE_FUNCTION_NAME
    ) {
      return false
    }
    val packageFqName = function.parent.let { parent ->
      when (parent) {
        is IrPackageFragment -> parent.packageFqName
        else -> null
      }
    }
    return packageFqName == Identifiers.PACKAGE_NAME.toFq()
  }

}
