// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

class Person(val name: String)

fun box(): String {
  // Test class types
  val personInfo = typeInfo<Person>()
  if (personInfo !is TypeInfo.Simple) return "FAIL: Person should be TypeInfo.Simple"
  if (personInfo.typeName != "test.Person") return "FAIL: Person typeName expected test.Person but got ${personInfo.typeName}"
  if (personInfo.kClass != Person::class) return "FAIL: Person kClass"
  if (personInfo.isNullable) return "FAIL: Person should not be nullable"

  // Test nullable class
  val nullablePersonInfo = typeInfo<Person?>()
  if (nullablePersonInfo !is TypeInfo.Simple) return "FAIL: Person? should be TypeInfo.Simple"
  if (!nullablePersonInfo.isNullable) return "FAIL: Person? should be nullable"

  return "OK"
}
