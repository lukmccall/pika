@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.lukmccall.pika.ir

import io.github.lukmccall.pika.Identifiers
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * IR transformer that generates `__PIntrospectionData()` functions for classes
 * that implement the Introspectable marker interface.
 */
class IntrospectableTransformer(
  private val context: IrPluginContext,
  private val poet: IRPoet,
  private val symbolFinder: SymbolFinder
) : IrTransformer<Nothing?>() {

  override fun visitClass(declaration: IrClass, data: Nothing?): IrStatement {
    declaration.transformChildren(this, data)

    if (declaration.isInterface || declaration.modality == Modality.ABSTRACT) {
      return declaration
    }

    if (!declaration.hasIntrospectableAnnotation()) {
      return declaration
    }

    if (declaration.isObject && !declaration.isCompanion) {
      return handleObjectDeclaration(declaration)
    }

    return handleClassDeclaration(declaration)
  }

  private fun handleObjectDeclaration(declaration: IrClass): IrStatement = declaration.apply {
    val function = declaration
      .find__PIntrospectionData()
      ?.takeIfHasNoBody()
      ?: return@apply

    generateIntrospectionDataFunctionBody(
      declaration,
      function
    )
  }

  private fun handleClassDeclaration(declaration: IrClass): IrStatement = declaration.apply {
    // First, generate bodies for synthetic accessor functions on the main class
    generateSyntheticAccessorBodies(declaration)

    // Then generate the introspection function in the companion
    val function = declaration
      .companionObject()
      ?.find__PIntrospectionData()
      ?.takeIfHasNoBody()
      ?: return@apply

    generateIntrospectionDataFunctionBody(
      declaration,
      function
    )
  }

  private fun generateSyntheticAccessorBodies(irClass: IrClass) {
    val allProperties = irClass
      .declarations
      .filterIsInstance<IrProperty>()
      .filter { it.backingField != null }
      .associateBy { it.name.asString() }

    val syntheticAccessors = irClass
      .declarations
      .asSequence()
      .filterIsInstance<IrSimpleFunction>()
      .filter { Identifiers.isSyntheticAccessor(it.name.asString()) && it.body == null }
      .mapNotNull {
        val propertyName = Identifiers.removeSyntheticAccessor(it.name.asString())
        val property = allProperties[propertyName] ?: return@mapNotNull null
        property to it
      }

    for ((originalProperty, syntheticAccessor) in syntheticAccessors) {
      val isGetter = Identifiers.isSyntheticGetter(syntheticAccessor.name.asString())
      val backingField = requireNotNull(originalProperty.backingField)

      if (isGetter) {
        // Generate: return this.<backingField>
        syntheticAccessor.body = context
          .irFactory
          .createBlockBody(-1, -1)
          .apply {
            statements.add(
              IrReturnImpl(
                startOffset = -1,
                endOffset = -1,
                type = context.irBuiltIns.nothingType,
                returnTargetSymbol = syntheticAccessor.symbol,
                value = IrGetFieldImpl(
                  startOffset = -1,
                  endOffset = -1,
                  symbol = backingField.symbol,
                  type = backingField.type,
                  receiver = IrGetValueImpl(
                    startOffset = -1,
                    endOffset = -1,
                    type = irClass.defaultType,
                    symbol = syntheticAccessor.dispatchReceiverParameter!!.symbol
                  )
                )
              )
            )
          }
      } else {
        val valueParam = syntheticAccessor
          .parameters
          .first { it.kind == IrParameterKind.Regular }

        // Generate: this.<backingField> = value
        syntheticAccessor.body = context
          .irFactory
          .createBlockBody(-1, -1)
          .apply {
            statements.add(
              IrSetFieldImpl(
                startOffset = -1,
                endOffset = -1,
                symbol = backingField.symbol,
                receiver = IrGetValueImpl(
                  startOffset = -1,
                  endOffset = -1,
                  type = irClass.defaultType,
                  symbol = syntheticAccessor.dispatchReceiverParameter!!.symbol
                ),
                value = IrGetValueImpl(
                  startOffset = -1,
                  endOffset = -1,
                  type = valueParam.type,
                  symbol = valueParam.symbol
                ),
                type = context.irBuiltIns.unitType
              )
            )
          }
      }
    }
  }

  @Suppress("FunctionName")
  private fun IrClass.find__PIntrospectionData(): IrSimpleFunction? {
    return declarations
      .filterIsInstance<IrSimpleFunction>()
      .find { it.name.asString() == Identifiers.P_INTROSPECTION_DATA_FUNCTION_NAME }
  }

  private fun generateIntrospectionDataFunctionBody(
    irClass: IrClass,
    function: IrSimpleFunction
  ) {
    // Build properties list (only properties declared directly on this class)
    val properties = irClass.declarations
      .filterIsInstance<IrProperty>()
      .filter { !it.isFakeOverride }
      .map { property ->
        poet.pika.pProperty(
          property,
          irClass,
          function,
          context.irFactory
        )
      }

    // Build functions list (only functions declared directly on this class, excluding special ones)
    val functions = irClass.declarations
      .filterIsInstance<IrSimpleFunction>()
      .filter { func ->
        // Exclude constructors, property accessors, the generated function, and fake overrides
        !func.name.isSpecial &&
          !func.isFakeOverride &&
          func.correspondingPropertySymbol == null &&
          func.name.asString() != Identifiers.P_INTROSPECTION_DATA_FUNCTION_NAME &&
          func.name.asString() != "<init>"
      }
      .map { func ->
        poet.pika.pFunction(func)
      }

    // Build base class reference if parent implements Introspectable
    val baseClassExpr = buildBaseClassReference(irClass)

    val introspectionData = poet.pika.pIntrospectionData(
      irClass = irClass,
      properties = properties,
      functions = functions,
      baseClassExpr = baseClassExpr
    )

    function.body = context.irFactory.createBlockBody(-1, -1).apply {
      statements.add(
        IrReturnImpl(
          startOffset = -1,
          endOffset = -1,
          type = context.irBuiltIns.nothingType,
          returnTargetSymbol = function.symbol,
          value = introspectionData
        )
      )
    }
  }

  /**
   * Build a call to the parent's companion __PIntrospectionData() function if the parent
   * has Introspectable annotation, otherwise return null.
   *
   * Since the function is now in the companion object, we call it directly:
   * ParentClass.__PIntrospectionData() (no instance needed)
   */
  private fun buildBaseClassReference(irClass: IrClass): IrExpression? {
    val superClass = irClass.superTypes
      .filterIsInstance<IrSimpleType>()
      .filter { !it.isInterface() }
      .mapNotNull { it.classOrNull?.owner }
      .firstOrNull { it.kotlinFqName.asString() != "kotlin.Any" }
      ?: return null

    // Check if parent implements Introspectable
    if (!superClass.hasIntrospectableAnnotation()) {
      return null
    }

    // Find the companion object of the parent class
    val parentCompanion = superClass.companionObject() ?: return null

    // Find the __PIntrospectionData function on the parent's companion
    val parentFunction = superClass.companionObject()?.find__PIntrospectionData() ?: return null

    val pIntrospectionDataClass = symbolFinder.pikaAPI.pIntrospectionData
    val parentReturnType = pIntrospectionDataClass.typeWith(superClass.defaultType)

    return IrCallImpl(
      startOffset = -1,
      endOffset = -1,
      type = parentReturnType,
      symbol = parentFunction.symbol,
      typeArgumentsCount = 0,
      origin = null
    ).apply {
      // Dispatch receiver is the companion object instance
      // In K2 IR, arguments array contains ALL parameters including dispatch receiver
      val dispatchReceiverValue = poet.kotlin.getObject(parentCompanion.symbol)
      parentFunction.parameters.forEach { param ->
        if (param.kind == IrParameterKind.DispatchReceiver) {
          arguments[param.indexInParameters] = dispatchReceiverValue
        }
      }
    }
  }
}
