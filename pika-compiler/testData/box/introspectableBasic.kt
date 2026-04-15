// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Person(val name: String)

fun box(): String {
  val person = Person("Alice")
  val data = Person.__PIntrospectionData()

  // Test kClass
  if (data.jClass != Person::class.java) return "FAIL: kClass expected Person::class.java"

  // Test properties count
  if (data.properties.size != 1) return "FAIL: expected 1 property but got ${data.properties.size}"

  // Test property name
  val nameProp = data.properties.find { it.name == "name" }
    ?: return "FAIL: name property not found"

  // Test getter
  if (nameProp.getter(person) != "Alice") return "FAIL: getter expected Alice but got ${nameProp.getter(person)}"

  return "OK"
}
