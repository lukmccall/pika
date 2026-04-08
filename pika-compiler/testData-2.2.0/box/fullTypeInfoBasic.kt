// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

class Person(val name: String)

fun box(): String {
  val fullPersonInfo = fullTypeInfo<Person>()

  // Test basic info
  if (fullPersonInfo.className != "test.Person") return "FAIL: fullTypeInfo className expected test.Person but got ${fullPersonInfo.className}"
  if (fullPersonInfo.kClass != Person::class) return "FAIL: fullTypeInfo kClass"
  if (fullPersonInfo.isNullable) return "FAIL: fullTypeInfo should not be nullable"

  // Test nullable fullTypeInfo
  val nullableFullPersonInfo = fullTypeInfo<Person?>()
  if (!nullableFullPersonInfo.isNullable) return "FAIL: fullTypeInfo<Person?> should be nullable"
  if (nullableFullPersonInfo.className != "test.Person") return "FAIL: nullable fullTypeInfo className"

  return "OK"
}
