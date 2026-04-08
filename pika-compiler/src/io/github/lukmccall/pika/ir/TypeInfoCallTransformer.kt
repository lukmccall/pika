@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.lukmccall.pika.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.FqName

/**
 * IR transformer that replaces calls to pTypeDescriptorOf<T>(), fullTypeInfo<T>(),
 * and pIntrospectionOf(instance) with constructed expressions.
 *
 * Note: When the type argument is a type parameter (e.g., inside an inline function),
 * we skip transformation and leave the call intact. The JvmIrIntrinsicExtension will
 * handle these calls during codegen after inline functions are inlined and type
 * parameters are substituted with concrete types.
 */
class TypeInfoCallTransformer(
  private val context: IrPluginContext,
  private val poet: IRPoet,
  private val symbolFinder: SymbolFinder
) : IrTransformer<Nothing?>() {

  companion object {
    private val PLUGIN_PACKAGE = FqName("io.github.lukmccall.pika")
    private const val P_TYPE_DESCRIPTOR_OF_FUNCTION_NAME = "pTypeDescriptorOf"
    private const val FULL_TYPE_INFO_FUNCTION_NAME = "fullTypeInfo"
    private const val P_INTROSPECTION_OF_FUNCTION_NAME = "pIntrospectionOf"
    private const val INTROSPECTION_DATA_FUNCTION_NAME = "__PIntrospectionData"
  }

  override fun visitCall(expression: IrCall, data: Nothing?): IrElement {
    expression.transformChildren(this, data)

    val function = expression.symbol.owner
    val functionName = function.name.asString()

    if (!isPluginCall(function)) {
      return expression
    }

    return when (functionName) {
      P_TYPE_DESCRIPTOR_OF_FUNCTION_NAME, FULL_TYPE_INFO_FUNCTION_NAME -> {
        val typeArg = expression.typeArguments.getOrNull(0) ?: return expression

        // If the type argument is a type parameter, skip transformation.
        if (isTypeParameter(typeArg)) {
          return expression
        }

        when (functionName) {
          P_TYPE_DESCRIPTOR_OF_FUNCTION_NAME -> poet.pika.pTypeDescriptor(typeArg)
          FULL_TYPE_INFO_FUNCTION_NAME -> poet.pika.fullTypeInfo(typeArg, typeArg.isMarkedNullable())
          else -> expression
        }
      }
      P_INTROSPECTION_OF_FUNCTION_NAME -> {
        val instanceArg = expression.arguments[0]
          ?: return expression
        generateIntrospectionCall(instanceArg, expression)
      }
      else -> expression
    }
  }

  /**
   * Generate a call to instance.__PIntrospectionData()
   */
  private fun generateIntrospectionCall(instance: IrExpression, originalCall: IrCall): IrExpression {
    val instanceType = instance.type as? IrSimpleType
      ?: return originalCall

    val irClass = instanceType.classOrNull?.owner
      ?: return originalCall

    // Find the __PIntrospectionData function on the class
    val introspectionDataFunction = irClass.declarations
      .filterIsInstance<IrSimpleFunction>()
      .find { it.name.asString() == INTROSPECTION_DATA_FUNCTION_NAME }
      ?: return originalCall

    // Build return type: PIntrospectionData<T>
    val pIntrospectionDataClass = symbolFinder.pikaAPI.pIntrospectionData
    val returnType = pIntrospectionDataClass.typeWith(irClass.defaultType)

    return IrCallImpl(
      startOffset = originalCall.startOffset,
      endOffset = originalCall.endOffset,
      type = returnType,
      symbol = introspectionDataFunction.symbol,
      typeArgumentsCount = 0,
      origin = null,
      superQualifierSymbol = null
    ).apply {
      dispatchReceiver = instance
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
    if (functionName != P_TYPE_DESCRIPTOR_OF_FUNCTION_NAME &&
        functionName != FULL_TYPE_INFO_FUNCTION_NAME &&
        functionName != P_INTROSPECTION_OF_FUNCTION_NAME) {
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
