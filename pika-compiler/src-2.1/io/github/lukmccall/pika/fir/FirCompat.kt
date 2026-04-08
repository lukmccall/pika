package io.github.lukmccall.pika.fir

import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol as checkersGetContainingClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

/**
 * Compatibility function for getContainingClassSymbol.
 * In Kotlin 2.1.x and 2.2.x, this is in org.jetbrains.kotlin.fir.analysis.checkers package.
 */
fun FirBasedSymbol<*>.getContainingClassSymbol(): FirClassLikeSymbol<*>? {
    return checkersGetContainingClassSymbol()
}
