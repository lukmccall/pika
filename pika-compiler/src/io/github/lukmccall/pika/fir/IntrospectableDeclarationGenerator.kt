package io.github.lukmccall.pika.fir

import io.github.lukmccall.pika.Identifiers
import io.github.lukmccall.pika.symbols.PikaAPI
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.addToStdlib.enumSetOf

/**
 * FIR extension that generates `__PIntrospectionData()` function declarations
 * in companion objects for classes annotated with @Introspectable.
 *
 * This makes the function visible during FIR resolution so calls to it don't fail
 * with "unresolved reference" errors. The actual implementation is generated
 * during the IR phase by IntrospectableTransformer.
 */
class IntrospectableDeclarationGenerator(
  session: FirSession
) : FirDeclarationGenerationExtension(session) {
  private val unallowedClassKinds = enumSetOf(
    ClassKind.OBJECT,
    ClassKind.ANNOTATION_CLASS,
    ClassKind.ENUM_CLASS,
    ClassKind.INTERFACE
  )

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(introspectablePredicate)
  }

  override fun getNestedClassifiersNames(
    classSymbol: FirClassSymbol<*>,
    context: NestedClassGenerationContext
  ): Set<Name> {
    if (!classSymbol.hasIntrospectableAnnotation(session)) {
      return emptySet()
    }

    if (unallowedClassKinds.contains(classSymbol.classKind)) {
      return emptySet()
    }

    if (classSymbol.hasCompanionObject()) {
      return emptySet()
    }

    return setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
  }

  override fun generateNestedClassLikeDeclaration(
    owner: FirClassSymbol<*>,
    name: Name,
    context: NestedClassGenerationContext
  ): FirClassLikeSymbol<*>? {
    if (!name.isCompanionObjectName() || !owner.hasIntrospectableAnnotation(session)) {
      return null
    }
    return createCompanionObject(owner, IntrospectableDeclarationGeneratorKey).symbol
  }

  override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
    val owner = context.owner
    if (!owner.wasGeneratedByPika()) {
      return emptyList()
    }

    return listOf(createDefaultPrivateConstructor(owner, IntrospectableDeclarationGeneratorKey).symbol)
  }

  /**
   * Declare callable names for companion objects, object declarations, and main classes.
   * - For regular classes: generate synthetic accessors for backing fields
   * - For companion objects: generate __PIntrospectionData function
   * - For object declarations: generate __PIntrospectionData directly on the object
   *
   * Note: We use declarationSymbols here instead of declaredProperties(session) because
   * the latter triggers scope resolution, which would cause infinite recursion
   * (scope resolution calls getCallableNamesForClass to determine what callables exist).
   */
  @OptIn(SymbolInternals::class)
  override fun getCallableNamesForClass(
    classSymbol: FirClassSymbol<*>,
    context: MemberGenerationContext
  ): Set<Name> {
    if (classSymbol.isCompanion) {
      return getCallableNamesForCompanionObject(classSymbol)
    }

    if (classSymbol.classKind == ClassKind.OBJECT) {
      return getCallableNamesForObject(classSymbol)
    }

    return getCallableNamesForClass(classSymbol)
  }

  private fun getCallableNamesForObject(classSymbol: FirClassSymbol<*>): Set<Name> {
    if (!classSymbol.hasIntrospectableAnnotation(session)) {
      return emptySet()
    }
    return setOf(PikaAPI.Names.IntrospectionDataFunction)
  }

  private fun getCallableNamesForCompanionObject(classSymbol: FirClassSymbol<*>): Set<Name> {
    val containingClass = classSymbol.getContainingClassSymbol() as? FirClassSymbol<*>
      ?: return emptySet()

    if (!containingClass.hasIntrospectableAnnotation(session)) {
      return emptySet()
    }

    return buildSet {
      add(PikaAPI.Names.IntrospectionDataFunction)
      if (classSymbol.wasGeneratedByPika()) {
        add(SpecialNames.INIT)
      }
    }
  }

  private fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>): Set<Name> {
    if (!classSymbol.hasIntrospectableAnnotation(session)) {
      return emptySet()
    }

    val properties = classSymbol
      .declarationSymbols
      .filterIsInstance<FirPropertySymbol>()
      .filter { it.hasBackingField }

    return buildSet {
      for (property in properties) {
        val propertyName = property.name.asString()
        add(Name.identifier(Identifiers.syntheticGetterName(propertyName)))
        add(Name.identifier(Identifiers.syntheticSetterName(propertyName)))
      }
    }
  }

  override fun generateFunctions(
    callableId: CallableId,
    context: MemberGenerationContext?
  ): List<FirNamedFunctionSymbol> {
    val owner = context?.owner ?: return emptyList()
    val callableName = callableId.callableName.asString()

    if (callableId.callableName == PikaAPI.Names.IntrospectionDataFunction) {
      return generateIntrospectionDataFunction(owner)
    }

    if (Identifiers.isSyntheticAccessor(callableName)) {
      return generateSyntheticAccessor(owner, callableName)
    }

    return emptyList()
  }

  private fun generateIntrospectionDataFunction(owner: FirClassSymbol<*>): List<FirNamedFunctionSymbol> {
    if (owner.isCompanion) {
      val containingClass = owner.getContainingClassSymbol() as? FirClassSymbol<*>
        ?: return emptyList()

      return generateIntrospectionDataFunctionForClass(owner, forClass = containingClass)
    }

    if (owner.classKind == ClassKind.OBJECT) {
      return generateIntrospectionDataFunctionForClass(owner, forClass = owner)
    }

    return emptyList()
  }

  private fun generateIntrospectionDataFunctionForClass(
    owner: FirClassSymbol<*>,
    forClass: FirClassSymbol<*>
  ): List<FirNamedFunctionSymbol> {
    val ownerType = forClass.defaultType()
    val returnType = PikaAPI.PIntrospectionData.constructClassLikeType(
      arrayOf(ownerType)
    )

    val function = createMemberFunction(
      owner,
      key = IntrospectableDeclarationGeneratorKey,
      name = PikaAPI.Names.IntrospectionDataFunction,
      returnType = returnType
    )

    return listOf(function.symbol)
  }

  private fun generateSyntheticAccessor(
    owner: FirClassSymbol<*>,
    callableName: String
  ): List<FirNamedFunctionSymbol> {
    // Find the property this accessor is for
    val propertySymbol = owner
      .declarationSymbols
      .filterIsInstance<FirPropertySymbol>()
      .filter { it.hasBackingField }
      .find { prop ->
        Identifiers.syntheticGetterName(prop.name.asString()) == callableName ||
          Identifiers.syntheticSetterName(prop.name.asString()) == callableName
      } ?: return emptyList()

    val propertyType = propertySymbol.resolvedReturnType
    val isGetter = callableName == Identifiers.syntheticGetterName(propertySymbol.name.asString())
    val accessorName = Name.identifier(callableName)

    return listOf(
      if (isGetter) {
        // Synthetic getter: fun __pika$get$X(): PropertyType
        generateSyntheticGetter(owner, accessorName, propertyType)
      } else {
        // Synthetic setter: fun __pika$set$X(value: PropertyType)
        generateSyntheticSetter(owner, accessorName, propertyType)
      }
    )
  }

  private fun generateSyntheticGetter(
    owner: FirClassSymbol<*>,
    name: Name,
    type: ConeKotlinType
  ) = createMemberFunction(
    owner,
    key = IntrospectableDeclarationGeneratorKey,
    name = name,
    returnType = type
  ) {
    visibility = Visibilities.Internal
    modality = Modality.FINAL
  }.symbol

  private fun generateSyntheticSetter(
    owner: FirClassSymbol<*>,
    name: Name,
    type: ConeKotlinType
  ) = createMemberFunction(
    owner,
    key = IntrospectableDeclarationGeneratorKey,
    name = name,
    returnType = session.builtinTypes.unitType.coneType
  ) {
    visibility = Visibilities.Internal
    modality = Modality.FINAL
    valueParameter(Name.identifier("value"), type)
  }.symbol


  private fun FirClassSymbol<*>.wasGeneratedByPika(): Boolean {
    val origin = origin as? FirDeclarationOrigin.Plugin
    return origin?.key == IntrospectableDeclarationGeneratorKey
  }
}

object IntrospectableDeclarationGeneratorKey : org.jetbrains.kotlin.GeneratedDeclarationKey()
