// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

// Registered via the `introspectableAnnotation=test.OptimizedRecord` plugin CLI
// option (see ExtensionRegistrarConfigurator in test-fixtures). Classes annotated
// with this user-defined marker must be treated identically to @Introspectable.
annotation class OptimizedRecord

@OptimizedRecord
class Person(val name: String)

fun box(): String {
  val person = Person("Alice")
  val data = Person.__PIntrospectionData()

  if (data.jClass != Person::class.java) return "FAIL: jClass expected Person::class.java"

  if (data.properties.size != 1) return "FAIL: expected 1 property but got ${data.properties.size}"

  val nameProp = data.properties.find { it.name == "name" }
    ?: return "FAIL: name property not found"

  if (nameProp.getter(person) != "Alice") return "FAIL: getter expected Alice but got ${nameProp.getter(person)}"

  return "OK"
}
