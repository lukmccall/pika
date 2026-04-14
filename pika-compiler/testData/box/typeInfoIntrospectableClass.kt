// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Person(val name: String, var age: Int)

class NonIntrospectable(val value: Int)

fun box(): String {
  // Introspectable class should have non-null introspectionData
  val personDescriptor = typeDescriptorOf<Person>()
  if (personDescriptor !is PTypeDescriptor.Concrete) return "FAIL: Person should be Concrete"
  if (personDescriptor.introspectionData == null) return "FAIL: Person introspectionData should not be null"

  // introspectionData should match introspectionOf<Person>()
  val data = personDescriptor.introspectionData!!
  if (data.kClass != Person::class) return "FAIL: introspectionData.kClass should be Person"
  if (data.properties.size != 2) return "FAIL: should have 2 properties, got ${data.properties.size}"

  // Cross-check with introspectionOf — should be the same data
  val direct = introspectionOf<Person>()
  if (data.kClass != direct.kClass) return "FAIL: kClass mismatch"
  if (data.properties.size != direct.properties.size) return "FAIL: properties size mismatch"

  // Nullable introspectable class should also have introspectionData
  val nullableDescriptor = typeDescriptorOf<Person?>()
  if (nullableDescriptor !is PTypeDescriptor.Concrete) return "FAIL: Person? should be Concrete"
  if (!nullableDescriptor.isNullable) return "FAIL: Person? should be nullable"
  if (nullableDescriptor.introspectionData == null) return "FAIL: Person? introspectionData should not be null"

  // Non-introspectable class should have null introspectionData
  val nonDescriptor = typeDescriptorOf<NonIntrospectable>()
  if (nonDescriptor !is PTypeDescriptor.Concrete) return "FAIL: NonIntrospectable should be Concrete"
  if (nonDescriptor.introspectionData != null) return "FAIL: NonIntrospectable introspectionData should be null"

  // Standard library types should have null introspectionData
  val stringDescriptor = typeDescriptorOf<String>()
  if (stringDescriptor !is PTypeDescriptor.Concrete) return "FAIL: String should be Concrete"
  if (stringDescriptor.introspectionData != null) return "FAIL: String introspectionData should be null"

  return "OK"
}
