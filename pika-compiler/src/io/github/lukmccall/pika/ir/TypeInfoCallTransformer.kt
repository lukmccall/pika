package io.github.lukmccall.pika.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.FqName

/**
 * IR transformer that replaces calls to typeInfo<T>() and fullTypeInfo<T>() with constructed expressions.
 */
class TypeInfoCallTransformer(
  private val context: IrPluginContext,
  private val poet: IRPoet
) : IrTransformer<Nothing?>() {

  companion object {
    private val PLUGIN_PACKAGE = FqName("io.github.lukmccall.pika")
    private const val TYPE_INFO_FUNCTION_NAME = "typeInfo"
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

    return when (functionName) {
      TYPE_INFO_FUNCTION_NAME -> poet.pika.typeInfo(typeArg)
      FULL_TYPE_INFO_FUNCTION_NAME -> poet.pika.fullTypeInfo(typeArg, typeArg.isMarkedNullable())
      else -> expression
    }
  }

  private fun isPluginCall(function: org.jetbrains.kotlin.ir.declarations.IrSimpleFunction): Boolean {
    val functionName = function.name.asString()
    if (functionName != TYPE_INFO_FUNCTION_NAME && functionName != FULL_TYPE_INFO_FUNCTION_NAME) {
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
