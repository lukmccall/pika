@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.lukmccall.pika.ir

import io.github.lukmccall.pika.Identifiers
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.createExtensionReceiver
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class IRPoet(
  context: IrPluginContext,
  private val symbolFinder: SymbolFinder,
  moduleFragment: IrModuleFragment,
  val extraAnnotationClassIds: Set<ClassId> = emptySet(),
) {
  val irBuiltIns = context.irBuiltIns
  val kotlin = Kotlin()

  /**
   * Synthetic symbol for the `kotlin.jvm.<get-java>` extension property on `KClass<*>`.
   * The JVM backend matches this by (package, receiver, name) and codegens a bare
   * `LDC LType;` without the `Reflection.getOrCreateKotlinClass` wrap.
   */
  private val kClassJavaGetter: IrSimpleFunctionSymbol = run {
    val kotlinJvmPackage = createEmptyExternalPackageFragment(
      moduleFragment.descriptor,
      FqName("kotlin.jvm")
    )
    IrFactoryImpl.buildProperty {
      name = Name.identifier("java")
    }.apply {
      parent = kotlinJvmPackage
      addGetter().apply {
        parameters += createExtensionReceiver(irBuiltIns.kClassClass.starProjectedType)
        returnType = symbolFinder.javaLangClass.defaultType
      }
    }.getter!!.symbol
  }

  fun createReturnBody(
    irFactory: IrFactory,
    returnTarget: IrSimpleFunction,
    value: IrExpression
  ): IrBlockBody = irFactory.createBlockBody(-1, -1).apply {
    statements.add(
      IrReturnImpl(
        -1,
        -1,
        irBuiltIns.nothingType,
        returnTarget.symbol,
        value
      )
    )
  }

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

    private fun buildIrCall(
      type: IrType,
      symbol: IrSimpleFunctionSymbol,
      typeArgumentsCount: Int = 0,
      origin: IrStatementOrigin? = null,
      setup: IrCallImpl.() -> Unit = {}
    ): IrExpression =
      IrCallImpl(
        -1,
        -1,
        type,
        symbol,
        typeArgumentsCount,
        origin,
        null
      ).also { it.setup() }

    /**
     * {key} to {value} -> Pair<{keyType}, {valueType}>
     */
    fun pair(keyType: IrType, key: IrExpression, valueType: IrType, value: IrExpression): IrExpression {
      val pairType = symbolFinder.kotlinStd.pair.typeWith(keyType, valueType)
      return buildIrCall(pairType, symbolFinder.kotlinStd.to.first(), typeArgumentsCount = 2) {
        typeArguments[0] = keyType
        typeArguments[1] = valueType
        arguments[0] = key
        arguments[1] = value
      }
    }

    /**
     * mapOf<{keyType}, {valueType}>({pairs})
     */
    fun mapOf(keyType: IrType, valueType: IrType, pairs: List<IrExpression>): IrExpression {
      val mapType = irBuiltIns.mapClass.typeWith(keyType, valueType)
      val pairType = symbolFinder.kotlinStd.pair.typeWith(keyType, valueType)
      val mapOf = symbolFinder.kotlinStd.collections.mapOf.firstSingleVarargsArgument()
      return buildIrCall(mapType, mapOf, typeArgumentsCount = 2, origin = IrStatementOrigin.INVOKE) {
        typeArguments[0] = keyType
        typeArguments[1] = valueType
        arguments[0] = IrVarargImpl(
          startOffset = -1, endOffset = -1,
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
      val mapType = irBuiltIns.mapClass.typeWith(keyType, valueType)
      val emptyMap = symbolFinder.kotlinStd.collections.emptyMap.single()
      return buildIrCall(mapType, emptyMap, typeArgumentsCount = 2) {
        typeArguments[0] = keyType
        typeArguments[1] = valueType
      }
    }

    /**
     * arrayOf<{type}>({elements})
     */
    fun arrayOf(type: IrType, elements: List<IrExpression>): IrExpression {
      val arrayType = irBuiltIns.arrayClass.typeWith(type)
      return IrCallImpl(
        -1,
        -1,
        arrayType,
        irBuiltIns.arrayOf,
        typeArgumentsCount = 1,
        origin = null
      ).apply {
        typeArguments[0] = type
        arguments[0] = IrVarargImpl(-1, -1, arrayType, type, elements)
      }
    }

    /**
     * listOf<{type}>({elements})
     */
    fun listOf(type: IrType, elements: List<IrExpression>): IrExpression {
      val listType = irBuiltIns.listClass.typeWith(type)
      val listOf = symbolFinder.kotlinStd.collections.listOf.firstSingleVarargsArgument()
      return buildIrCall(listType, listOf, typeArgumentsCount = 1, origin = IrStatementOrigin.INVOKE) {
        typeArguments[0] = type
        arguments[0] = IrVarargImpl(
          startOffset = -1, endOffset = -1,
          type = irBuiltIns.arrayClass.typeWith(type),
          varargElementType = type,
          elements = elements
        )
      }
    }

    /**
     * Class<{classSymbol}> — emits `KClass<{classSymbol}>.java` so the JVM backend's
     * KClassJavaProperty intrinsic strips the KClass wrap, leaving a bare LDC.
     */
    fun javaClass(classSymbol: IrClassSymbol): IrExpression {
      val classType = classSymbol.owner.defaultType
      val kClassReference = IrClassReferenceImpl(
        startOffset = -1,
        endOffset = -1,
        type = irBuiltIns.kClassClass.starProjectedType,
        symbol = classSymbol,
        classType = classType
      )
      return IrCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = symbolFinder.javaLangClass.typeWith(classType),
        symbol = kClassJavaGetter,
        typeArgumentsCount = 0,
        origin = null,
        superQualifierSymbol = null
      ).apply {
        arguments[0] = kClassReference
      }
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
    private val IrProperty.resolvedType: IrType
      get() = getter?.returnType ?: backingField?.type ?: irBuiltIns.anyNType

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

    private fun IrClassSymbol.buildConstructorCall(
      errorName: String,
      setup: IrConstructorCall.() -> Unit
    ): IrExpression {
      val constructor = owner.primaryConstructor
        ?: error("$errorName must have a primary constructor")
      return IrConstructorCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = owner.defaultType,
        symbol = constructor.symbol,
        typeArgumentsCount = 0,
        constructorTypeArgumentsCount = 0,
        origin = null
      ).also { it.setup() }
    }

    /**
     * PTypeDescriptorRegistry.getOrCreateConcrete({Class<{classSymbol}>}, {isNullable}, {introspection})
     */
    fun concrete(
      classSymbol: IrClassSymbol,
      isNullable: Boolean,
      introspection: IrExpression? = null
    ): IrExpression {
      val registry = symbolFinder.pikaAPI.pTypeDescriptorRegistry
      val fnSymbol = registry.getOrCreateConcrete
      val valueArgs = listOf(
        kotlin.javaClass(classSymbol),
        kotlin.bool(isNullable),
        introspection ?: kotlin.`null`()
      )
      return IrCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = symbolFinder.pikaAPI.pTypeDescriptor.concrete.owner.defaultType,
        symbol = fnSymbol,
        typeArgumentsCount = 0,
        origin = null,
      ).apply {
        var valueArgIdx = 0
        fnSymbol.owner.parameters.forEach { param ->
          when (param.kind) {
            IrParameterKind.DispatchReceiver -> arguments[param.indexInParameters] =
              IrGetObjectValueImpl(
                -1,
                -1,
                registry.classSymbol.owner.defaultType,
                registry.classSymbol
              )

            IrParameterKind.Regular -> arguments[param.indexInParameters] = valueArgs[valueArgIdx++]
            else -> {}
          }
        }
      }
    }

    /**
     * PTypeDescriptorRegistry.getOrCreateParameterized(
     *   {Class<{classSymbol}>}, {isNullable}, *{parameters}, {introspection}
     * )
     */
    fun parameterized(
      classSymbol: IrClassSymbol,
      isNullable: Boolean,
      parameters: List<IrExpression>,
      introspection: IrExpression? = null
    ): IrExpression {
      val registry = symbolFinder.pikaAPI.pTypeDescriptorRegistry
      val pTypeDescriptorType = symbolFinder.pikaAPI.pTypeDescriptor.root.owner.defaultType
      val introspectionArg = introspection ?: kotlin.`null`()

      val fnSymbol = registry.getOrCreateParameterized
      val valueArgs = listOf(
        kotlin.javaClass(classSymbol),
        kotlin.bool(isNullable),
        kotlin.listOf(pTypeDescriptorType, parameters),
        introspectionArg,
      )

      return IrCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = symbolFinder.pikaAPI.pTypeDescriptor.parameterized.owner.defaultType,
        symbol = fnSymbol,
        typeArgumentsCount = 0,
        origin = null,
      ).apply {
        var valueArgIdx = 0
        fnSymbol.owner.parameters.forEach { param ->
          when (param.kind) {
            IrParameterKind.DispatchReceiver -> arguments[param.indexInParameters] =
              IrGetObjectValueImpl(
                -1,
                -1,
                registry.classSymbol.owner.defaultType,
                registry.classSymbol
              )

            IrParameterKind.Regular -> arguments[param.indexInParameters] = valueArgs[valueArgIdx++]
            else -> {}
          }
        }
      }
    }

    private val simpleTypeFieldNames: Map<IrClassifierSymbol, Pair<String, String>> by lazy {
      mapOf(
        irBuiltIns.intClass to ("INT" to "INT_NULLABLE"),
        irBuiltIns.longClass to ("LONG" to "LONG_NULLABLE"),
        irBuiltIns.floatClass to ("FLOAT" to "FLOAT_NULLABLE"),
        irBuiltIns.shortClass to ("SHORT" to "SHORT_NULLABLE"),
        irBuiltIns.doubleClass to ("DOUBLE" to "DOUBLE_NULLABLE"),
        irBuiltIns.stringClass to ("STRING" to "STRING_NULLABLE"),
        irBuiltIns.booleanClass to ("BOOLEAN" to "BOOLEAN_NULLABLE"),
      )
    }

    fun staticFieldDescriptor(fieldName: String): IrExpression {
      val registry = symbolFinder.pikaAPI.pTypeDescriptorRegistry
      val concreteType = symbolFinder.pikaAPI.pTypeDescriptor.concrete.owner.defaultType
      val field = irBuiltIns.irFactory.createField(
        startOffset = -1,
        endOffset = -1,
        origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
        name = Name.identifier(fieldName),
        visibility = DescriptorVisibilities.PUBLIC,
        symbol = IrFieldSymbolImpl(),
        type = concreteType,
        isFinal = true,
        isStatic = true,
        isExternal = false,
      ).also { it.parent = registry.classSymbol.owner }
      return IrGetFieldImpl(
        startOffset = -1,
        endOffset = -1,
        symbol = field.symbol,
        type = concreteType,
        receiver = null,
      )
    }

    private fun staticArrayField(fieldName: String, elementType: IrType, parentClass: IrClass): IrExpression {
      val arrayType = irBuiltIns.arrayClass.typeWith(elementType)
      val field = irBuiltIns.irFactory.createField(
        startOffset = -1,
        endOffset = -1,
        origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
        name = Name.identifier(fieldName),
        visibility = DescriptorVisibilities.PUBLIC,
        symbol = IrFieldSymbolImpl(),
        type = arrayType,
        isFinal = true,
        isStatic = true,
        isExternal = false,
      ).also { it.parent = parentClass }
      return IrGetFieldImpl(-1, -1, field.symbol, arrayType, receiver = null)
    }

    fun emptyAnnotationsArray(): IrExpression =
      staticArrayField("EMPTY_ANNOTATIONS", symbolFinder.pikaAPI.pAnnotation.owner.defaultType, symbolFinder.pikaAPI.pEmptyArrays.owner)

    fun emptyFunctionsArray(): IrExpression =
      staticArrayField("EMPTY_FUNCTIONS", symbolFinder.pikaAPI.pFunction.owner.defaultType, symbolFinder.pikaAPI.pEmptyArrays.owner)

    /**
     * IrType -> io.github.lukmccall.pika.PTypeDescriptor.*
     */
    fun typeDescriptor(type: IrType): IrExpression {
      val simpleType = type as? IrSimpleType
        ?: return pika.concrete(irBuiltIns.anyClass, false)

      val classifier = simpleType.classifier
      val isNullable = simpleType.isMarkedNullable()

      if (simpleType.arguments.isEmpty()) {
        val fieldNames = simpleTypeFieldNames[classifier]
        if (fieldNames != null) {
          val fieldName = if (isNullable) {
            fieldNames.second
          } else {
            fieldNames.first
          }
          return pika.staticFieldDescriptor(fieldName)
        }
      }

      val irClass = (classifier as? IrClassSymbol)?.owner
      val introspection = irClass?.let { buildIntrospectionCallFor(it) }

      return if (simpleType.arguments.isEmpty()) {
        pika.concrete(classifier as IrClassSymbol, isNullable, introspection)
      } else {
        val typeArgInfos = simpleType.arguments.map { arg ->
          when (arg) {
            is IrTypeProjection -> typeDescriptor(arg.type)
            is IrStarProjection -> pika.star()
          }
        }
        pika.parameterized(classifier as IrClassSymbol, isNullable, typeArgInfos, introspection)
      }
    }

    private fun buildIntrospectionCallFor(
      irClass: IrClass,
      startOffset: Int = -1,
      endOffset: Int = -1
    ): IrExpression? {
      if (!irClass.hasIntrospectableAnnotation(extraAnnotationClassIds)) {
        return null
      }

      val fieldOwner = if (irClass.isObject && !irClass.isCompanion) {
        irClass
      } else {
        irClass.companionObject() ?: return null
      }

      val field = fieldOwner.declarations
        .filterIsInstance<IrField>()
        .find { it.name.asString() == Identifiers.INTROSPECTION_DATA_FIELD_NAME }
        ?: return null

      val pIntrospectionDataClass = symbolFinder.pikaAPI.pIntrospectionData
      val returnType = pIntrospectionDataClass.typeWith(irClass.defaultType)

      return IrGetFieldImpl(
        startOffset = startOffset,
        endOffset = endOffset,
        symbol = field.symbol,
        type = returnType,
        receiver = null
      )
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
        .filterIsInstance<IrEnumEntry>()
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
     * io.github.lukmccall.pika.PAnnotation({jClass}, mapOf({annotationArgs}))
     */
    fun pAnnotation(annotation: IrConstructorCall): IrExpression {
      val annotationClass = annotation.type.classOrNull?.owner
      val argumentsMap = annotation.toArgsMap(this@IRPoet)
      return symbolFinder
        .pikaAPI
        .pAnnotation
        .buildConstructorCall("PAnnotation") {
          arguments[0] = kotlin.javaClass(annotationClass?.symbol ?: irBuiltIns.anyClass)
          arguments[1] = argumentsMap
        }
    }

    /**
     * io.github.lukmccall.pika.PFunction({name}, {visibility})
     */
    fun pFunction(function: IrSimpleFunction): IrExpression =
      symbolFinder
        .pikaAPI
        .pFunction
        .buildConstructorCall("PFunction") {
          arguments[0] = kotlin.string(function.name.asString())
          arguments[1] = pika.pVisibility(function.visibility)
        }

    /**
     * Creates a getter lambda: { owner -> owner.propertyName }
     * Uses the property getter if available, otherwise uses the synthetic accessor.
     */
    fun propertyGetterLambda(
      property: IrProperty,
      ownerClass: IrClass,
      containingDeclaration: IrDeclarationParent,
      irFactory: IrFactory
    ): IrExpression {
      val propertyType = property.resolvedType

      val ownerType = ownerClass.defaultType

      // Create Function1<OwnerType, PropertyType>
      val function1Type = irBuiltIns.functionN(1).typeWith(ownerType, propertyType)

      val lambdaFunction = irFactory.buildFun {
        name = Name.special("<anonymous>")
        returnType = propertyType
        origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        visibility = DescriptorVisibilities.LOCAL
      }.apply {
        parent = containingDeclaration
        val ownerParam = addValueParameter("owner", ownerType)

        val returnValue = if (property.getter != null) {
          // Call the getter
          val getter = property.getter!!
          IrCallImpl(
            -1,
            -1,
            propertyType,
            getter.symbol,
            0,
            IrStatementOrigin.GET_PROPERTY,
            null
          )
            .apply {
              bindFunctionParameters(
                getter,
                ownerParam,
                ownerType
              )
            }
        } else {
          // Use synthetic getter to access backing field
          val syntheticGetter = ownerClass.findSyntheticGetter(property)
          if (syntheticGetter != null) {
            IrCallImpl(
              -1,
              -1,
              propertyType,
              syntheticGetter.symbol,
              0,
              null,
              null
            )
              .apply {
                bindFunctionParameters(
                  syntheticGetter,
                  ownerParam,
                  ownerType
                )
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

        body = createReturnBody(irFactory, this, returnValue)
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
     * Sets arguments for dispatch receiver (and optionally a regular/value parameter)
     * on a function call, using K2 IR's unified `arguments` array indexed by `indexInParameters`.
     */
    private fun IrCallImpl.bindFunctionParameters(
      function: IrSimpleFunction,
      dispatchParam: IrValueParameter,
      dispatchType: IrType,
      regularParam: IrValueParameter? = null,
      regularType: IrType? = null
    ) {
      function.parameters.forEach { param ->
        when (param.kind) {
          IrParameterKind.DispatchReceiver ->
            arguments[param.indexInParameters] = IrGetValueImpl(
              -1,
              -1,
              dispatchType,
              dispatchParam.symbol
            )

          IrParameterKind.Regular -> if (regularParam != null) {
            arguments[param.indexInParameters] = IrGetValueImpl(
              -1,
              -1,
              regularType!!,
              regularParam.symbol
            )
          }

          else -> {}
        }
      }
    }

    private fun IrClass.findSyntheticAccessor(
      property: IrProperty,
      nameTransform: (String) -> String
    ): IrSimpleFunction? {
      val name = nameTransform(property.name.asString())
      return declarations
        .filterIsInstance<IrSimpleFunction>()
        .find { it.name.asString() == name }
    }

    private fun IrClass.findSyntheticGetter(property: IrProperty) =
      findSyntheticAccessor(property, Identifiers::syntheticGetterName)

    private fun IrClass.findSyntheticSetter(property: IrProperty) =
      findSyntheticAccessor(property, Identifiers::syntheticSetterName)

    /**
     * Creates a setter lambda: { owner, value -> owner.property = value }
     * Uses the property setter if available, otherwise uses the synthetic accessor
     * to access the backing field.
     */
    fun propertySetterLambda(
      property: IrProperty,
      ownerClass: IrClass,
      containingDeclaration: IrDeclarationParent,
      irFactory: IrFactory
    ): IrExpression? {
      val propertyType = property.resolvedType
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
        parent = containingDeclaration
        val ownerParam = addValueParameter("owner", ownerType)
        val valueParam = addValueParameter("value", propertyType)

        val setterCall = if (kotlinSetter != null) {
          IrCallImpl(
            -1,
            -1,
            irBuiltIns.unitType,
            kotlinSetter.symbol,
            0,
            IrStatementOrigin.EQ,
            null
          )
            .apply {
              bindFunctionParameters(
                kotlinSetter,
                ownerParam,
                ownerType,
                valueParam,
                propertyType
              )
            }
        } else {
          IrCallImpl(
            -1,
            -1,
            irBuiltIns.unitType,
            syntheticSetter!!.symbol,
            0,
            null,
            null
          )
            .apply {
              bindFunctionParameters(
                syntheticSetter,
                ownerParam,
                ownerType,
                valueParam,
                propertyType
              )
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
      containingDeclaration: IrDeclarationParent,
      irFactory: IrFactory
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
        parent = containingDeclaration
        val ownerParam = addValueParameter("owner", ownerType)

        val returnValue = if (syntheticGetter != null) {
          IrCallImpl(
            -1,
            -1,
            delegateType,
            syntheticGetter.symbol,
            0,
            null,
            null
          )
            .apply {
              bindFunctionParameters(
                syntheticGetter,
                ownerParam,
                ownerType
              )
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

        body = createReturnBody(irFactory, this, returnValue)
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
      containingDeclaration: IrDeclarationParent,
      irFactory: IrFactory
    ): IrExpression {
      val propertyType = property.resolvedType

      val pPropertyClass = symbolFinder.pikaAPI.pProperty
      val pAnnotationClass = symbolFinder.pikaAPI.pAnnotation

      val constructor = pPropertyClass.owner.primaryConstructor
        ?: error("PProperty must have a primary constructor")

      val annotations = property.annotations.map { annotation ->
        pika.pAnnotation(annotation)
      }

      val hasBackingField = property.backingField != null
      val getterLambda = propertyGetterLambda(property, ownerClass, containingDeclaration, irFactory)
      val setterLambda = propertySetterLambda(property, ownerClass, containingDeclaration, irFactory)
      val isDelegated = property.isDelegated
      val delegateLambda = delegateGetterLambda(property, ownerClass, containingDeclaration, irFactory)

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
        call.arguments[2] = if (annotations.isEmpty()) {
          pika.emptyAnnotationsArray()
        } else {
          kotlin.arrayOf(pAnnotationClass.owner.defaultType, annotations)
        }
        call.arguments[3] = pika.typeDescriptor(propertyType)
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
      irFactory: IrFactory,
      containingDeclaration: IrDeclarationParent
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

      // PProperty<OwnerType, *> (use star projection for the array)
      val pPropertyStarType = pPropertyClass.typeWith(ownerType, irBuiltIns.anyNType)

      // Build properties factory lambda: () -> Array<PProperty<OwnerType, *>>
      val propertiesArrayType = irBuiltIns.arrayClass.typeWith(pPropertyStarType)
      val function0Type = irBuiltIns.functionN(0).typeWith(propertiesArrayType)

      val propertiesArrayExpr = if (properties.isEmpty()) {
        kotlin.arrayOf(pPropertyStarType, emptyList())
      } else {
        kotlin.arrayOf(pPropertyStarType, properties)
      }

      val lambdaFunction = irFactory.buildFun {
        name = Name.special("<anonymous>")
        returnType = propertiesArrayType
        origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        visibility = DescriptorVisibilities.LOCAL
      }.apply {
        parent = containingDeclaration
        body = createReturnBody(irFactory, this, propertiesArrayExpr)
      }

      val propertiesLambda = IrFunctionExpressionImpl(
        startOffset = -1,
        endOffset = -1,
        type = function0Type,
        function = lambdaFunction,
        origin = IrStatementOrigin.LAMBDA
      )

      // Annotations array
      val annotationsExpr = if (classAnnotations.isEmpty()) {
        pika.emptyAnnotationsArray()
      } else {
        kotlin.arrayOf(pAnnotationClass.owner.defaultType, classAnnotations)
      }

      // Functions array
      val functionsExpr = if (functions.isEmpty()) {
        pika.emptyFunctionsArray()
      } else {
        kotlin.arrayOf(pFunctionClass.owner.defaultType, functions)
      }

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
        call.arguments[0] = kotlin.javaClass(irClass.symbol)
        call.arguments[1] = annotationsExpr
        call.arguments[2] = propertiesLambda
        call.arguments[3] = functionsExpr
        call.arguments[4] = baseClassExpr ?: kotlin.`null`()
      }
    }

    fun isIntrospectable(type: IrType): IrExpression {
      val simpleType = type as? IrSimpleType
        ?: return kotlin.bool(false)
      val irClass = simpleType.classOrNull?.owner
        ?: return kotlin.bool(false)
      return kotlin.bool(irClass.hasIntrospectableAnnotation(extraAnnotationClassIds))
    }

    fun introspectionOf(type: IrType, originalCall: IrCall): IrExpression {
      val irClass = (type as? IrSimpleType)?.classOrNull?.owner ?: return originalCall
      return buildIntrospectionCallFor(irClass, originalCall.startOffset, originalCall.endOffset)
        ?: originalCall
    }
  }
}
