package io.github.lukmccall.pika

/**
 * Descriptor for a property with getter/setter lambdas for introspection.
 *
 * @property name The property name
 * @property visibility The visibility level of this property
 * @property annotations List of annotations applied to this property
 * @property type Type descriptor for the property
 * @property getter Lambda to get the property value from an owner instance
 * @property isMutable Whether this is a var (true) or val (false)
 * @property hasBackingField Whether this property has a backing field
 * @property setter Lambda to set the property value on an owner instance, or null if no backing field
 * @property isDelegated Whether this property is delegated (e.g., `val x by lazy { ... }`)
 * @property delegateGetter Lambda to get the delegate object from an owner instance, or null if not delegated
 */
public class PProperty<OwnerType, Type>(
  public val name: String,
  public val visibility: PVisibility,
  public val annotations: List<PAnnotation>,
  public val type: PTypeDescriptor,
  public val getter: (OwnerType) -> Type,
  public val isMutable: Boolean,
  public val hasBackingField: Boolean,
  public val setter: ((OwnerType, Type) -> Unit)?,
  public val isDelegated: Boolean,
  public val delegateGetter: ((OwnerType) -> Any?)?
)
