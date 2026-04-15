// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Person(val name: String)

fun box(): String {
  // List<Person>: outer type (List) not introspectable, type arg (Person) is
  val listDescriptor = typeDescriptorOf<List<Person>>()
  if (listDescriptor !is PTypeDescriptor.Concrete.Parameterized) return "FAIL: List<Person> should be Parameterized"
  if (listDescriptor.pType.jClass != List::class.java) return "FAIL: List<Person> kClass"
  if (listDescriptor.introspection != null) return "FAIL: List introspection should be null"
  if (listDescriptor.parameters.size != 1) return "FAIL: List<Person> should have 1 type arg"

  // Person type arg should carry introspection
  val personArg = listDescriptor.parameters[0] as? PTypeDescriptor.Concrete
    ?: return "FAIL: Person arg should be Concrete"
  if (personArg.introspection == null) return "FAIL: Person arg introspection should not be null"
  if (personArg.introspection!!.jClass != Person::class.java) return "FAIL: Person arg introspection.jClass"

  // Map<String, Person>: key not introspectable, value is
  val mapDescriptor = typeDescriptorOf<Map<String, Person>>()
  if (mapDescriptor !is PTypeDescriptor.Concrete.Parameterized) return "FAIL: Map should be Parameterized"
  if (mapDescriptor.introspection != null) return "FAIL: Map introspection should be null"
  if (mapDescriptor.parameters.size != 2) return "FAIL: Map should have 2 type args"

  val keyArg = mapDescriptor.parameters[0] as? PTypeDescriptor.Concrete
    ?: return "FAIL: key arg should be Concrete"
  if (keyArg.introspection != null) return "FAIL: String key arg introspection should be null"

  val valueArg = mapDescriptor.parameters[1] as? PTypeDescriptor.Concrete
    ?: return "FAIL: value arg should be Concrete"
  if (valueArg.introspection == null) return "FAIL: Person value arg introspection should not be null"

  return "OK"
}
