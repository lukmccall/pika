// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class MyAnnotation(val value: String)

@Target(AnnotationTarget.PROPERTY)
annotation class Deprecated(val reason: String)

@MyAnnotation("class level")
class Person(
  @MyAnnotation("property level")
  @Deprecated("use fullName instead")
  val name: String
) : Introspectable

fun box(): String {
  val person = Person("Alice")
  val data = person.__PIntrospectionData()

  // Class annotation
  if (data.annotations.size != 1) return "FAIL: expected 1 class annotation"
  val classAnnotation = data.annotations[0]
  if (classAnnotation.kClass != MyAnnotation::class) return "FAIL: class annotation kClass"
  if (classAnnotation.arguments["value"] != "class level") return "FAIL: class annotation value"

  // Property annotations
  val nameProp = data.properties.find { it.name == "name" }
    ?: return "FAIL: name not found"
  if (nameProp.annotations.size != 2) return "FAIL: expected 2 property annotations"

  val myAnnotation = nameProp.annotations.find { it.kClass == MyAnnotation::class }
    ?: return "FAIL: MyAnnotation not found on property"
  if (myAnnotation.arguments["value"] != "property level") return "FAIL: property MyAnnotation value"

  val deprecatedAnnotation = nameProp.annotations.find { it.kClass == Deprecated::class }
    ?: return "FAIL: Deprecated not found on property"
  if (deprecatedAnnotation.arguments["reason"] != "use fullName instead") return "FAIL: Deprecated reason"

  return "OK"
}
