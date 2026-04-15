// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Person(val name: String, var age: Int)

class NonIntrospectable(val value: Int)

fun box(): String {
  // Introspectable class should have non-null introspection
  val personDescriptor = typeDescriptorOf<Person>()
  if (personDescriptor !is PTypeDescriptor.Concrete) return "FAIL: Person should be Concrete"
  if (personDescriptor.introspection == null) return "FAIL: Person introspection should not be null"

  // introspection should match introspectionOf<Person>()
  val data = personDescriptor.introspection!!
  if (data.jClass != Person::class.java) return "FAIL: introspection.jClass should be Person"
  if (data.properties.size != 2) return "FAIL: should have 2 properties, got ${data.properties.size}"

  // Cross-check with introspectionOf — should be the same data
  val direct = introspectionOf<Person>()
  if (data.jClass != direct.jClass) return "FAIL: jClass mismatch"
  if (data.properties.size != direct.properties.size) return "FAIL: properties size mismatch"

  // Nullable introspectable class should also have introspection
  val nullableDescriptor = typeDescriptorOf<Person?>()
  if (nullableDescriptor !is PTypeDescriptor.Concrete) return "FAIL: Person? should be Concrete"
  if (!nullableDescriptor.isNullable) return "FAIL: Person? should be nullable"
  if (nullableDescriptor.introspection == null) return "FAIL: Person? introspection should not be null"

  // Non-introspectable class should have null introspection
  val nonDescriptor = typeDescriptorOf<NonIntrospectable>()
  if (nonDescriptor !is PTypeDescriptor.Concrete) return "FAIL: NonIntrospectable should be Concrete"
  if (nonDescriptor.introspection != null) return "FAIL: NonIntrospectable introspection should be null"

  // Standard library types should have null introspection
  val stringDescriptor = typeDescriptorOf<String>()
  if (stringDescriptor !is PTypeDescriptor.Concrete) return "FAIL: String should be Concrete"
  if (stringDescriptor.introspection != null) return "FAIL: String introspection should be null"

  return "OK"
}
