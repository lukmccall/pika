package io.github.lukmccall.pika

/**
 * Returns true if the type T is @Introspectable,
 * meaning introspectionOf() will return data for instances of T.
 * The compiler plugin replaces calls to this function at compile time
 * with a constant true or false.
 *
 * Example:
 * ```kotlin
 * @Introspectable
 * class Person(val name: String)
 *
 * class NotIntrospectable
 *
 * isIntrospectable<Person>()          // true
 * isIntrospectable<NotIntrospectable>()  // false
 * ```
 */
public fun <T> isIntrospectable(): Boolean =
  throw NotImplementedError("isIntrospectable<T>() should be replaced by the compiler plugin")

/**
 * Called at runtime when isIntrospectable is used inside an inline function whose
 * type parameter is not reified. This always throws — the compiler plugin must replace
 * isIntrospectable at the inlined call site.
 */
@PublishedApi
internal fun throwNonReifiedIsIntrospectableError(): Boolean =
  throw UnsupportedOperationException(
    "isIntrospectable<T>() requires a reified type parameter. " +
      "Use 'inline fun <reified T>' or call isIntrospectable<T>() with a concrete type."
  )
