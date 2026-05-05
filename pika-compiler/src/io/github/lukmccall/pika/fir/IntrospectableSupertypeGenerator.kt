package io.github.lukmccall.pika.fir

import io.github.lukmccall.pika.symbols.PikaAPI
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.constructClassLikeType

class IntrospectableSupertypeGenerator(
  session: FirSession
) : FirSupertypeGenerationExtension(session) {
  private val disallowedClassKinds = setOf(
    ClassKind.INTERFACE,
    ClassKind.ANNOTATION_CLASS,
    ClassKind.ENUM_CLASS
  )

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(session.introspectablePredicateMatcher.predicate)
  }

  override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
    val classSymbol = declaration.symbol as? FirClassSymbol<*> ?: return false
    if (disallowedClassKinds.contains(classSymbol.classKind)) {
      return false
    }
    return classSymbol.hasIntrospectableAnnotation(session)
  }

  override fun computeAdditionalSupertypes(
    classLikeDeclaration: FirClassLikeDeclaration,
    resolvedSupertypes: List<FirResolvedTypeRef>,
    typeResolver: TypeResolveService
  ): List<ConeKotlinType> {
    if (resolvedSupertypes.any { it.coneType.classId == PikaAPI.PIntrospectionProvider }) {
      return emptyList()
    }
    return listOf(
      PikaAPI.PIntrospectionProvider.constructClassLikeType(emptyArray(), isMarkedNullable = false)
    )
  }
}
