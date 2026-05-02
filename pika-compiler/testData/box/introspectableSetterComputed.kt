// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Person(val firstName: String, val lastName: String) {
  // Computed property - no backing field
  val fullName: String
    get() = "$firstName $lastName"
}

fun box(): String {
  val person = Person("Alice", "Smith")
  val data = introspectionOf<Person>()

  val fullNameProp = data.properties.find { it.name == "fullName" } as? PProperty<Person, String>
    ?: return "FAIL: fullName not found"

  // Computed property has no backing field
  if (fullNameProp.hasBackingField) return "FAIL: fullName should not have backing field"

  // Getter should still work
  if (fullNameProp.get(person) != "Alice Smith") return "FAIL: fullName getter"

  // Set should throw for computed properties
  try {
    fullNameProp.set(person, "should fail")
    return "FAIL: set should throw for computed property"
  } catch (e: UnsupportedOperationException) {
    // expected
  }

  return "OK"
}
