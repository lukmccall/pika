// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
data class Person(val name: String, val age: Int)

fun box(): String {
  val person = Person("Alice", 30)
  val data = introspectionOf<Person>()

  // Data class properties
  if (data.properties.size != 2) return "FAIL: expected 2 properties"

  val nameProp = data.properties.find { it.name == "name" } as? PProperty<Person, String>
    ?: return "FAIL: name not found"

  val ageProp = data.properties.find { it.name == "age" } as? PProperty<Person, Int>
    ?: return "FAIL: age not found"

  // Both should have backing fields
  if (!nameProp.hasBackingField) return "FAIL: name should have backing field"
  if (!ageProp.hasBackingField) return "FAIL: age should have backing field"

  // Use set to modify val
  nameProp.set(person, "Bob")

  if (person.name != "Bob") return "FAIL: name setter didn't work"

  ageProp.set(person, 25)

  if (person.age != 25) return "FAIL: age setter didn't work"

  return "OK"
}
