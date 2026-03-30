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
    println("  - $visibilityStr $mutabilityStr ${field.name}: ${formatTypeInfo(field.typeInfo)}")
    if (field.annotations.isNotEmpty()) {
      for (annotation in field.annotations) {
        println("      @${annotation.className}")
      }
    }
  }
  println()

  println("=== typeInfo<T>() Examples (basic type info) ===")
  println()

  // Simple types
  println("Simple types:")
  println("  typeInfo<String>(): ${formatTypeInfo(typeInfo<String>())}")
  println("  typeInfo<Int>(): ${formatTypeInfo(typeInfo<Int>())}")
  println("  typeInfo<Boolean>(): ${formatTypeInfo(typeInfo<Boolean>())}")
  println()

  // Nullable types
  println("Nullable types:")
  println("  typeInfo<Int?>(): ${formatTypeInfo(typeInfo<Int?>())}")
  println("  typeInfo<String?>(): ${formatTypeInfo(typeInfo<String?>())}")
  println()

  // Parameterized types
  println("Parameterized types:")
  println("  typeInfo<List<String>>(): ${formatTypeInfo(typeInfo<List<String>>())}")
  println("  typeInfo<Map<String, Int>>(): ${formatTypeInfo(typeInfo<Map<String, Int>>())}")
  println()

  // Nested generics
  println("Nested generics:")
  println("  typeInfo<Map<String, List<Int?>>>(): ${formatTypeInfo(typeInfo<Map<String, List<Int?>>>())}")
  println()

  // Star projections
  println("Star projections:")
  println("  typeInfo<List<*>>(): ${formatTypeInfo(typeInfo<List<*>>())}")
  println()

  // Class types
  println("Class types:")
  println("  typeInfo<User>(): ${formatTypeInfo(typeInfo<User>())}")
  println("  typeInfo<User?>(): ${formatTypeInfo(typeInfo<User?>())}")
  println()

  // Inline proxy function test
  println("Inline proxy function (typeInfo through inline function):")
  println("  proxy<String>(): ${formatTypeInfo(proxy<String>())}")
  println("  proxy<List<Int?>>(): ${formatTypeInfo(proxy<List<Int?>>())}")
  println()

  // Inline nested proxy function test
  println("Inline nested proxy function (typeInfo through inline function):")
  println("  proxy2<String>(): ${formatTypeInfo(proxy2<String>())}")
  println("  proxy2<List<Int?>>(): ${formatTypeInfo(proxy2<List<Int?>>())}")
  println()

  // generic function test (non-reified - throws exception)
  println("generic function (typeInfo through non-reified generic function):")
  try {
    generic<String>()
  } catch (e: IllegalStateException) {
    println("  generic<String>(): Throws: ${e.message}")
  }
}

/**
 * Inline proxy function that calls typeInfo<T>().
 * This tests that typeInfo works correctly through inline functions.
 */
inline fun <reified T> proxy(): TypeInfo = typeInfo<T>()
inline fun <reified T> proxy2(): TypeInfo = proxy<T>()

fun <T> generic(): TypeInfo? = typeInfo<T>()
/**
 * Helper function to format TypeInfo for display.
 */
fun formatTypeInfo(info: TypeInfo?): String = when (info) {
  null -> "null"
  is TypeInfo.Simple -> {
    val nullable = if (info.isNullable) "?" else ""
    "${info.typeName}$nullable"
  }

  is TypeInfo.Parameterized -> {
    val nullable = if (info.isNullable) "?" else ""
    val args = info.typeArguments.joinToString(", ") { formatTypeInfo(it) }
    "${info.typeName}<$args>$nullable"
  }

  is TypeInfo.Star -> "*"
}
