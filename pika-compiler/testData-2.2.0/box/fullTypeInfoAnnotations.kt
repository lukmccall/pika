// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

annotation class TestAnnotation(val value: String)

@TestAnnotation("test-class")
class Person(
  @Deprecated("use fullName")
  val name: String
)

fun box(): String {
  val fullPersonInfo = fullTypeInfo<Person>()

  // Test class annotations
  val testAnnotation = fullPersonInfo.classAnnotations.find { it.className == "test.TestAnnotation" }
    ?: return "FAIL: TestAnnotation not found on class"
  if (testAnnotation.arguments["value"] != "test-class") return "FAIL: TestAnnotation value expected 'test-class' but got ${testAnnotation.arguments["value"]}"

  // Test field annotations
  val nameFieldInfo = fullPersonInfo.fields.find { it.name == "name" }
    ?: return "FAIL: name not found"
  val deprecatedAnnotation = nameFieldInfo.annotations.find { it.className == "kotlin.Deprecated" }
    ?: return "FAIL: name should have @Deprecated annotation"
  if (deprecatedAnnotation.kClass != Deprecated::class) return "FAIL: Deprecated annotation kClass"

  return "OK"
}
