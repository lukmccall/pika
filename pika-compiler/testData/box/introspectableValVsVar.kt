// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Person(val name: String, var age: Int)

fun box(): String {
  val person = Person("Alice", 30)
  val data = Person.__PIntrospectionData()

  val nameProp = data.properties.find { it.name == "name" }
    ?: return "FAIL: name not found"

  val ageProp = data.properties.find { it.name == "age" }
    ?: return "FAIL: age not found"

  // val should not be mutable
  if (nameProp.isMutable) return "FAIL: name (val) should not be mutable"

  // var should be mutable
  if (!ageProp.isMutable) return "FAIL: age (var) should be mutable"

  return "OK"
}
