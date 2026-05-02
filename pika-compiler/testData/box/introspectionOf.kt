// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Person(val name: String, var age: Int)

fun box(): String {
  val person = Person("Alice", 30)

  // Use introspectionOf helper function
  val data = introspectionOf<Person>()

  // Verify kClass
  if (data.jClass != Person::class.java) return "FAIL: kClass should be Person"

  // Verify properties
  if (data.properties.size != 2) return "FAIL: should have 2 properties, got ${data.properties.size}"

  val nameProp = data.properties.find { it.name == "name" }
    ?: return "FAIL: name property not found"
  val ageProp = data.properties.find { it.name == "age" } as? PProperty<Person, Int>
    ?: return "FAIL: age property not found"

  // Test getter via get()
  if (nameProp.get(person) != "Alice") return "FAIL: name getter returned ${nameProp.get(person)}"
  if (ageProp.get(person) != 30) return "FAIL: age getter returned ${ageProp.get(person)}"

  // Test setter via set()
  ageProp.set(person, 31)
  if (person.age != 31) return "FAIL: age setter didn't work"

  return "OK"
}
