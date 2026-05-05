// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Person(val name: String, var age: Int)

fun box(): String {
  val person: Any = Person("Alice", 30)

  if (person !is PIntrospectionProvider) return "FAIL: Person should implement PIntrospectionProvider"

  val data = person.getIntrospectionData()

  if (data.jClass != Person::class.java) return "FAIL: jClass should be Person"
  if (data.properties.size != 2) return "FAIL: expected 2 properties, got ${data.properties.size}"

  val staticData = introspectionOf<Person>()
  if (data !== staticData) return "FAIL: should return same cached instance"

  return "OK"
}
