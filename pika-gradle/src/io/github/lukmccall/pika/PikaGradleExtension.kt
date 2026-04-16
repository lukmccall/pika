package io.github.lukmccall.pika

import org.gradle.api.model.ObjectFactory

open class PikaGradleExtension(objectFactory: ObjectFactory) {
  internal val introspectableAnnotations: MutableList<String> = mutableListOf()

  /** Registers an additional annotation class FqName to be treated as @Introspectable. */
  open fun introspectableAnnotation(fqName: String) {
    introspectableAnnotations.add(fqName)
  }

  /** Registers additional annotation class FqNames to be treated as @Introspectable. */
  open fun introspectableAnnotations(vararg fqNames: String) {
    introspectableAnnotations.addAll(fqNames)
  }

  /** Registers additional annotation class FqNames to be treated as @Introspectable. */
  open fun introspectableAnnotations(fqNames: Iterable<String>) {
    introspectableAnnotations.addAll(fqNames)
  }
}
