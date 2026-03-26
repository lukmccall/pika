package io.github.lukmccall.pika

/**
 * Returns type information for the specified type parameter T.
 * The compiler plugin replaces calls to this function at compile time.
 */
@Suppress("UNUSED_PARAMETER", "unused")
public fun <T> typeInfo(): TypeInfo =
  throw NotImplementedError("typeInfo<T>() should be replaced by the compiler plugin")

/**
 * Returns full type information for the specified type parameter T.
 * The compiler plugin replaces calls to this function at compile time.
 *
 * This function provides comprehensive metadata including:
 * - All declared properties with their types, annotations, and visibility
 * - Class annotations
 * - Inheritance information
 */
@Suppress("UNUSED_PARAMETER", "unused")
public fun <T> fullTypeInfo(): FullTypeInfo =
  throw NotImplementedError("fullTypeInfo<T>() should be replaced by the compiler plugin")
