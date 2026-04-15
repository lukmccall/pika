package io.github.lukmccall.pika

/**
 * Represents a runtime type with its Java Class reference.
 *
 * @property jClass Runtime Java class reference. Call `jClass.kotlin` if you need the Kotlin KClass.
 */
public class PType(public val jClass: Class<*>)
