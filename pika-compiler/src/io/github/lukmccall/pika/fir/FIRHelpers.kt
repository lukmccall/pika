package io.github.lukmccall.pika.fir

import io.github.lukmccall.pika.symbols.PikaAPI
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.FqName

/**
 * Session component holding the predicate that matches @Introspectable-equivalent
 * annotations. The built-in @Introspectable is always included; additional
 * marker annotation FqNames come from the `-P plugin:<id>:introspectableAnnotation=<fqn>` CLI
 * option (see [io.github.lukmccall.pika.PikaConfigurationKeys.INTROSPECTABLE_ANNOTATION]).
 */
class FirIntrospectablePredicateMatcher(
  session: FirSession,
  extraAnnotationFqNames: List<String>,
) : FirExtensionSessionComponent(session) {
  val predicate = DeclarationPredicate.create {
    val fqNames = buildList {
      add(PikaAPI.Introspectable.asSingleFqName())
      extraAnnotationFqNames.mapTo(this, ::FqName)
    }
    annotated(fqNames)
  }

  companion object {
    fun getFactory(extraAnnotationFqNames: List<String>): Factory {
      return Factory { session -> FirIntrospectablePredicateMatcher(session, extraAnnotationFqNames) }
    }
  }
}

val FirSession.introspectablePredicateMatcher: FirIntrospectablePredicateMatcher by
  FirSession.sessionComponentAccessor<FirIntrospectablePredicateMatcher>()

/**
 * Checks if this class has @Introspectable or a user-registered equivalent.
 * Note: Each class in an inheritance chain must be explicitly annotated.
 */
fun FirClassSymbol<*>.hasIntrospectableAnnotation(session: FirSession): Boolean {
  return session.predicateBasedProvider.matches(session.introspectablePredicateMatcher.predicate, this)
}
