package io.github.lukmccall.pika

/**
 * Returns introspection data for the given Introspectable instance.
 * The compiler plugin replaces calls to this function at compile time
 * with a call to `instance.__PIntrospectionData()`.
 *
 * Example:
 * ```kotlin
 * class Person(val name: String) : Introspectable
 *
 * val person = Person("Alice")
 * val data = pIntrospectionOf(person)
 * val name = data.properties[0].getter(person) // "Alice"
 * ```
 */
public fun <T : Introspectable> pIntrospectionOf(instance: T): PIntrospectionData<T> =
  throw NotImplementedError("pIntrospectionOf(instance) should be replaced by the compiler plugin")