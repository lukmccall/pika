// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

class Person(val name: String)

fun box(): String {
  // Test class types
  val personInfo = typeDescriptorOf<Person>()
  if (personInfo !is PTypeDescriptor.Concrete) return "FAIL: Person should be PTypeDescriptor.Concrete"
  if (personInfo.pType.kClass != Person::class) return "FAIL: Person kClass"
  if (personInfo.isNullable) return "FAIL: Person should not be nullable"

  // Test nullable class
  val nullablePersonInfo = typeDescriptorOf<Person?>()
  if (nullablePersonInfo !is PTypeDescriptor.Concrete) return "FAIL: Person? should be PTypeDescriptor.Concrete"
  if (!nullablePersonInfo.isNullable) return "FAIL: Person? should be nullable"

  return "OK"
}
