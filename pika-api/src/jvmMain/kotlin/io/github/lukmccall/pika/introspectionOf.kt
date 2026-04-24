package io.github.lukmccall.pika

/**
 * Returns introspection data for the @Introspectable type T.
 * The compiler plugin replaces calls to this function at compile time
 * with a direct field read of the cached introspection data.
 *
 * Example:
 * ```kotlin
 * @Introspectable
 * class Person(val name: String)
 *
 * val data = introspectionOf<Person>()
 * val person = Person("Alice")
 * val name = data.properties[0].getter(person) // "Alice"
 * ```
 */
public fun <T> introspectionOf(): PIntrospectionData<T & Any> =
  throw NotImplementedError("introspectionOf<T>() should be replaced by the compiler plugin")

/**
 * Called at runtime when introspectionOf is used inside an inline function whose
 * type parameter is not reified. This always throws — the compiler plugin must replace
 * introspectionOf at the inlined call site.
 */
@PublishedApi
internal fun throwNonReifiedIntrospectionOfError(): PIntrospectionData<Any> =
  throw UnsupportedOperationException(
    "introspectionOf<T>() requires a reified type parameter. " +
      "Use 'inline fun <reified T>' or call introspectionOf<T>() with a concrete type."
  )
