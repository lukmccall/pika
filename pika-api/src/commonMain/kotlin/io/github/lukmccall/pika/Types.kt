package io.github.lukmccall.pika

import kotlin.reflect.KClass

/**
 * Visibility levels for class members.
 */
public enum class Visibility { PUBLIC, PRIVATE, PROTECTED, INTERNAL }

/**
 * Information about an annotation applied to a class or property.
 *
 * @property className Fully qualified class name of the annotation
 * @property kClass Runtime class reference for the annotation
 * @property arguments Map of argument names to values (primitives, strings, enums, KClass only)
 */
public data class AnnotationInfo(
  val className: String,
  val kClass: KClass<*>,
  val arguments: Map<String, Any?>
)

/**
 * Full information about a class field/property.
 *
 * @property name The property name
 * @property pTypeDescriptor Type descriptor for the property
 * @property annotations List of annotations applied to this property
 * @property visibility The visibility level of this property
 * @property isMutable Whether this is a var (true) or val (false)
 */
public data class FullFieldInfo(
  val name: String,
  val pTypeDescriptor: PTypeDescriptor,
  val annotations: List<AnnotationInfo>,
  val visibility: Visibility,
  val isMutable: Boolean
)

/**
 * Complete type information for a class.
 *
 * @property className Fully qualified class name
 * @property kClass Runtime class reference
 * @property fields List of field information for properties declared directly on this class
 * @property baseClass FullTypeInfo for the superclass, or null if none (extends Any directly)
 * @property interfaces List of KClass references for implemented interfaces
 * @property classAnnotations List of annotations applied to the class
 * @property isNullable Whether the type is nullable
 */
public data class FullTypeInfo(
  val className: String,
  val kClass: KClass<*>,
  val fields: List<FullFieldInfo>,
  val baseClass: FullTypeInfo?,
  val interfaces: List<KClass<*>>,
  val classAnnotations: List<AnnotationInfo>,
  val isNullable: Boolean
)

/**
 * Represents a runtime type with its KClass reference.
 *
 * @property kClass Runtime class reference
 */
public class PType(public val kClass: KClass<*>)

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
