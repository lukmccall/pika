@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.lukmccall.pika.ir

import io.github.lukmccall.pika.Identifiers
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildReceiverParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.Name

class IntrospectableTransformer(
  private val context: IrPluginContext,
  private val poet: IRPoet,
  private val symbolFinder: SymbolFinder
) : IrTransformer<Nothing?>() {

  private val irBuiltIns get() = poet.irBuiltIns

  private val indexOutOfBoundsConstructor: IrConstructorSymbol by lazy {
    symbolFinder.javaLang.indexOutOfBoundsException.constructors.find { ctor ->
      val params = ctor.owner.parameters
      params.size == 1 && params[0].type.classOrNull == irBuiltIns.stringClass
    } ?: error("Could not find IndexOutOfBoundsException constructor")
  }

  private val unsupportedOperationConstructor: IrConstructorSymbol by lazy {
    symbolFinder.javaLang.unsupportedOperationException.constructors.find { ctor ->
      val params = ctor.owner.parameters
      params.size == 1 && params[0].type.classOrNull == irBuiltIns.stringClass
    } ?: error("Could not find IndexOutOfBoundsException constructor")
}

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
    relaxBackingFieldAccess(declaration)

    val pikaObject = declaration.pikaObject() ?: return@apply
    generateIntrospectionDataField(
      irClass = declaration,
      fieldOwner = pikaObject
    )
  }

  private fun relaxBackingFieldAccess(irClass: IrClass) {
    irClass.declarations
      .filterIsInstance<IrProperty>()
      .mapNotNull { it.backingField }
      .forEach { backingField ->
        backingField.isFinal = false
        if (DescriptorVisibilities.isPrivate(backingField.visibility)) {
          backingField.visibility = DescriptorVisibilities.INTERNAL
        }
      }
  }

  private fun indexEqualsCondition(indexParam: IrValueParameter, value: Int): IrExpression =
    IrCallImpl(
      -1, -1, irBuiltIns.booleanType,
      irBuiltIns.eqeqSymbol,
      typeArgumentsCount = 0,
      origin = IrStatementOrigin.EQEQ
    ).apply {
      arguments[0] = IrGetValueImpl(-1, -1, irBuiltIns.intType, indexParam.symbol)
      arguments[1] = IrConstImpl.int(-1, -1, irBuiltIns.intType, value)
    }

  private fun throwIndexOutOfBounds(indexParam: IrValueParameter): IrExpression =
    IrThrowImpl(
      -1, -1, irBuiltIns.nothingType,
      IrConstructorCallImpl(
        -1, -1,
        type = indexOutOfBoundsConstructor.owner.returnType,
        symbol = indexOutOfBoundsConstructor,
        typeArgumentsCount = 0, constructorTypeArgumentsCount = 0, origin = null
      ).apply {
        arguments[0] = IrStringConcatenationImpl(-1, -1, irBuiltIns.stringType, listOf(
          IrGetValueImpl(-1, -1, irBuiltIns.intType, indexParam.symbol)
        ))
      }
    )

  private fun throwUnsupportedOperation(message: String): IrExpression =
    IrThrowImpl(
      -1, -1, irBuiltIns.nothingType,
      IrConstructorCallImpl(
        -1, -1,
        type = unsupportedOperationConstructor.owner.returnType,
        symbol = unsupportedOperationConstructor,
        typeArgumentsCount = 0, constructorTypeArgumentsCount = 0, origin = null
      ).apply {
        arguments[0] = IrConstImpl.string(-1, -1, irBuiltIns.stringType, message)
      }
    )

  private fun generateIntrospectionDataField(
    irClass: IrClass,
    fieldOwner: IrClass
  ) {
    val ownerType = irClass.defaultType

    val irProperties = irClass.declarations
      .filterIsInstance<IrProperty>()
      .filter { !it.isFakeOverride }

    val accessorType = symbolFinder.pikaAPI.pPropertyAccessor.owner.defaultType
    fieldOwner.superTypes = fieldOwner.superTypes + accessorType

    generatePropertyGetMethod(fieldOwner, irProperties, ownerType)
    generatePropertySetMethod(fieldOwner, irProperties, ownerType)

    val properties = irProperties.mapIndexed { index, property ->
      val accessorExpr = IrGetObjectValueImpl(-1, -1, fieldOwner.defaultType, fieldOwner.symbol)
      poet.pika.pProperty(property, irClass, index, accessorExpr)
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
      .map { func -> poet.pika.pFunction(func) }

    val baseClassExpr = buildBaseClassReference(irClass)

    val introspectionData = poet.pika.pIntrospectionData(
      irClass = irClass,
      properties = properties,
      functions = functions,
      baseClassExpr = baseClassExpr,
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
      initializer = context.irFactory.createExpressionBody(-1, -1, introspectionData).also { body ->
        body.patchDeclarationParents(this)
      }
    }
    fieldOwner.declarations.add(field)
  }

  private fun generatePropertyGetMethod(
    fieldOwner: IrClass,
    irProperties: List<IrProperty>,
    ownerType: IrType
  ) {
    val getMethod = context.irFactory.buildFun {
      name = Name.identifier(Identifiers.PROPERTY_ACCESSOR_GET_NAME)
      returnType = irBuiltIns.anyNType
      visibility = DescriptorVisibilities.PUBLIC
      modality = Modality.OPEN
      origin = IrDeclarationOrigin.DEFINED
    }.apply {
      parent = fieldOwner
      parameters = listOfNotNull(
        buildReceiverParameter { type = fieldOwner.defaultType }
      )
      overriddenSymbols = listOf(symbolFinder.pikaAPI.pPropertyAccessorGet)
      val instanceParam = addValueParameter("instance", irBuiltIns.anyNType)
      val indexParam = addValueParameter("index", irBuiltIns.intType)

      val branches = irProperties.mapIndexed { index, property ->
        IrBranchImpl(
          -1, -1,
          condition = indexEqualsCondition(indexParam, index),
          result = poet.pika.propertyGetExpression(property, ownerType, instanceParam)
        )
      }

      val elseBranch = IrElseBranchImpl(
        -1, -1,
        condition = IrConstImpl.boolean(-1, -1, irBuiltIns.booleanType, true),
        result = throwIndexOutOfBounds(indexParam)
      )

      body = poet.createReturnBody(
        context.irFactory, this,
        IrWhenImpl(-1, -1, irBuiltIns.anyNType, origin = null).apply {
          this.branches.addAll(branches)
          this.branches.add(elseBranch)
        }
      )
    }
    fieldOwner.declarations.add(getMethod)
  }

  private fun generatePropertySetMethod(
    fieldOwner: IrClass,
    irProperties: List<IrProperty>,
    ownerType: IrType
  ) {
    val setMethod = context.irFactory.buildFun {
      name = Name.identifier(Identifiers.PROPERTY_ACCESSOR_SET_NAME)
      returnType = irBuiltIns.unitType
      visibility = DescriptorVisibilities.PUBLIC
      modality = Modality.OPEN
      origin = IrDeclarationOrigin.DEFINED
    }.apply {
      parent = fieldOwner
      parameters = listOfNotNull(
        buildReceiverParameter { type = fieldOwner.defaultType }
      )
      overriddenSymbols = listOf(symbolFinder.pikaAPI.pPropertyAccessorSet)
      val instanceParam = addValueParameter("instance", irBuiltIns.anyNType)
      val indexParam = addValueParameter("index", irBuiltIns.intType)
      val valueParam = addValueParameter("value", irBuiltIns.anyNType)

      val branches = irProperties.mapIndexed { index, property ->
        val hasBackingField = property.backingField != null
        IrBranchImpl(
          -1, -1,
          condition = indexEqualsCondition(indexParam, index),
          result = if (hasBackingField) {
            poet.pika.propertySetExpression(property, ownerType, instanceParam, valueParam)
          } else {
            throwUnsupportedOperation("Property '${property.name.asString()}' does not have a backing field and cannot be set.")
          }
        )
      }

      val elseBranch = IrElseBranchImpl(
        -1, -1,
        condition = IrConstImpl.boolean(-1, -1, irBuiltIns.booleanType, true),
        result = throwIndexOutOfBounds(indexParam)
      )

      body = context.irFactory.createBlockBody(-1, -1).apply {
        statements.add(
          IrWhenImpl(-1, -1, irBuiltIns.unitType, origin = null).apply {
            this.branches.addAll(branches)
            this.branches.add(elseBranch)
          }
        )
      }
    }
    fieldOwner.declarations.add(setMethod)
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
      superClass.pikaObject() ?: return null
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
