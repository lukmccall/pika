package sample

import io.github.lukmccall.pika.*
import java.io.Serializable
import kotlin.reflect.typeOf
import kotlin.time.measureTime


/**
 * Example annotation for testing.
 */
annotation class MyAnnotation(val value: String)

/**
 * User-declared marker registered via `pika { introspectableAnnotation(...) }`
 * in build.gradle.kts. Classes annotated with this should behave identically
 * to ones annotated with `@Introspectable`.
 */
annotation class OptimizedRecord

@OptimizedRecord
class Product(val sku: String, val price: Double)

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
//  repeat(100) {
//    val skotlinTypeOf = measureTime {
//      repeat(10000) {
//        val typeOf = typeOf<SimplePerson>()
//      }
//    }
//
//    val sikaTypeOf = measureTime {
//      repeat(10000) {
//        val pikaTypeOf = typeDescriptorOf<SimplePerson>()
//      }
//    }
//
//    println("kotlinTypeOf = $skotlinTypeOf")
//    println("pikaTypeOf = $sikaTypeOf")
//
//  }
//  return


  println("=== Custom @OptimizedRecord marker (registered via Gradle DSL) ===")
  println()
  val product = Product(sku = "SKU-1", price = 9.99)
  val productData = introspectionOf<Product>()
  println("isIntrospectable<Product>(): ${isIntrospectable<Product>()}")
  println("productData.jClass: ${productData.jClass}")
  println("productData.properties: ${productData.properties.size}")
  for (prop in productData.properties) {
    println("  - ${prop.name}: getter(product)=${prop.getter(product)}")
  }
  println()

  println("=== Introspectable Test ===")
  println()
  val person = SimplePerson("Alice", Address("Paris", "France"))

  // Two equivalent ways to get introspection data:
  // 1. Using the helper function (recommended)
  val data = introspectionOf<SimplePerson>()
  // 2. Or via typeDescriptorOf<SimplePerson>() which includes introspection data

  println("kClass: ${data.jClass}")
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

  // introspection embedded in type descriptors
  println("=== introspection in typeDescriptorOf<T>() ===")
  println()

  // Introspectable type: introspection is populated
  val personDescriptor = typeDescriptorOf<SimplePerson>()
  val personIntrospection = (personDescriptor as? PTypeDescriptor.Concrete)?.introspection
  println("typeDescriptorOf<SimplePerson>().introspection: $personIntrospection")
  println("  kClass: ${personIntrospection?.jClass}")
  println("  properties: ${personIntrospection?.properties?.map { it.name }}")
  println()

  // Non-introspectable type: introspection is null
  val userDescriptor = typeDescriptorOf<User>()
  println("typeDescriptorOf<User>().introspection: ${(userDescriptor as? PTypeDescriptor.Concrete)?.introspection}")
  println()

  // Property type descriptor also carries introspection for introspectable types
  println("Property type descriptors:")
  for (prop in personIntrospection!!.properties) {
    val propConcreteType = prop.type as? PTypeDescriptor.Concrete
    println("  ${prop.name}: ${prop.type::class.simpleName}, introspection=${propConcreteType?.introspection?.jClass}")
  }
  println()

  // Nested introspectable: List<SimplePerson> — List has null, SimplePerson arg has data
  val listPersonDescriptor = typeDescriptorOf<List<SimplePerson>>() as PTypeDescriptor.Concrete.Parameterized
  val listIntrospection = listPersonDescriptor.introspection
  val personArgDescriptor = listPersonDescriptor.parameters[0] as? PTypeDescriptor.Concrete
  println("typeDescriptorOf<List<SimplePerson>>():")
  println("  List introspection: $listIntrospection")
  println("  SimplePerson arg introspection.jClass: ${personArgDescriptor?.introspection?.jClass}")
  println()

  // Through inline proxy
  val proxyDescriptor = proxy<SimplePerson>() as? PTypeDescriptor.Concrete
  println("proxy<SimplePerson>().introspection.jClass: ${proxyDescriptor?.introspection?.jClass}")
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
    val args = info.parameters.joinToString(", ") { formatPTypeDescriptor(it) }
    "${info.pType.jClass.canonicalName}<$args>$nullable"
  }

  is PTypeDescriptor.Concrete -> {
    val nullable = if (info.isNullable) "?" else ""
    "${info.pType.jClass.canonicalName}$nullable"
  }

  is PTypeDescriptor.Star -> "*"
}
