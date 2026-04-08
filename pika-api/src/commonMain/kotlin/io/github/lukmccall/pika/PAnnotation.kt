package io.github.lukmccall.pika

import kotlin.reflect.KClass

/**
 * Information about an annotation applied to a class or property (for introspection).
 *
 * @property kClass Runtime class reference for the annotation
 * @property arguments Map of argument names to values (primitives, strings, enums, KClass only)
 */
public class PAnnotation(
  public val kClass: KClass<*>,
  public val arguments: Map<String, Any?>
)
