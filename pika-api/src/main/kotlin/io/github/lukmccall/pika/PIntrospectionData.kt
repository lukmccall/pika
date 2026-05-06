package io.github.lukmccall.pika

/**
 * Main introspection data for a class.
 *
 * @property jClass Runtime Java class reference. Call `jClass.kotlin` if you need the Kotlin KClass.
 * @property annotations Array of annotations applied to the class
 * @property properties Array of property descriptors for properties declared directly on this class
 * @property functions Array of function descriptors for functions declared directly on this class
 * @property baseClass Reference to parent's introspection data, or null if no introspectable parent
 */
public class PIntrospectionData<OwnerType : Any>(
  public val jClass: Class<OwnerType>,
  public val annotations: Array<PAnnotation>,
  public val properties: Array<PProperty<OwnerType, *>>,
  public val functions: Array<PFunction>,
  public val baseClass: PIntrospectionData<*>?
)
