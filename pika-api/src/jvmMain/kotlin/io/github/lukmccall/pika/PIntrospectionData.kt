package io.github.lukmccall.pika

/**
 * Main introspection data for a class.
 *
 * @property jClass Runtime Java class reference. Call `jClass.kotlin` if you need the Kotlin KClass.
 * @property annotations List of annotations applied to the class
 * @property properties List of property descriptors for properties declared directly on this class
 * @property functions List of function descriptors for functions declared directly on this class
 * @property baseClass Reference to parent's introspection data, or null if no introspectable parent
 */
public class PIntrospectionData<OwnerType : Any>(
  public val jClass: Class<OwnerType>,
  public val annotations: List<PAnnotation>,
  public val properties: List<PProperty<OwnerType, *>>,
  public val functions: List<PFunction>,
  public val baseClass: PIntrospectionData<*>?
)
