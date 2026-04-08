package io.github.lukmccall.pika

import kotlin.reflect.KClass

/**
 * Represents a runtime type with its KClass reference.
 *
 * @property kClass Runtime class reference
 */
public class PType(public val kClass: KClass<*>)
