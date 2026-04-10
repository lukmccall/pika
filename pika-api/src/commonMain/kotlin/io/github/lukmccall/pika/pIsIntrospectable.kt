package io.github.lukmccall.pika

/**
 * Returns true if the type T is @Introspectable,
 * meaning pIntrospectionOf() will return data for instances of T.
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
 * pIsIntrospectable<Person>()          // true
 * pIsIntrospectable<NotIntrospectable>()  // false
 * ```
 */
public fun <T : Any> pIsIntrospectable(): Boolean =
  throw NotImplementedError("pIsIntrospectable<T>() should be replaced by the compiler plugin")

/**
 * Called at runtime when pIsIntrospectable is used inside an inline function whose
 * type parameter is not reified. This always throws — the compiler plugin must replace
 * pIsIntrospectable at the inlined call site.
 */
@PublishedApi
internal fun throwNonReifiedPIsIntrospectableError(): Boolean =
  throw UnsupportedOperationException(
    "pIsIntrospectable<T>() requires a reified type parameter. " +
      "Use 'inline fun <reified T>' or call pIsIntrospectable<T>() with a concrete type."
  )
