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
   * @property introspectionData Introspection data for this type, or null if the type is not introspectable
   */
  public open class Concrete(
    public val pType: PType,
    public val isNullable: Boolean,
    public val introspectionData: PIntrospectionData<*>? = null
  ) : PTypeDescriptor {
    /**
     * Represents a parameterized (generic) type.
     *
     * @property pType The runtime type reference for the raw type
     * @property isNullable Whether the type is nullable
     * @property argumentsPTypes List of type argument descriptors
     * @property introspectionData Introspection data for this type, or null if the type is not introspectable
     */
    public class Parameterized(
      pType: PType,
      isNullable: Boolean,
      public val argumentsPTypes: List<PTypeDescriptor>,
      introspectionData: PIntrospectionData<*>? = null
    ) : Concrete(pType, isNullable, introspectionData)
  }

  /**
   * Represents a star projection (*) in generic types.
   */
  public data object Star : PTypeDescriptor
}
