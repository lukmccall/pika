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
 * Simple test class for Introspectable.
 */
open class SimplePerson(val name: String) : Introspectable

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
  val person = SimplePerson("Alice")

  // Two equivalent ways to get introspection data:
  // 1. Using the helper function (recommended)
  val data = pIntrospectionOf(person)
  // 2. Or directly: person.__PIntrospectionData()

  println("kClass: ${data.kClass}")
  println("properties: ${data.properties.size}")

  for (prop in data.properties) {
    println("  - ${prop.name}: getter(person)=${prop.getter(person)}")
  }
  println()

  println("=== fullTypeInfo<T>() Examples ===")
  println()

  // Get full type information for User class
  val userInfo: FullTypeInfo = fullTypeInfo<User>()

  println("Full Type Information for User:")
  println("  Class: ${userInfo.className}")
  println("  KClass: ${userInfo.kClass}")
  println("  Is Nullable: ${userInfo.isNullable}")
  println()

  println("Class Annotations:")
  if (userInfo.classAnnotations.isEmpty()) {
    println("  (none)")
  } else {
    for (annotation in userInfo.classAnnotations) {
      println("  - @${annotation.className}")
      if (annotation.arguments.isNotEmpty()) {
        println("    Arguments: ${annotation.arguments}")
      }
    }
  }
  println()

  println("Inheritance:")
  println("  Base Class: ${userInfo.baseClass?.className ?: "none"}")
  if (userInfo.baseClass != null) {
    println("    Base Class Fields: ${userInfo.baseClass!!.fields.map { it.name }}")
  }
  println("  Interfaces: ${userInfo.interfaces.map { it.simpleName }}")
  println()

  println("Fields (declared on User only):")
  for (field in userInfo.fields) {
    val visibilityStr = when (field.visibility) {
      Visibility.PUBLIC -> "public"
      Visibility.PRIVATE -> "private"
      Visibility.PROTECTED -> "protected"
      Visibility.INTERNAL -> "internal"
    }
    val mutabilityStr = if (field.isMutable) "var" else "val"
    println("  - $visibilityStr $mutabilityStr ${field.name}: ${formatPTypeDescriptor(field.pTypeDescriptor)}")
    if (field.annotations.isNotEmpty()) {
      for (annotation in field.annotations) {
        println("      @${annotation.className}")
      }
    }
  }
  println()

  println("=== pTypeDescriptorOf<T>() Examples (basic type info) ===")
  println()

  // Simple types
  println("Simple types:")
  println("  pTypeDescriptorOf<String>(): ${formatPTypeDescriptor(pTypeDescriptorOf<String>())}")
  println("  pTypeDescriptorOf<Int>(): ${formatPTypeDescriptor(pTypeDescriptorOf<Int>())}")
  println("  pTypeDescriptorOf<Boolean>(): ${formatPTypeDescriptor(pTypeDescriptorOf<Boolean>())}")
  println()

  // Nullable types
  println("Nullable types:")
  println("  pTypeDescriptorOf<Int?>(): ${formatPTypeDescriptor(pTypeDescriptorOf<Int?>())}")
  println("  pTypeDescriptorOf<String?>(): ${formatPTypeDescriptor(pTypeDescriptorOf<String?>())}")
  println()

  // Parameterized types
  println("Parameterized types:")
  println("  pTypeDescriptorOf<List<String>>(): ${formatPTypeDescriptor(pTypeDescriptorOf<List<String>>())}")
  println("  pTypeDescriptorOf<Map<String, Int>>(): ${formatPTypeDescriptor(pTypeDescriptorOf<Map<String, Int>>())}")
  println()

  // Nested generics
  println("Nested generics:")
  println("  pTypeDescriptorOf<Map<String, List<Int?>>>(): ${formatPTypeDescriptor(pTypeDescriptorOf<Map<String, List<Int?>>>())}")
  println()

  // Star projections
  println("Star projections:")
  println("  pTypeDescriptorOf<List<*>>(): ${formatPTypeDescriptor(pTypeDescriptorOf<List<*>>())}")
  println()

  // Class types
  println("Class types:")
  println("  pTypeDescriptorOf<User>(): ${formatPTypeDescriptor(pTypeDescriptorOf<User>())}")
  println("  pTypeDescriptorOf<User?>(): ${formatPTypeDescriptor(pTypeDescriptorOf<User?>())}")
  println()

  // Inline proxy function test
  println("Inline proxy function (pTypeDescriptorOf through inline function):")
  println("  proxy<String>(): ${formatPTypeDescriptor(proxy<String>())}")
  println("  proxy<List<Int?>>(): ${formatPTypeDescriptor(proxy<List<Int?>>())}")
  println()

  // Inline nested proxy function test
  println("Inline nested proxy function (pTypeDescriptorOf through inline function):")
  println("  proxy2<String>(): ${formatPTypeDescriptor(proxy2<String>())}")
  println("  proxy2<List<Int?>>(): ${formatPTypeDescriptor(proxy2<List<Int?>>())}")
  println()

  // generic function test (non-reified - throws exception)
  println("generic function (pTypeDescriptorOf through non-reified generic function):")
  try {
    generic<String>()
  } catch (e: IllegalStateException) {
    println("  generic<String>(): Throws: ${e.message}")
  }
}

/**
 * Inline proxy function that calls pTypeDescriptorOf<T>().
 * This tests that pTypeDescriptorOf works correctly through inline functions.
 */
inline fun <reified T> proxy(): PTypeDescriptor = pTypeDescriptorOf<T>()
inline fun <reified T> proxy2(): PTypeDescriptor = proxy<T>()

fun <T> generic(): PTypeDescriptor = pTypeDescriptorOf<T>()

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
