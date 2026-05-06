package io.github.lukmccall.pika

/**
 * Information about an annotation applied to a class or property (for introspection).
 *
 * @property jClass Runtime Java class reference for the annotation. Call `jClass.kotlin` for the KClass.
 * @property arguments Map of argument names to values (primitives, strings, enums, Class only)
 */
public class PAnnotation(
  public val jClass: Class<*>,
  public val arguments: Map<String, Any?>
)
