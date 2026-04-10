package io.github.lukmccall.pika

/**
 * Returns introspection data for the given @Introspectable instance.
 * The compiler plugin replaces calls to this function at compile time
 * with a call to `instance.__PIntrospectionData()`.
 *
 * Example:
 * ```kotlin
 * @Introspectable
 * class Person(val name: String)
 *
 * val person = Person("Alice")
 * val data = introspectionOf(person)
 * val name = data.properties[0].getter(person) // "Alice"
 * ```
 */
public fun <T : Any> introspectionOf(instance: T): PIntrospectionData<T> =
  throw NotImplementedError("introspectionOf(instance) should be replaced by the compiler plugin")