package io.github.lukmccall.pika

/**
 * Sealed interface hierarchy representing type descriptors for properties.
 */
public sealed interface PTypeDescriptor {
  /**
   * Represents a concrete (non-star) type.
   *
   * @property pType The runtime type reference
   * @property isNullable Whether the type is nullable
   * @property introspection Introspection data for this type, or null if the type is not introspectable
   */
  public open class Concrete(
    public val pType: PType,
    public val isNullable: Boolean,
    public val introspection: PIntrospectionData<*>? = null
  ) : PTypeDescriptor {
    /**
     * Represents a parameterized (generic) type.
     *
     * @property pType The runtime type reference for the raw type
     * @property isNullable Whether the type is nullable
     * @property parameters List of type argument descriptors
     * @property introspection Introspection data for this type, or null if the type is not introspectable
     */
    public class Parameterized(
      pType: PType,
      isNullable: Boolean,
      public val parameters: List<PTypeDescriptor>,
      introspection: PIntrospectionData<*>? = null
    ) : Concrete(pType, isNullable, introspection)
  }

  /**
   * Represents a star projection (*) in generic types.
   */
  public data object Star : PTypeDescriptor
}
