@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.lukmccall.pika.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.primaryConstructor

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
     * emptyList<{type}>()
     */
    fun emptyList(type: IrType): IrExpression {
      val listType = irBuiltIns
        .listClass
        .typeWith(type)

      val emptyList = symbolFinder
        .kotlinStd
        .collections
        .emptyList
        .single()

      return IrCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = listType,
        symbol = emptyList,
        typeArgumentsCount = 1,
        origin = null,
        superQualifierSymbol = null
      ).also { call ->
        call.typeArguments[0] = type
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
  }

  val pika = Pika()

  inner class Pika {
    /**
     * io.github.lukmccall.pika.Visibility.{value}
     */
    fun visibility(value: DescriptorVisibility): IrExpression {
      val visibility = when (value) {
        DescriptorVisibilities.PUBLIC -> "PUBLIC"
        DescriptorVisibilities.PRIVATE -> "PRIVATE"
        DescriptorVisibilities.PROTECTED -> "PROTECTED"
        DescriptorVisibilities.INTERNAL -> "INTERNAL"
        else -> "PUBLIC"
      }

      val visibilityEnumClass = symbolFinder.pikaAPI.visibility

      val enumEntry = visibilityEnumClass
        .owner
        .declarations
        .filterIsInstance<org.jetbrains.kotlin.ir.declarations.IrEnumEntry>()
        .find { it.name.asString() == visibility }
        ?: error("Cannot find Visibility.$value")

      return IrGetEnumValueImpl(
        startOffset = -1,
        endOffset = -1,
        type = visibilityEnumClass.owner.defaultType,
        symbol = enumEntry.symbol
      )
    }

    /**
     * io.github.lukmccall.pika.TypeInfo.Star
     */
    fun star(): IrExpression {
      val typeInfoStarClass = symbolFinder.pikaAPI.typeInfo.star

      return IrGetObjectValueImpl(
        startOffset = -1,
        endOffset = -1,
        type = typeInfoStarClass.owner.defaultType,
        symbol = typeInfoStarClass
      )
    }

    /**
     * io.github.lukmccall.pika.TypeInfo.Simple({typeName}, {KClass<{classSymbol}>}, {isNullable})
     */
    fun simple(
      typeName: String,
      classSymbol: IrClassSymbol,
      isNullable: Boolean
    ): IrExpression {
      val typeInfoSimpleClass = symbolFinder.pikaAPI.typeInfo.simple

      val constructor = typeInfoSimpleClass.owner.primaryConstructor
        ?: error("TypeInfo.Simple must have a primary constructor")

      return IrConstructorCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = typeInfoSimpleClass.owner.defaultType,
        symbol = constructor.symbol,
        typeArgumentsCount = 0,
        constructorTypeArgumentsCount = 0,
        origin = null
      ).also { call ->
        call.arguments[0] = kotlin.string(typeName)
        call.arguments[1] = kotlin.kClass(classSymbol)
        call.arguments[2] = kotlin.bool(isNullable)
      }
    }

    /**
     * io.github.lukmccall.pika.TypeInfo.Parameterized(
     *   {typeName},
     *   {KClass<{classSymbol}>},
     *   {isNullable},
     *   {listOf({typeArguments)}
     * )
     */
    fun parameterized(
      typeName: String,
      classSymbol: IrClassSymbol,
      isNullable: Boolean,
      typeArguments: List<IrExpression>
    ): IrExpression {
      val typeInfoParameterizedClass = symbolFinder.pikaAPI.typeInfo.parameterized
      val typeInfoClass = symbolFinder.pikaAPI.typeInfo.root

      val constructor = typeInfoParameterizedClass.owner.primaryConstructor
        ?: error("TypeInfo.Parameterized must have a primary constructor")

      return IrConstructorCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = typeInfoParameterizedClass.owner.defaultType,
        symbol = constructor.symbol,
        typeArgumentsCount = 0,
        constructorTypeArgumentsCount = 0,
        origin = null
      ).also { call ->
        call.arguments[0] = kotlin.string(typeName)
        call.arguments[1] = kotlin.kClass(classSymbol)
        call.arguments[2] = kotlin.bool(isNullable)
        call.arguments[3] = kotlin.listOf(typeInfoClass.owner.defaultType, typeArguments)
      }
    }

    /**
     * IrType -> io.github.lukmccall.pika.TypeInfo.*
     */
    fun typeInfo(type: IrType): IrExpression {
      val simpleType = type as? IrSimpleType
        ?: return pika.simple("Unknown", irBuiltIns.anyClass, false)

      val classifier = simpleType.classifier
      val irClass = classifier.owner as? IrClass
      val typeName = irClass?.kotlinFqName?.asString() ?: "Unknown"
      val isNullable = simpleType.isMarkedNullable()

      return if (simpleType.arguments.isEmpty()) {
        pika.simple(typeName, classifier as IrClassSymbol, isNullable)
      } else {
        val typeArgInfos = simpleType.arguments.map { arg ->
          when (arg) {
            is IrTypeProjection -> typeInfo(arg.type)
            is IrStarProjection -> pika.star()
          }
        }
        pika.parameterized(typeName, classifier as IrClassSymbol, isNullable, typeArgInfos)
      }
    }

    /**
     * example.Annotation -> io.github.lukmccall.pika.AnnotationInfo({annotationName}, KClass<{annotation}>, mapOf({annotationArgs})
     */
    fun annotationInfo(
      annotation: IrConstructorCall
    ): IrExpression {
      val annotationClass = annotation.type.classOrNull?.owner
      val className = annotationClass?.kotlinFqName?.asString() ?: "Unknown"

      val annotationInfoClass = symbolFinder.pikaAPI.annotationInfo

      val constructor = annotationInfoClass.owner.primaryConstructor
        ?: error("AnnotationInfo must have a primary constructor")

      val argumentsMap = annotation.toArgsMap(this@IRPoet)

      return IrConstructorCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = annotationInfoClass.owner.defaultType,
        symbol = constructor.symbol,
        typeArgumentsCount = 0,
        constructorTypeArgumentsCount = 0,
        origin = null
      ).also { call ->
        call.arguments[0] = kotlin.string(className)
        call.arguments[1] = kotlin.kClass(
          annotationClass?.symbol ?: irBuiltIns.anyClass
        )
        call.arguments[2] = argumentsMap
      }
    }

    /**
     * IrProperty -> io.github.lukmccall.pika.FullFieldInfo
     */
    fun fullFieldInfo(
      property: IrProperty
    ): IrExpression {
      val propertyType = property
        .getter
        ?.returnType
        ?: property.backingField?.type
        ?: irBuiltIns.anyNType

      val fullFieldInfoClass = symbolFinder.pikaAPI.fullFieldInfo
      val annotationInfoClass = symbolFinder.pikaAPI.annotationInfo

      val constructor = fullFieldInfoClass.owner.primaryConstructor
        ?: error("FullFieldInfo must have a primary constructor")

      val annotations = property.annotations.map { annotation ->
        pika.annotationInfo(annotation)
      }

      return IrConstructorCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = fullFieldInfoClass.owner.defaultType,
        symbol = constructor.symbol,
        typeArgumentsCount = 0,
        constructorTypeArgumentsCount = 0,
        origin = null
      ).also { call ->
        call.arguments[0] = kotlin.string(property.name.asString())
        call.arguments[1] = pika.typeInfo(propertyType)
        call.arguments[2] = kotlin.listOf(
          type = annotationInfoClass.owner.defaultType,
          elements = annotations
        )
        call.arguments[3] = pika.visibility(property.visibility)
        call.arguments[4] = kotlin.bool(property.isVar)
      }
    }

    /**
     * IrType -> io.github.lukmccall.pika.FullTypeInfo
     */
    fun fullTypeInfo(type: IrType, isNullable: Boolean): IrExpression {
      val simpleType = type as? IrSimpleType
        ?: error("TypeInfo.Parameterized must have a simple type")

      val classifier = simpleType.classifier
      val irClass = classifier.owner as? IrClass
        ?: error("TypeInfo.Parameterized must have a class")

      val className = irClass.kotlinFqName.asString()

      // Build field info only for properties declared directly on this class
      val properties = irClass.declarations.filterIsInstance<IrProperty>()
      val fieldInfoList = properties.map { property ->
        pika.fullFieldInfo(property)
      }

      val interfaceKClasses = mutableListOf<IrExpression>()

      val superTypes = irClass
        .superTypes
        .filterIsInstance<IrSimpleType>()

      interfaceKClasses.addAll(
        superTypes
          .filter { it.isInterface() }
          .mapNotNull { it.classOrNull?.owner }
          .map { kotlin.kClass(it.symbol) }
      )

      val baseClass = superTypes
        .singleOrNull { !it.isInterface() && !it.isAny() }

      val baseClassInfo = if (baseClass != null) {
        pika.fullTypeInfo(baseClass, false)
      } else {
        kotlin.`null`()
      }

      // Build class annotations
      val classAnnotations = irClass.annotations.map { annotation ->
        pika.annotationInfo(annotation)
      }

      val fullTypeInfoClass = symbolFinder.pikaAPI.fullTypedInfo
      val fullFieldInfoClass = symbolFinder.pikaAPI.fullFieldInfo
      val annotationInfoClass = symbolFinder.pikaAPI.annotationInfo

      val constructor = fullTypeInfoClass.owner.primaryConstructor
        ?: error("FullTypeInfo must have a primary constructor")

      val kClassStarType = irBuiltIns.kClassClass.typeWith(irBuiltIns.anyNType)

      return IrConstructorCallImpl(
        startOffset = -1,
        endOffset = -1,
        type = fullTypeInfoClass.owner.defaultType,
        symbol = constructor.symbol,
        typeArgumentsCount = 0,
        constructorTypeArgumentsCount = 0,
        origin = null
      ).also { call ->
        call.arguments[0] = kotlin.string(className)
        call.arguments[1] = kotlin.kClass(classifier as IrClassSymbol)
        call.arguments[2] = kotlin.listOf(fullFieldInfoClass.owner.defaultType, fieldInfoList)
        call.arguments[3] = baseClassInfo
        call.arguments[4] = kotlin.listOf(kClassStarType, interfaceKClasses)
        call.arguments[5] = kotlin.listOf(annotationInfoClass.owner.defaultType, classAnnotations)
        call.arguments[6] = kotlin.bool(isNullable)
      }
    }
  }
}

