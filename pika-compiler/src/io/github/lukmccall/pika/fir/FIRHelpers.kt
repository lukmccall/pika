package io.github.lukmccall.pika.fir

import io.github.lukmccall.pika.symbols.PikaAPI
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.classId

fun FirClassSymbol<*>.implementsIntrospectable(session: FirSession): Boolean {
  val superTypes = resolvedSuperTypes
  for (superType in superTypes) {
    val superTypeClassId = superType.classId ?: continue
    if (superTypeClassId == PikaAPI.Introspectable) {
      return true
    }

    val superClassSymbol = session
      .symbolProvider
      .getClassLikeSymbolByClassId(superTypeClassId) as? FirClassSymbol<*> ?: continue

    if (superClassSymbol.implementsIntrospectable(session)) {
      return true
    }
  }

  return false
}


