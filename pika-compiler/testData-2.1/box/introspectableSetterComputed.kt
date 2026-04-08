// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

class Person(val firstName: String, val lastName: String) : Introspectable {
  // Computed property - no backing field
  val fullName: String
    get() = "$firstName $lastName"
}

fun box(): String {
  val person = Person("Alice", "Smith")
  val data = person.__PIntrospectionData()

  val fullNameProp = data.properties.find { it.name == "fullName" }
    ?: return "FAIL: fullName not found"

  // Computed property has no backing field
  if (fullNameProp.hasBackingField) return "FAIL: fullName should not have backing field"

  // Setter should be null for computed properties
  if (fullNameProp.setter != null) return "FAIL: computed property should not have setter"

  // Getter should still work
  @Suppress("UNCHECKED_CAST")
  val getter = fullNameProp.getter as (Person) -> String
  if (getter(person) != "Alice Smith") return "FAIL: fullName getter"

  return "OK"
}
