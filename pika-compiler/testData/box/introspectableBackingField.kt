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
  val data = introspectionOf<Person>()

  val nameProp = data.properties.find { it.name == "name" }
    ?: return "FAIL: name not found"

  val greetingProp = data.properties.find { it.name == "greeting" }
    ?: return "FAIL: greeting not found"

  // name has backing field
  if (!nameProp.hasBackingField) return "FAIL: name should have backing field"

  // greeting is computed, no backing field
  if (greetingProp.hasBackingField) return "FAIL: greeting should not have backing field"

  // Verify getters work
  if (nameProp.get(person) != "Alice") return "FAIL: name getter"
  if (greetingProp.get(person) != "Hello, Alice") return "FAIL: greeting getter"

  // name has backing field so set should work
  if (!nameProp.hasBackingField) return "FAIL: name should have backing field for set"

  // greeting has no backing field so set should throw
  if (greetingProp.hasBackingField) return "FAIL: greeting should not have backing field"

  return "OK"
}
