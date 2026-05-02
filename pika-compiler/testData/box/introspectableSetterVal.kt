// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Person(val name: String, val age: Int)

fun box(): String {
  val person = Person("Alice", 30)
  val data = introspectionOf<Person>()

  val nameProp = data.properties.find { it.name == "name" } as? PProperty<Person, String>
    ?: return "FAIL: name not found"

  // Verify val has backing field
  if (!nameProp.hasBackingField) return "FAIL: name should have backing field"
  if (nameProp.isMutable) return "FAIL: name should not be mutable"

  // Use set to modify val (allowed because it has a backing field)
  nameProp.set(person, "Bob")

  // Verify change via get
  if (nameProp.get(person) != "Bob") return "FAIL: setter didn't work, got ${nameProp.get(person)}"

  // Verify change via direct access
  if (person.name != "Bob") return "FAIL: direct access shows ${person.name}"

  return "OK"
}
