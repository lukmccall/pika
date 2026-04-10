@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.lukmccall.pika.ir

import io.github.lukmccall.pika.Identifiers
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.name.Name

class IRPoet(
  context: IrPluginContext,
  private val symbolFinder: SymbolFinder
) {
  val irBuiltIns = context.irBuiltIns
  val kotlin = Kotlin()

  inner class Kotlin {
    fun string(value: String) =
      IrConstImpl.string(
        startOffset = -1,
        endOffset = -1,
        type = irBuiltIns.stringType,
        value = value
      )

    fun bool(value: Boolean) =
      IrConstImpl.boolean(
        startOffset = -1,
        endOffset = -1,
        type = irBuiltIns.booleanType,
        value = value
      )

    fun `null`() = createIrConst(
      startOffset = -1,
      endOffset = -1,
      type = irBuiltIns.nothingNType,
      kind = IrConstKind.Null,
      value = null
    )

    /**
     * {key} to {value} -> Pair<{keyType}, {valueType}>
     */
    fun pair(keyType: IrType, key: IrExpression, valueType: IrType, value: IrExpression): IrExpression {
      val pairType = symbolFinder
        .kotlinStd
        .pair
        .typeWith(
          keyType,
          valueType
        )

      val to = symbolFinder.kotlinStd.to.first()

      return IrCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = pairType,
        symbol = to,
        typeArgumentsCount = 2,
        origin = null,
        superQualifierSymbol = null
      ).also { call ->
        call.typeArguments[0] = keyType
        call.typeArguments[1] = valueType
        call.arguments[0] = key
        call.arguments[1] = value
      }
    }

    /**
     * mapOf<{keyType}, {valueType}>({pairs})
     */
    fun mapOf(keyType: IrType, valueType: IrType, pairs: List<IrExpression>): IrExpression {
      val mapType = irBuiltIns
        .mapClass
        .typeWith(
          keyType,
          valueType
        )

      val pairType = symbolFinder
        .kotlinStd
        .pair
        .typeWith(
          keyType,
          valueType
        )

      val mapOf = symbolFinder
        .kotlinStd
        .collections
        .mapOf
        .firstSingleVarargsArgument()

      return IrCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = mapType,
        symbol = mapOf,
        typeArgumentsCount = 2,
        origin = IrStatementOrigin.INVOKE,
        superQualifierSymbol = null
      ).also { call ->
        call.typeArguments[0] = keyType
        call.typeArguments[1] = valueType
        call.arguments[0] = IrVarargImpl(
          startOffset = -1,
          endOffset = -1,
          type = irBuiltIns.arrayClass.typeWith(pairType),
          varargElementType = pairType,
          elements = pairs
        )
      }
    }

    /**
     * emptyMap<{keyType}, {valueType}>()
     */
    fun emptyMap(keyType: IrType, valueType: IrType): IrExpression {
      val mapType = irBuiltIns
        .mapClass
        .typeWith(
          keyType,
          valueType
        )

      val emptyMap = symbolFinder
        .kotlinStd
        .collections
        .emptyMap
        .single()

      return IrCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = mapType,
        symbol = emptyMap,
        typeArgumentsCount = 2,
        origin = null,
        superQualifierSymbol = null
      ).also { call ->
        call.typeArguments[0] = keyType
        call.typeArguments[1] = valueType
      }
    }

    /**
     * listOf<{type}>({elements})
     */
    fun listOf(type: IrType, elements: List<IrExpression>): IrExpression {
      val listType = irBuiltIns
        .listClass
        .typeWith(type)

      val listOf = symbolFinder
        .kotlinStd
        .collections
        .listOf
        .firstSingleVarargsArgument()

      return IrCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = listType,
        symbol = listOf,
        typeArgumentsCount = 1,
        origin = IrStatementOrigin.INVOKE,
        superQualifierSymbol = null
      ).also { call ->
        call.typeArguments[0] = type
        call.arguments[0] = IrVarargImpl(
          startOffset = -1,
          endOffset = -1,
          type = irBuiltIns.arrayClass.typeWith(type),
          varargElementType = type,
          elements = elements
        )
      }
    }

    /**
     * KClass<{classSymbol}>
     */
    fun kClass(classSymbol: IrClassSymbol): IrExpression {
      val classType = classSymbol.owner.defaultType
      val kClassType = irBuiltIns
        .kClassClass
        .typeWith(classType)

      return IrClassReferenceImpl(
        startOffset = -1,
        endOffset = -1,
        type = kClassType,
        symbol = classSymbol,
        classType = classType
      )
    }

    /**
     * Get reference to an object (companion object or singleton)
     */
    fun getObject(classSymbol: IrClassSymbol): IrExpression {
      return IrGetObjectValueImpl(
        startOffset = -1,
        endOffset = -1,
        type = classSymbol.owner.defaultType,
        symbol = classSymbol
      )
    }
  }

  val pika = Pika()

  inner class Pika {
    /**
     * io.github.lukmccall.pika.PTypeDescriptor.Star
     */
    fun star(): IrExpression {
      val pTypeDescriptorStarClass = symbolFinder.pikaAPI.pTypeDescriptor.star

      return IrGetObjectValueImpl(
        startOffset = -1,
        endOffset = -1,
        type = pTypeDescriptorStarClass.owner.defaultType,
        symbol = pTypeDescriptorStarClass
      )
    }

    /**
     * io.github.lukmccall.pika.PType({KClass<{classSymbol}>})
     */
    fun pType(classSymbol: IrClassSymbol): IrExpression {
      val pTypeClass = symbolFinder.pikaAPI.pType

      val constructor = pTypeClass.owner.primaryConstructor
        ?: error("PType must have a primary constructor")

      return IrConstructorCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = pTypeClass.owner.defaultType,
        symbol = constructor.symbol,
        typeArgumentsCount = 0,
        constructorTypeArgumentsCount = 0,
        origin = null
      ).also { call ->
        call.arguments[0] = kotlin.kClass(classSymbol)
      }
    }

    /**
     * io.github.lukmccall.pika.PTypeDescriptor.Concrete({PType({classSymbol})}, {isNullable})
     */
    fun concrete(
      classSymbol: IrClassSymbol,
      isNullable: Boolean
    ): IrExpression {
      val pTypeDescriptorConcreteClass = symbolFinder.pikaAPI.pTypeDescriptor.concrete

      val constructor = pTypeDescriptorConcreteClass.owner.primaryConstructor
        ?: error("PTypeDescriptor.Concrete must have a primary constructor")

      return IrConstructorCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = pTypeDescriptorConcreteClass.owner.defaultType,
        symbol = constructor.symbol,
        typeArgumentsCount = 0,
        constructorTypeArgumentsCount = 0,
        origin = null
      ).also { call ->
        call.arguments[0] = pType(classSymbol)
        call.arguments[1] = kotlin.bool(isNullable)
      }
    }

    /**
     * io.github.lukmccall.pika.PTypeDescriptor.Concrete.Parameterized(
     *   {PType({classSymbol})},
     *   {isNullable},
     *   {listOf({argumentsPTypes})}
     * )
     */
    fun parameterized(
      classSymbol: IrClassSymbol,
      isNullable: Boolean,
      argumentsPTypes: List<IrExpression>
    ): IrExpression {
      val pTypeDescriptorParameterizedClass = symbolFinder.pikaAPI.pTypeDescriptor.parameterized
      val pTypeDescriptorClass = symbolFinder.pikaAPI.pTypeDescriptor.root

      val constructor = pTypeDescriptorParameterizedClass.owner.primaryConstructor
        ?: error("PTypeDescriptor.Concrete.Parameterized must have a primary constructor")

      return IrConstructorCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = pTypeDescriptorParameterizedClass.owner.defaultType,
        symbol = constructor.symbol,
        typeArgumentsCount = 0,
        constructorTypeArgumentsCount = 0,
        origin = null
      ).also { call ->
        call.arguments[0] = pType(classSymbol)
        call.arguments[1] = kotlin.bool(isNullable)
        call.arguments[2] = kotlin.listOf(pTypeDescriptorClass.owner.defaultType, argumentsPTypes)
      }
    }

    /**
     * IrType -> io.github.lukmccall.pika.PTypeDescriptor.*
     */
    fun pTypeDescriptor(type: IrType): IrExpression {
      val simpleType = type as? IrSimpleType
        ?: return pika.concrete(irBuiltIns.anyClass, false)

      val classifier = simpleType.classifier
      val isNullable = simpleType.isMarkedNullable()

      return if (simpleType.arguments.isEmpty()) {
        pika.concrete(classifier as IrClassSymbol, isNullable)
      } else {
        val typeArgInfos = simpleType.arguments.map { arg ->
          when (arg) {
            is IrTypeProjection -> pTypeDescriptor(arg.type)
            is IrStarProjection -> pika.star()
          }
        }
        pika.parameterized(classifier as IrClassSymbol, isNullable, typeArgInfos)
      }
    }

    /**
     * io.github.lukmccall.pika.PVisibility.{value}
     */
    fun pVisibility(value: DescriptorVisibility): IrExpression {
      val visibility = when (value) {
        DescriptorVisibilities.PUBLIC -> "PUBLIC"
        DescriptorVisibilities.PRIVATE -> "PRIVATE"
        DescriptorVisibilities.PROTECTED -> "PROTECTED"
        DescriptorVisibilities.INTERNAL -> "INTERNAL"
        else -> "PUBLIC"
      }

      val pVisibilityEnumClass = symbolFinder.pikaAPI.pVisibility

      val enumEntry = pVisibilityEnumClass
        .owner
        .declarations
        .filterIsInstance<org.jetbrains.kotlin.ir.declarations.IrEnumEntry>()
        .find { it.name.asString() == visibility }
        ?: error("Cannot find PVisibility.$value")

      return IrGetEnumValueImpl(
        startOffset = -1,
        endOffset = -1,
        type = pVisibilityEnumClass.owner.defaultType,
        symbol = enumEntry.symbol
      )
    }

    /**
     * io.github.lukmccall.pika.PAnnotation({kClass}, mapOf({annotationArgs}))
     */
    fun pAnnotation(annotation: IrConstructorCall): IrExpression {
      val annotationClass = annotation.type.classOrNull?.owner

      val pAnnotationClass = symbolFinder.pikaAPI.pAnnotation

      val constructor = pAnnotationClass.owner.primaryConstructor
        ?: error("PAnnotation must have a primary constructor")

      val argumentsMap = annotation.toArgsMap(this@IRPoet)

      return IrConstructorCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = pAnnotationClass.owner.defaultType,
        symbol = constructor.symbol,
        typeArgumentsCount = 0,
        constructorTypeArgumentsCount = 0,
        origin = null
      ).also { call ->
        call.arguments[0] = kotlin.kClass(
          annotationClass?.symbol ?: irBuiltIns.anyClass
        )
        call.arguments[1] = argumentsMap
      }
    }

    /**
     * io.github.lukmccall.pika.PFunction({name}, {visibility})
     */
    fun pFunction(function: IrSimpleFunction): IrExpression {
      val pFunctionClass = symbolFinder.pikaAPI.pFunction

      val constructor = pFunctionClass.owner.primaryConstructor
        ?: error("PFunction must have a primary constructor")

      return IrConstructorCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = pFunctionClass.owner.defaultType,
        symbol = constructor.symbol,
        typeArgumentsCount = 0,
        constructorTypeArgumentsCount = 0,
        origin = null
      ).also { call ->
        call.arguments[0] = kotlin.string(function.name.asString())
        call.arguments[1] = pika.pVisibility(function.visibility)
      }
    }

    /**
     * Creates a getter lambda: { owner -> owner.propertyName }
     * Uses the property getter if available, otherwise uses the synthetic accessor.
     */
    fun propertyGetterLambda(
      property: IrProperty,
      ownerClass: IrClass,
      containingFunction: IrSimpleFunction,
      irFactory: org.jetbrains.kotlin.ir.declarations.IrFactory
    ): IrExpression {
      val propertyType = property.getter?.returnType
        ?: property.backingField?.type
        ?: irBuiltIns.anyNType

      val ownerType = ownerClass.defaultType

      // Create Function1<OwnerType, PropertyType>
      val function1Type = irBuiltIns.functionN(1).typeWith(ownerType, propertyType)

      val lambdaFunction = irFactory.buildFun {
        name = Name.special("<anonymous>")
        returnType = propertyType
        origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        visibility = DescriptorVisibilities.LOCAL
      }.apply {
        parent = containingFunction
        val ownerParam = addValueParameter("owner", ownerType)

        val returnValue = if (property.getter != null) {
          // Call the getter
          val getter = property.getter!!
          IrCallImpl(
            startOffset = -1,
            endOffset = -1,
            type = propertyType,
            symbol = getter.symbol,
            typeArgumentsCount = 0,
            origin = IrStatementOrigin.GET_PROPERTY,
            superQualifierSymbol = null
          ).apply {
            // In K2 IR, arguments array contains ALL parameters including dispatch receiver
            getter.parameters.forEach { param ->
              if (param.kind == IrParameterKind.DispatchReceiver) {
                arguments[param.indexInParameters] = IrGetValueImpl(
                  startOffset = -1,
                  endOffset = -1,
                  type = ownerType,
                  symbol = ownerParam.symbol
                )
              }
            }
          }
        } else {
          // Use synthetic getter to access backing field
          val syntheticGetter = ownerClass.findSyntheticGetter(property)
          if (syntheticGetter != null) {
            IrCallImpl(
              startOffset = -1,
              endOffset = -1,
              type = propertyType,
              symbol = syntheticGetter.symbol,
              typeArgumentsCount = 0,
              origin = null,
              superQualifierSymbol = null
            ).apply {
              // In K2 IR, arguments array contains ALL parameters including dispatch receiver
              syntheticGetter.parameters.forEach { param ->
                if (param.kind == IrParameterKind.DispatchReceiver) {
                  arguments[param.indexInParameters] = IrGetValueImpl(
                    startOffset = -1,
                    endOffset = -1,
                    type = ownerType,
                    symbol = ownerParam.symbol
                  )
                }
              }
            }
          } else {
            // Fallback: direct field access (only works for instance methods)
            IrGetFieldImpl(
              startOffset = -1,
              endOffset = -1,
              symbol = property.backingField!!.symbol,
              type = propertyType,
              receiver = IrGetValueImpl(
                startOffset = -1,
                endOffset = -1,
                type = ownerType,
                symbol = ownerParam.symbol
              )
            )
          }
        }

        body = irFactory.createBlockBody(-1, -1).apply {
          statements.add(
            IrReturnImpl(
              startOffset = -1,
              endOffset = -1,
              type = irBuiltIns.nothingType,
              returnTargetSymbol = symbol,
              value = returnValue
            )
          )
        }
      }

      return IrFunctionExpressionImpl(
        startOffset = -1,
        endOffset = -1,
        type = function1Type,
        function = lambdaFunction,
        origin = IrStatementOrigin.LAMBDA
      )
    }

    private fun IrClass.findSyntheticGetter(property: IrProperty): IrSimpleFunction? {
      val name = Identifiers.syntheticGetterName(property.name.asString())
      return declarations
        .filterIsInstance<IrSimpleFunction>()
        .find { it.name.asString() == name }
    }

    private fun IrClass.findSyntheticSetter(property: IrProperty): IrSimpleFunction? {
      val name = Identifiers.syntheticSetterName(property.name.asString())
      return declarations
        .filterIsInstance<IrSimpleFunction>()
        .find { it.name.asString() == name }
    }

    /**
     * Creates a setter lambda: { owner, value -> owner.property = value }
     * Uses the property setter if available, otherwise uses the synthetic accessor
     * to access the backing field.
     */
    fun propertySetterLambda(
      property: IrProperty,
      ownerClass: IrClass,
      containingFunction: IrSimpleFunction,
      irFactory: org.jetbrains.kotlin.ir.declarations.IrFactory
    ): IrExpression? {
      val propertyType = property.getter?.returnType
        ?: property.backingField?.type
        ?: irBuiltIns.anyNType
      val ownerType = ownerClass.defaultType

      // Find setter: either Kotlin setter or synthetic setter
      val kotlinSetter = property.setter
      val syntheticSetter = ownerClass.findSyntheticSetter(property)

      // Need at least one way to set the value
      if (kotlinSetter == null && syntheticSetter == null) return null

      // Create Function2<OwnerType, PropertyType, Unit>
      val function2Type = irBuiltIns.functionN(2).typeWith(ownerType, propertyType, irBuiltIns.unitType)

      val lambdaFunction = irFactory.buildFun {
        name = Name.special("<anonymous>")
        returnType = irBuiltIns.unitType
        origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        visibility = DescriptorVisibilities.LOCAL
      }.apply {
        parent = containingFunction
        val ownerParam = addValueParameter("owner", ownerType)
        val valueParam = addValueParameter("value", propertyType)

        val setterCall = if (kotlinSetter != null) {
          // Use Kotlin setter
          IrCallImpl(
            startOffset = -1,
            endOffset = -1,
            type = irBuiltIns.unitType,
            symbol = kotlinSetter.symbol,
            typeArgumentsCount = 0,
            origin = IrStatementOrigin.EQ,
            superQualifierSymbol = null
          ).apply {
            // In K2 IR, arguments array contains ALL parameters including dispatch receiver
            kotlinSetter.parameters.forEach { param ->
              when (param.kind) {
                IrParameterKind.DispatchReceiver -> {
                  arguments[param.indexInParameters] = IrGetValueImpl(
                    startOffset = -1,
                    endOffset = -1,
                    type = ownerType,
                    symbol = ownerParam.symbol
                  )
                }
                IrParameterKind.Regular -> {
                  arguments[param.indexInParameters] = IrGetValueImpl(
                    startOffset = -1,
                    endOffset = -1,
                    type = propertyType,
                    symbol = valueParam.symbol
                  )
                }
                else -> {}
              }
            }
          }
        } else {
          // Use synthetic setter
          IrCallImpl(
            startOffset = -1,
            endOffset = -1,
            type = irBuiltIns.unitType,
            symbol = syntheticSetter!!.symbol,
            typeArgumentsCount = 0,
            origin = null,
            superQualifierSymbol = null
          ).apply {
            // In K2 IR, arguments array contains ALL parameters including dispatch receiver
            // Use indexInParameters to correctly map each parameter
            syntheticSetter.parameters.forEach { param ->
              when (param.kind) {
                IrParameterKind.DispatchReceiver -> {
                  arguments[param.indexInParameters] = IrGetValueImpl(
                    startOffset = -1,
                    endOffset = -1,
                    type = ownerType,
                    symbol = ownerParam.symbol
                  )
                }
                IrParameterKind.Regular -> {
                  arguments[param.indexInParameters] = IrGetValueImpl(
                    startOffset = -1,
                    endOffset = -1,
                    type = propertyType,
                    symbol = valueParam.symbol
                  )
                }
                else -> {}
              }
            }
          }
        }

        body = irFactory.createBlockBody(-1, -1).apply {
          statements.add(setterCall)
        }
      }

      return IrFunctionExpressionImpl(
        startOffset = -1,
        endOffset = -1,
        type = function2Type,
        function = lambdaFunction,
        origin = IrStatementOrigin.LAMBDA
      )
    }

    /**
     * Creates a delegate getter lambda: { owner -> owner.<delegate_backing_field> }
     * Uses synthetic accessor to access the delegate backing field.
     * Returns null if property is not delegated.
     */
    fun delegateGetterLambda(
      property: IrProperty,
      ownerClass: IrClass,
      containingFunction: IrSimpleFunction,
      irFactory: org.jetbrains.kotlin.ir.declarations.IrFactory
    ): IrExpression? {
      if (!property.isDelegated) return null
      val backingField = property.backingField ?: return null

      val delegateType = backingField.type
      val ownerType = ownerClass.defaultType

      // Function1<OwnerType, Any?>
      val function1Type = irBuiltIns.functionN(1).typeWith(ownerType, irBuiltIns.anyNType)

      // Find synthetic getter for the delegate field
      val syntheticGetter = ownerClass.findSyntheticGetter(property)

      val lambdaFunction = irFactory.buildFun {
        name = Name.special("<anonymous>")
        returnType = irBuiltIns.anyNType
        origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        visibility = DescriptorVisibilities.LOCAL
      }.apply {
        parent = containingFunction
        val ownerParam = addValueParameter("owner", ownerType)

        val returnValue = if (syntheticGetter != null) {
          // Use synthetic getter
          IrCallImpl(
            startOffset = -1,
            endOffset = -1,
            type = delegateType,
            symbol = syntheticGetter.symbol,
            typeArgumentsCount = 0,
            origin = null,
            superQualifierSymbol = null
          ).apply {
            // In K2 IR, arguments array contains ALL parameters including dispatch receiver
            syntheticGetter.parameters.forEach { param ->
              if (param.kind == IrParameterKind.DispatchReceiver) {
                arguments[param.indexInParameters] = IrGetValueImpl(
                  startOffset = -1,
                  endOffset = -1,
                  type = ownerType,
                  symbol = ownerParam.symbol
                )
              }
            }
          }
        } else {
          // Fallback: direct field access
          IrGetFieldImpl(
            startOffset = -1,
            endOffset = -1,
            symbol = backingField.symbol,
            type = delegateType,
            receiver = IrGetValueImpl(
              startOffset = -1,
              endOffset = -1,
              type = ownerType,
              symbol = ownerParam.symbol
            )
          )
        }

        body = irFactory.createBlockBody(-1, -1).apply {
          statements.add(
            IrReturnImpl(
              startOffset = -1,
              endOffset = -1,
              type = irBuiltIns.nothingType,
              returnTargetSymbol = symbol,
              value = returnValue
            )
          )
        }
      }

      return IrFunctionExpressionImpl(
        startOffset = -1,
        endOffset = -1,
        type = function1Type,
        function = lambdaFunction,
        origin = IrStatementOrigin.LAMBDA
      )
    }

    /**
     * io.github.lukmccall.pika.PProperty(...)
     */
    fun pProperty(
      property: IrProperty,
      ownerClass: IrClass,
      containingFunction: IrSimpleFunction,
      irFactory: org.jetbrains.kotlin.ir.declarations.IrFactory
    ): IrExpression {
      val propertyType = property.getter?.returnType
        ?: property.backingField?.type
        ?: irBuiltIns.anyNType

      val pPropertyClass = symbolFinder.pikaAPI.pProperty
      val pAnnotationClass = symbolFinder.pikaAPI.pAnnotation

      val constructor = pPropertyClass.owner.primaryConstructor
        ?: error("PProperty must have a primary constructor")

      val annotations = property.annotations.map { annotation ->
        pika.pAnnotation(annotation)
      }

      val hasBackingField = property.backingField != null
      val getterLambda = propertyGetterLambda(property, ownerClass, containingFunction, irFactory)
      val setterLambda = propertySetterLambda(property, ownerClass, containingFunction, irFactory)
      val isDelegated = property.isDelegated
      val delegateLambda = delegateGetterLambda(property, ownerClass, containingFunction, irFactory)

      val ownerType = ownerClass.defaultType

      // PProperty<OwnerType, Type>
      val pPropertyType = pPropertyClass.typeWith(ownerType, propertyType)

      return IrConstructorCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = pPropertyType,
        symbol = constructor.symbol,
        typeArgumentsCount = 2,
        constructorTypeArgumentsCount = 2,
        origin = null
      ).also { call ->
        call.typeArguments[0] = ownerType
        call.typeArguments[1] = propertyType
        call.arguments[0] = kotlin.string(property.name.asString())
        call.arguments[1] = pika.pVisibility(property.visibility)
        call.arguments[2] = kotlin.listOf(pAnnotationClass.owner.defaultType, annotations)
        call.arguments[3] = pika.pTypeDescriptor(propertyType)
        call.arguments[4] = getterLambda
        call.arguments[5] = kotlin.bool(property.isVar)
        call.arguments[6] = kotlin.bool(hasBackingField)
        call.arguments[7] = setterLambda ?: kotlin.`null`()
        call.arguments[8] = kotlin.bool(isDelegated)
        call.arguments[9] = delegateLambda ?: kotlin.`null`()
      }
    }

    /**
     * io.github.lukmccall.pika.PIntrospectionData(...)
     */
    fun pIntrospectionData(
      irClass: IrClass,
      properties: List<IrExpression>,
      functions: List<IrExpression>,
      baseClassExpr: IrExpression?,
    ): IrExpression {
      val pIntrospectionDataClass = symbolFinder.pikaAPI.pIntrospectionData
      val pAnnotationClass = symbolFinder.pikaAPI.pAnnotation
      val pPropertyClass = symbolFinder.pikaAPI.pProperty
      val pFunctionClass = symbolFinder.pikaAPI.pFunction

      val constructor = pIntrospectionDataClass.owner.primaryConstructor
        ?: error("PIntrospectionData must have a primary constructor")

      val classAnnotations = irClass.annotations.map { annotation ->
        pika.pAnnotation(annotation)
      }

      val ownerType = irClass.defaultType

      // PIntrospectionData<OwnerType>
      val pIntrospectionDataType = pIntrospectionDataClass.typeWith(ownerType)

      // PProperty<OwnerType, *> (use star projection for the list)
      val pPropertyStarType = pPropertyClass.typeWith(ownerType, irBuiltIns.anyNType)

      return IrConstructorCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = pIntrospectionDataType,
        symbol = constructor.symbol,
        typeArgumentsCount = 1,
        constructorTypeArgumentsCount = 1,
        origin = null
      ).also { call ->
        call.typeArguments[0] = ownerType
        call.arguments[0] = kotlin.kClass(irClass.symbol)
        call.arguments[1] = kotlin.listOf(pAnnotationClass.owner.defaultType, classAnnotations)
        call.arguments[2] = kotlin.listOf(pPropertyStarType, properties)
        call.arguments[3] = kotlin.listOf(pFunctionClass.owner.defaultType, functions)
        call.arguments[4] = baseClassExpr ?: kotlin.`null`()
      }
    }

    fun pIsIntrospectable(type: IrType): IrExpression {
      val simpleType = type as? IrSimpleType
        ?: return kotlin.bool(false)
      val irClass = simpleType.classOrNull?.owner
        ?: return kotlin.bool(false)
      return kotlin.bool(irClass.hasIntrospectableAnnotation())
    }

    fun pIntrospectionOf(instance: IrExpression, originalCall: IrCall): IrExpression {
      val instanceType = instance.type as? IrSimpleType
        ?: return originalCall

      val irClass = instanceType.classOrNull?.owner
        ?: return originalCall

      // Find the __PIntrospectionData function
      // For object declarations: look on the object itself
      // For regular classes: look in the companion object
      val (functionOwner, introspectionDataFunction) = if (irClass.isObject && !irClass.isCompanion) {
        val function = irClass.declarations
          .filterIsInstance<IrSimpleFunction>()
          .find { it.name.asString() == Identifiers.P_INTROSPECTION_DATA_FUNCTION_NAME }
        irClass to function
      } else {
        val companion = irClass.companionObject()
        val function = companion?.declarations
          ?.filterIsInstance<IrSimpleFunction>()
          ?.find { it.name.asString() == Identifiers.P_INTROSPECTION_DATA_FUNCTION_NAME }
        companion to function
      }

      if (introspectionDataFunction == null || functionOwner == null) {
        return originalCall
      }

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
        // For objects: dispatch receiver is the instance itself
        // For regular classes: dispatch receiver is the companion object
        val dispatchReceiverValue = if (irClass.isObject && !irClass.isCompanion) {
          instance
        } else {
          kotlin.getObject(functionOwner.symbol)
        }
        // In K2 IR, arguments array contains ALL parameters including dispatch receiver
        introspectionDataFunction.parameters.forEach { param ->
          if (param.kind == IrParameterKind.DispatchReceiver) {
            arguments[param.indexInParameters] = dispatchReceiverValue
          }
        }
      }
    }


  }
}
