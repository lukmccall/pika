package sample

import io.github.lukmccall.pika.*
import java.io.Serializable

/**
 * Example annotation for testing.
 */
annotation class MyAnnotation(val value: String)

/**
 * Example base class.
 */
open class BaseEntity(open val id: String)

/**
 * Example nested introspectable class.
 */
@Introspectable
class Address(val city: String, val country: String)

/**
 * Simple test class for Introspectable.
 */
@Introspectable
open class SimplePerson(val name: String, val address: Address)

/**
 * Test class with delegated properties.
 */
@Introspectable
class DelegatedExample {
  val lazyValue by lazy { "computed lazily" }
  val regularValue: String = "regular"
  val computedValue: Int get() = 42
}

/**
 * Example class with various field types, annotations, and inheritance.
 */
@MyAnnotation("user-class")
class User(
  override val id: String,
  @Deprecated("use fullName instead")
  val name: String,
  private var age: Int,
  val tags: List<String>
) : BaseEntity(id), Serializable

fun main() {
  println("=== Introspectable Test ===")
  println()
  val person = SimplePerson("Alice", Address("Paris", "France"))

  // Two equivalent ways to get introspection data:
  // 1. Using the helper function (recommended)
  val data = introspectionOf<SimplePerson>()
  // 2. Or directly: SimplePerson.Companion.__PIntrospectionData()

  println("kClass: ${data.kClass}")
  println("properties: ${data.properties.size}")

  for (prop in data.properties) {
    println("  - ${prop.name}: getter(person)=${prop.getter(person)}")
  }
  println()

  println("=== typeDescriptorOf<T>() Examples (basic type info) ===")
  println()

  // Simple types
  println("Simple types:")
  println("  typeDescriptorOf<String>(): ${formatPTypeDescriptor(typeDescriptorOf<String>())}")
  println("  typeDescriptorOf<Int>(): ${formatPTypeDescriptor(typeDescriptorOf<Int>())}")
  println("  typeDescriptorOf<Boolean>(): ${formatPTypeDescriptor(typeDescriptorOf<Boolean>())}")
  println()

  // Nullable types
  println("Nullable types:")
  println("  typeDescriptorOf<Int?>(): ${formatPTypeDescriptor(typeDescriptorOf<Int?>())}")
  println("  typeDescriptorOf<String?>(): ${formatPTypeDescriptor(typeDescriptorOf<String?>())}")
  println()

  // Parameterized types
  println("Parameterized types:")
  println("  typeDescriptorOf<List<String>>(): ${formatPTypeDescriptor(typeDescriptorOf<List<String>>())}")
  println("  typeDescriptorOf<Map<String, Int>>(): ${formatPTypeDescriptor(typeDescriptorOf<Map<String, Int>>())}")
  println()

  // Nested generics
  println("Nested generics:")
  println("  typeDescriptorOf<Map<String, List<Int?>>>(): ${formatPTypeDescriptor(typeDescriptorOf<Map<String, List<Int?>>>())}")
  println()

  // Star projections
  println("Star projections:")
  println("  typeDescriptorOf<List<*>>(): ${formatPTypeDescriptor(typeDescriptorOf<List<*>>())}")
  println()

  // Class types
  println("Class types:")
  println("  typeDescriptorOf<User>(): ${formatPTypeDescriptor(typeDescriptorOf<User>())}")
  println("  typeDescriptorOf<User?>(): ${formatPTypeDescriptor(typeDescriptorOf<User?>())}")
  println()

  // Inline proxy function test
  println("Inline proxy function (typeDescriptorOf through inline function):")
  println("  proxy<String>(): ${formatPTypeDescriptor(proxy<String>())}")
  println("  proxy<List<Int?>>(): ${formatPTypeDescriptor(proxy<List<Int?>>())}")
  println()

  // Inline nested proxy function test
  println("Inline nested proxy function (typeDescriptorOf through inline function):")
  println("  proxy2<String>(): ${formatPTypeDescriptor(proxy2<String>())}")
  println("  proxy2<List<Int?>>(): ${formatPTypeDescriptor(proxy2<List<Int?>>())}")
  println()

  // generic function test (non-reified - throws exception)
  println("generic function (typeDescriptorOf through non-reified generic function):")
  try {
    generic<String>()
  } catch (e: IllegalStateException) {
    println("  generic<String>(): Throws: ${e.message}")
  }
  println()

  // introspectionData embedded in type descriptors
  println("=== introspectionData in typeDescriptorOf<T>() ===")
  println()

  // Introspectable type: introspectionData is populated
  val personDescriptor = typeDescriptorOf<SimplePerson>()
  val personIntrospectionData = (personDescriptor as? PTypeDescriptor.Concrete)?.introspectionData
  println("typeDescriptorOf<SimplePerson>().introspectionData: $personIntrospectionData")
  println("  kClass: ${personIntrospectionData?.kClass}")
  println("  properties: ${personIntrospectionData?.properties?.map { it.name }}")
  println()

  // Non-introspectable type: introspectionData is null
  val userDescriptor = typeDescriptorOf<User>()
  println("typeDescriptorOf<User>().introspectionData: ${(userDescriptor as? PTypeDescriptor.Concrete)?.introspectionData}")
  println()

  // Property type descriptor also carries introspectionData for introspectable types
  println("Property type descriptors:")
  for (prop in personIntrospectionData!!.properties) {
    val propConcreteType = prop.type as? PTypeDescriptor.Concrete
    println("  ${prop.name}: ${prop.type::class.simpleName}, introspectionData=${propConcreteType?.introspectionData?.kClass}")
  }
  println()

  // Nested introspectable: List<SimplePerson> — List has null, SimplePerson arg has data
  val listPersonDescriptor = typeDescriptorOf<List<SimplePerson>>() as PTypeDescriptor.Concrete.Parameterized
  val listIntrospectionData = listPersonDescriptor.introspectionData
  val personArgDescriptor = listPersonDescriptor.argumentsPTypes[0] as? PTypeDescriptor.Concrete
  println("typeDescriptorOf<List<SimplePerson>>():")
  println("  List introspectionData: $listIntrospectionData")
  println("  SimplePerson arg introspectionData.kClass: ${personArgDescriptor?.introspectionData?.kClass}")
  println()

  // Through inline proxy
  val proxyDescriptor = proxy<SimplePerson>() as? PTypeDescriptor.Concrete
  println("proxy<SimplePerson>().introspectionData.kClass: ${proxyDescriptor?.introspectionData?.kClass}")
  println()

  // Delegated properties test
  println("=== Delegated Properties Test ===")
  println()
  val delegatedExample = DelegatedExample()
  val delegatedData = introspectionOf<DelegatedExample>()

  for (prop in delegatedData.properties) {
    println("  ${prop.name}:")
    println("    isDelegated: ${prop.isDelegated}")
    println("    hasBackingField: ${prop.hasBackingField}")
    if (prop.isDelegated && prop.delegateGetter != null) {
      val delegate = prop.delegateGetter!!(delegatedExample)
      println("    delegate type: ${delegate?.let { it::class.simpleName }}")
      if (delegate is Lazy<*>) {
        println("    isInitialized: ${delegate.isInitialized()}")
      }
    }
    println("    value: ${prop.getter(delegatedExample)}")
    if (prop.isDelegated && prop.delegateGetter != null) {
      val delegate = prop.delegateGetter!!(delegatedExample)
      if (delegate is Lazy<*>) {
        println("    isInitialized (after access): ${delegate.isInitialized()}")
      }
    }
    println()
  }
}

/**
 * Inline proxy function that calls typeDescriptorOf<T>().
 * This tests that typeDescriptorOf works correctly through inline functions.
 */
inline fun <reified T> proxy(): PTypeDescriptor = typeDescriptorOf<T>()
inline fun <reified T> proxy2(): PTypeDescriptor = proxy<T>()

fun <T> generic(): PTypeDescriptor = typeDescriptorOf<T>()

/**
 * Helper function to format PTypeDescriptor for display.
 */
fun formatPTypeDescriptor(info: PTypeDescriptor?): String = when (info) {
  null -> "null"
  is PTypeDescriptor.Concrete.Parameterized -> {
    val nullable = if (info.isNullable) "?" else ""
    val args = info.argumentsPTypes.joinToString(", ") { formatPTypeDescriptor(it) }
    "${info.pType.kClass.qualifiedName}<$args>$nullable"
  }

  is PTypeDescriptor.Concrete -> {
    val nullable = if (info.isNullable) "?" else ""
    "${info.pType.kClass.qualifiedName}$nullable"
  }

  is PTypeDescriptor.Star -> "*"
}
