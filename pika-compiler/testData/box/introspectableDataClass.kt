// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
data class Person(val name: String, val age: Int)

fun box(): String {
  val person = Person("Alice", 30)
  val data = Person.__PIntrospectionData()

  // Data class properties
  if (data.properties.size != 2) return "FAIL: expected 2 properties"

  val nameProp = data.properties.find { it.name == "name" }
    ?: return "FAIL: name not found"

  val ageProp = data.properties.find { it.name == "age" }
    ?: return "FAIL: age not found"

  // Both should have backing fields
  if (!nameProp.hasBackingField) return "FAIL: name should have backing field"
  if (!ageProp.hasBackingField) return "FAIL: age should have backing field"

  // Both should have setters (via synthetic accessors)
  if (nameProp.setter == null) return "FAIL: name should have setter"
  if (ageProp.setter == null) return "FAIL: age should have setter"

  // Use setter to modify val
  @Suppress("UNCHECKED_CAST")
  val nameSetter = nameProp.setter as (Person, String) -> Unit
  nameSetter(person, "Bob")

  if (person.name != "Bob") return "FAIL: name setter didn't work"

  @Suppress("UNCHECKED_CAST")
  val ageSetter = ageProp.setter as (Person, Int) -> Unit
  ageSetter(person, 25)

  if (person.age != 25) return "FAIL: age setter didn't work"

  return "OK"
}
