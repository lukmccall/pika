@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.lukmccall.pika.ir

import io.github.lukmccall.pika.Identifiers
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
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
import org.jetbrains.kotlin.name.Name

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

      val dispatchReceiver = syntheticAccessor.parameters
        .first { it.kind == IrParameterKind.DispatchReceiver }

      if (isGetter) {
        // Generate: return this.<backingField>
        val getField = IrGetFieldImpl(
          startOffset = -1, endOffset = -1,
          symbol = backingField.symbol,
          type = backingField.type,
          receiver = IrGetValueImpl(-1, -1, irClass.defaultType, dispatchReceiver.symbol)
        )
        syntheticAccessor.body = poet.createReturnBody(context.irFactory, syntheticAccessor, getField)
      } else {
        val valueParam = syntheticAccessor.parameters.first { it.kind == IrParameterKind.Regular }

        // Generate: this.<backingField> = value
        syntheticAccessor.body = context.irFactory.createBlockBody(-1, -1).apply {
          statements.add(
            IrSetFieldImpl(
              startOffset = -1, endOffset = -1,
              symbol = backingField.symbol,
              receiver = IrGetValueImpl(-1, -1, irClass.defaultType, dispatchReceiver.symbol),
              value = IrGetValueImpl(-1, -1, valueParam.type, valueParam.symbol),
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

    val cacheOwner = function.parentAsClass
    val cacheField = createIntrospectionDataCacheField(
      owner = cacheOwner,
      type = introspectionData.type,
      initializer = introspectionData
    )

    val dispatchReceiver = function.parameters
      .first { it.kind == IrParameterKind.DispatchReceiver }

    val getCachedField = IrGetFieldImpl(
      startOffset = -1, endOffset = -1,
      symbol = cacheField.symbol,
      type = cacheField.type,
      receiver = IrGetValueImpl(-1, -1, cacheOwner.defaultType, dispatchReceiver.symbol)
    )

    function.body = poet.createReturnBody(context.irFactory, function, getCachedField)
  }

  private fun createIntrospectionDataCacheField(
    owner: IrClass,
    type: org.jetbrains.kotlin.ir.types.IrType,
    initializer: IrExpression
  ): IrField {
    val field = context.irFactory.buildField {
      startOffset = -1
      endOffset = -1
      name = Name.identifier(Identifiers.P_INTROSPECTION_DATA_CACHE_FIELD_NAME)
      this.type = type
      visibility = DescriptorVisibilities.PRIVATE
      isFinal = true
      isStatic = false
    }.apply {
      parent = owner
      this.initializer = context.irFactory.createExpressionBody(-1, -1, initializer).also { body ->
        body.patchDeclarationParents(this)
      }
    }
    owner.declarations.add(field)
    return field
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
