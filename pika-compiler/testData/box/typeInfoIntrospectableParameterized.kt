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
  if (listDescriptor.introspectionData != null) return "FAIL: List introspectionData should be null"
  if (listDescriptor.argumentsPTypes.size != 1) return "FAIL: List<Person> should have 1 type arg"

  // Person type arg should carry introspectionData
  val personArg = listDescriptor.argumentsPTypes[0] as? PTypeDescriptor.Concrete
    ?: return "FAIL: Person arg should be Concrete"
  if (personArg.introspectionData == null) return "FAIL: Person arg introspectionData should not be null"
  if (personArg.introspectionData!!.jClass != Person::class.java) return "FAIL: Person arg introspectionData.jClass"

  // Map<String, Person>: key not introspectable, value is
  val mapDescriptor = typeDescriptorOf<Map<String, Person>>()
  if (mapDescriptor !is PTypeDescriptor.Concrete.Parameterized) return "FAIL: Map should be Parameterized"
  if (mapDescriptor.introspectionData != null) return "FAIL: Map introspectionData should be null"
  if (mapDescriptor.argumentsPTypes.size != 2) return "FAIL: Map should have 2 type args"

  val keyArg = mapDescriptor.argumentsPTypes[0] as? PTypeDescriptor.Concrete
    ?: return "FAIL: key arg should be Concrete"
  if (keyArg.introspectionData != null) return "FAIL: String key arg introspectionData should be null"

  val valueArg = mapDescriptor.argumentsPTypes[1] as? PTypeDescriptor.Concrete
    ?: return "FAIL: value arg should be Concrete"
  if (valueArg.introspectionData == null) return "FAIL: Person value arg introspectionData should not be null"

  return "OK"
}
