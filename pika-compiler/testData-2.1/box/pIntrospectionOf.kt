// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

class Person(val name: String, var age: Int) : Introspectable

fun box(): String {
  val person = Person("Alice", 30)

  // Use pIntrospectionOf helper function
  val data = pIntrospectionOf(person)

  // Verify kClass
  if (data.kClass != Person::class) return "FAIL: kClass should be Person"

  // Verify properties
  if (data.properties.size != 2) return "FAIL: should have 2 properties, got ${data.properties.size}"

  val nameProp = data.properties.find { it.name == "name" }
    ?: return "FAIL: name property not found"
  val ageProp = data.properties.find { it.name == "age" }
    ?: return "FAIL: age property not found"

  // Test getter via pIntrospectionOf result
  if (nameProp.getter(person) != "Alice") return "FAIL: name getter returned ${nameProp.getter(person)}"
  if (ageProp.getter(person) != 30) return "FAIL: age getter returned ${ageProp.getter(person)}"

  // Test setter (need to cast due to star projection)
  @Suppress("UNCHECKED_CAST")
  val ageSetter = ageProp.setter as? ((Person, Int) -> Unit)
    ?: return "FAIL: age setter should not be null"
  ageSetter(person, 31)
  if (person.age != 31) return "FAIL: age setter didn't work"

  return "OK"
}
