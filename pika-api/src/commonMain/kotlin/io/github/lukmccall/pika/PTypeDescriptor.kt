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
   */
  public open class Concrete(
    public val pType: PType,
    public val isNullable: Boolean
  ) : PTypeDescriptor {
    /**
     * Represents a parameterized (generic) type.
     *
     * @property pType The runtime type reference for the raw type
     * @property isNullable Whether the type is nullable
     * @property argumentsPTypes List of type argument descriptors
     */
    public class Parameterized(
      pType: PType,
      isNullable: Boolean,
      public val argumentsPTypes: List<PTypeDescriptor>
    ) : Concrete(pType, isNullable)
  }

  /**
   * Represents a star projection (*) in generic types.
   */
  public data object Star : PTypeDescriptor
}
