// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Person(val name: String, val age: Int)

fun box(): String {
  val person = Person("Alice", 30)
  val data = Person.__PIntrospectionData()

  val nameProp = data.properties.find { it.name == "name" }
    ?: return "FAIL: name not found"

  // Verify val has backing field
  if (!nameProp.hasBackingField) return "FAIL: name should have backing field"
  if (nameProp.isMutable) return "FAIL: name should not be mutable"

  // CRITICAL: setter should exist for val with backing field
  if (nameProp.setter == null) return "FAIL: val with backing should have setter"

  // Use setter to modify val
  @Suppress("UNCHECKED_CAST")
  val setter = nameProp.setter as (Person, String) -> Unit
  setter(person, "Bob")

  // Verify change via getter
  @Suppress("UNCHECKED_CAST")
  val getter = nameProp.getter as (Person) -> String
  if (getter(person) != "Bob") return "FAIL: setter didn't work, got ${getter(person)}"

  // Verify change via direct access
  if (person.name != "Bob") return "FAIL: direct access shows ${person.name}"

  return "OK"
}
