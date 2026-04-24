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
 * IR transformer that generates `__pika$IntrospectionData` cached fields for classes
 * annotated with @Introspectable.
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

    if (!declaration.hasIntrospectableAnnotation(poet.extraAnnotationClassIds)) {
      return declaration
    }

    if (declaration.isObject && !declaration.isCompanion) {
      return handleObjectDeclaration(declaration)
    }

    return handleClassDeclaration(declaration)
  }

  private fun handleObjectDeclaration(declaration: IrClass): IrStatement = declaration.apply {
    generateIntrospectionDataField(
      irClass = declaration,
      fieldOwner = declaration
    )
  }

  private fun handleClassDeclaration(declaration: IrClass): IrStatement = declaration.apply {
    generateSyntheticAccessorBodies(declaration)

    val companion = declaration.companionObject() ?: return@apply
    generateIntrospectionDataField(
      irClass = declaration,
      fieldOwner = companion
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

        // Strip ACC_FINAL so PUTFIELD from the synthetic setter (non-<init>) does not
        // throw java.lang.IllegalAccessError: Update to non-static final field ...
        // attempted from a different method (__pika$set$...) than the initializer method <init>
        backingField.isFinal = false

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

  private fun generateIntrospectionDataField(
    irClass: IrClass,
    fieldOwner: IrClass
  ) {
    val properties = irClass.declarations
      .filterIsInstance<IrProperty>()
      .filter { !it.isFakeOverride }
      .map { property ->
        poet.pika.pProperty(
          property,
          irClass,
          fieldOwner,
          context.irFactory
        )
      }

    val functions = irClass.declarations
      .filterIsInstance<IrSimpleFunction>()
      .filter { func ->
        !func.name.isSpecial &&
          !func.isFakeOverride &&
          func.correspondingPropertySymbol == null &&
          func.name.asString() != "<init>" &&
          !func.name.asString().startsWith(Identifiers.PIKA_SPECIAL_PREFIX)
      }
      .map { func ->
        poet.pika.pFunction(func)
      }

    val baseClassExpr = buildBaseClassReference(irClass)

    val introspectionData = poet.pika.pIntrospectionData(
      irClass = irClass,
      properties = properties,
      functions = functions,
      baseClassExpr = baseClassExpr,
      irFactory = context.irFactory,
      containingDeclaration = fieldOwner
    )

    val field = context.irFactory.buildField {
      startOffset = -1
      endOffset = -1
      name = Name.identifier(Identifiers.INTROSPECTION_DATA_FIELD_NAME)
      type = introspectionData.type
      visibility = DescriptorVisibilities.PUBLIC
      isFinal = true
      isStatic = true
    }.apply {
      parent = fieldOwner
      initializer = context.irFactory.createExpressionBody(
        startOffset = -1,
        endOffset = -1,
        expression = introspectionData
      ).also { body ->
        body.patchDeclarationParents(this)
      }
    }
    fieldOwner.declarations.add(field)
  }

  private fun buildBaseClassReference(irClass: IrClass): IrExpression? {
    val superClass = irClass.superTypes
      .filterIsInstance<IrSimpleType>()
      .filter { !it.isInterface() }
      .mapNotNull { it.classOrNull?.owner }
      .firstOrNull { it.kotlinFqName.asString() != "kotlin.Any" }
      ?: return null

    if (!superClass.hasIntrospectableAnnotation(poet.extraAnnotationClassIds)) {
      return null
    }

    val fieldOwner = if (superClass.isObject && !superClass.isCompanion) {
      superClass
    } else {
      superClass.companionObject() ?: return null
    }

    val field = fieldOwner.declarations
      .filterIsInstance<IrField>()
      .find { it.name.asString() == Identifiers.INTROSPECTION_DATA_FIELD_NAME }
      ?: return null

    val pIntrospectionDataClass = symbolFinder.pikaAPI.pIntrospectionData
    val parentReturnType = pIntrospectionDataClass.typeWith(superClass.defaultType)

    return IrGetFieldImpl(
      startOffset = -1,
      endOffset = -1,
      symbol = field.symbol,
      type = parentReturnType,
      receiver = null
    )
  }
}
