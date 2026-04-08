package io.github.lukmccall.pika

/**
 * Returns a type descriptor for the specified type parameter T.
 * The compiler plugin replaces calls to this function at compile time.
 */
public fun <T> pTypeDescriptorOf(): PTypeDescriptor =
  throw NotImplementedError("pTypeDescriptorOf<T>() should be replaced by the compiler plugin")

/**
 * Internal function called when pTypeDescriptorOf<T>() is used with a non-reified type parameter.
 * This provides a clear error message at runtime.
 * Returns PTypeDescriptor for bytecode verifier compatibility (but always throws).
 */
@PublishedApi
internal fun throwNonReifiedPTypeDescriptorError(): PTypeDescriptor =
  throw IllegalStateException(
    "pTypeDescriptorOf<T>() requires a reified type parameter. " +
      "Use 'inline fun <reified T>' or call pTypeDescriptorOf<T>() with a concrete type."
  )

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
