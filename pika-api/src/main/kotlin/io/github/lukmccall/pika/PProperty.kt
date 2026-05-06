package io.github.lukmccall.pika

/**
 * Descriptor for a property with index-based access through a shared [PPropertyAccessor].
 *
 * @property name The property name
 * @property visibility The visibility level of this property
 * @property annotations List of annotations applied to this property
 * @property type Type descriptor for the property
 * @property index Index used for dispatch in the shared accessor
 * @property accessor Shared accessor that handles get/set for all properties via index dispatch
 * @property isMutable Whether this is a var (true) or val (false)
 * @property hasBackingField Whether this property has a backing field
 * @property isDelegated Whether this property is delegated (e.g., `val x by lazy { ... }`)
 */
public class PProperty<OwnerType, Type> @PublishedApi internal constructor(
  public val name: String,
  public val visibility: PVisibility,
  public val annotations: Array<PAnnotation>,
  public val type: PTypeDescriptor,
  public val index: Int,
  private val accessor: PPropertyAccessor,
  public val isMutable: Boolean,
  public val hasBackingField: Boolean,
  public val isDelegated: Boolean
) {
  @Suppress("UNCHECKED_CAST")
  public fun get(instance: OwnerType): Type = accessor.`__pika$PropertyGet`(instance, index) as Type

  public fun set(instance: OwnerType, value: Type) {
    accessor.`__pika$PropertySet`(instance, index, value)
  }
}

@Suppress("UNCHECKED_CAST")
public inline fun <OwnerType, reified T> PProperty<OwnerType, *>.cast(propertyType: Class<T> = T::class.java): PProperty<OwnerType, T> = this as PProperty<OwnerType, T>
