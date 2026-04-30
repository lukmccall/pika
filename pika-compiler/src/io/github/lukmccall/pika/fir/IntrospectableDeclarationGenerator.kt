package io.github.lukmccall.pika.fir

import io.github.lukmccall.pika.Identifiers
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.addToStdlib.enumSetOf

/**
 * FIR extension that generates __Pika objects for classes annotated with @Introspectable.
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
    register(session.introspectablePredicateMatcher.predicate)
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

    return setOf(Name.identifier(Identifiers.PIKA_NESTED_OBJECT_NAME))
  }

  override fun generateNestedClassLikeDeclaration(
    owner: FirClassSymbol<*>,
    name: Name,
    context: NestedClassGenerationContext
  ): FirClassLikeSymbol<*>? {
    if (name.asString() != Identifiers.PIKA_NESTED_OBJECT_NAME || !owner.hasIntrospectableAnnotation(session)) {
      return null
    }

    return createNestedClass(
      owner,
      name,
      IntrospectableDeclarationGeneratorKey,
      classKind = ClassKind.OBJECT
    ).symbol
  }

  override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
    val owner = context.owner
    if (!owner.wasGeneratedByPika()) {
      return emptyList()
    }

    return listOf(createDefaultPrivateConstructor(owner, IntrospectableDeclarationGeneratorKey).symbol)
  }

  @OptIn(SymbolInternals::class)
  override fun getCallableNamesForClass(
    classSymbol: FirClassSymbol<*>,
    context: MemberGenerationContext
  ): Set<Name> {
    if (classSymbol.classKind == ClassKind.OBJECT && classSymbol.wasGeneratedByPika()) {
      return getCallableNamesForPikaObject(classSymbol)
    }

    return emptySet()
  }

  private fun getCallableNamesForPikaObject(classSymbol: FirClassSymbol<*>): Set<Name> {
    val containingClass = classSymbol.getContainingClassSymbol() as? FirClassSymbol<*>
      ?: return emptySet()

    if (!containingClass.hasIntrospectableAnnotation(session)) {
      return emptySet()
    }

    return setOf(SpecialNames.INIT)
  }

  private fun FirClassSymbol<*>.wasGeneratedByPika(): Boolean {
    val origin = origin as? FirDeclarationOrigin.Plugin
    return origin?.key == IntrospectableDeclarationGeneratorKey
  }
}

object IntrospectableDeclarationGeneratorKey : org.jetbrains.kotlin.GeneratedDeclarationKey()
