package io.github.lukmccall.pika

import kotlin.reflect.KClass

/**
 * Main introspection data for a class.
 *
 * @property kClass Runtime class reference
 * @property annotations List of annotations applied to the class
 * @property properties List of property descriptors for properties declared directly on this class
 * @property functions List of function descriptors for functions declared directly on this class
 * @property baseClass Reference to parent's introspection data, or null if no introspectable parent
 */
public class PIntrospectionData<OwnerType : Any>(
  public val kClass: KClass<OwnerType>,
  public val annotations: List<PAnnotation>,
  public val properties: List<PProperty<OwnerType, *>>,
  public val functions: List<PFunction>,
  public val baseClass: PIntrospectionData<*>?
)
