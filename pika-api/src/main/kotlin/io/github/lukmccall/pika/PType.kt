package io.github.lukmccall.pika

import kotlin.reflect.KClass

/**
 * Represents a runtime type with its Java Class reference.
 *
 * @property jClass Runtime Java class reference. Call `jClass.kotlin` if you need the Kotlin KClass.
 */
public class PType(public val jClass: Class<*>) {
  public val kClass: KClass<*>
    get() = jClass.kotlin
}
