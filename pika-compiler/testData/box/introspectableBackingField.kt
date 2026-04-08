// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Person(val name: String) {
  // Computed property without backing field
  val greeting: String
    get() = "Hello, $name"
}

fun box(): String {
  val person = Person("Alice")
  val data = Person.__PIntrospectionData()

  val nameProp = data.properties.find { it.name == "name" }
    ?: return "FAIL: name not found"

  val greetingProp = data.properties.find { it.name == "greeting" }
    ?: return "FAIL: greeting not found"

  // name has backing field
  if (!nameProp.hasBackingField) return "FAIL: name should have backing field"

  // greeting is computed, no backing field
  if (greetingProp.hasBackingField) return "FAIL: greeting should not have backing field"

  // Verify getters work
  if (nameProp.getter(person) != "Alice") return "FAIL: name getter"
  if (greetingProp.getter(person) != "Hello, Alice") return "FAIL: greeting getter"

  // name has setter (because it has backing field)
  if (nameProp.setter == null) return "FAIL: name should have setter"

  // greeting has no setter (computed property)
  if (greetingProp.setter != null) return "FAIL: greeting should not have setter"

  return "OK"
}
