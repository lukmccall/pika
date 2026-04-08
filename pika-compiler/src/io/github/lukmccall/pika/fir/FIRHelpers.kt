package io.github.lukmccall.pika.fir

import io.github.lukmccall.pika.symbols.PikaAPI
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

/**
 * Predicate that matches classes with @Introspectable annotation.
 * This must be registered with the FirDeclarationPredicateRegistrar.
 */
val introspectablePredicate: DeclarationPredicate = DeclarationPredicate.create {
  annotated(PikaAPI.Introspectable.asSingleFqName())
}

/**
 * Checks if this class has the @Introspectable annotation directly.
 * Note: Each class in an inheritance chain must be explicitly annotated.
 */
fun FirClassSymbol<*>.hasIntrospectableAnnotation(session: FirSession): Boolean {
  return session.predicateBasedProvider.matches(introspectablePredicate, this)
}

@OptIn(SymbolInternals::class)
fun FirClassSymbol<*>.hasCompanionObject(): Boolean {
  return (this as? FirRegularClassSymbol)?.companionObjectSymbol != null
}

fun Name.isCompanionObjectName(): Boolean = this == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
