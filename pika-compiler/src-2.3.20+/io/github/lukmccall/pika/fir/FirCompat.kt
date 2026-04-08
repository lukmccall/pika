package io.github.lukmccall.pika.fir

import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol as resolveGetContainingClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

/**
 * Compatibility function for getContainingClassSymbol.
 * In Kotlin 2.3.x, this is in org.jetbrains.kotlin.fir.resolve package.
 */
fun FirBasedSymbol<*>.getContainingClassSymbol(): FirClassLikeSymbol<*>? {
    return resolveGetContainingClassSymbol()
}
