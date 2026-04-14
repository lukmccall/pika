// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Address(val city: String, val zip: String)

@Introspectable
class Person(val name: String, val address: Address)

fun box(): String {
  val personDescriptor = typeDescriptorOf<Person>()
  if (personDescriptor !is PTypeDescriptor.Concrete) return "FAIL: Person should be Concrete"
  val personData = personDescriptor.introspectionData
    ?: return "FAIL: Person introspectionData should not be null"

  if (personData.properties.size != 2) return "FAIL: should have 2 properties, got ${personData.properties.size}"

  // Find the address property
  val addressProp = personData.properties.find { it.name == "address" }
    ?: return "FAIL: address property not found"

  // The type descriptor of the address property should carry Address's introspectionData
  val addressTypeDescriptor = addressProp.type as? PTypeDescriptor.Concrete
    ?: return "FAIL: address property type should be Concrete"

  if (addressTypeDescriptor.pType.kClass != Address::class)
    return "FAIL: address type kClass should be Address"

  val addressData = addressTypeDescriptor.introspectionData
    ?: return "FAIL: address property type introspectionData should not be null"

  if (addressData.kClass != Address::class)
    return "FAIL: nested introspectionData.kClass should be Address"

  if (addressData.properties.size != 2)
    return "FAIL: Address should have 2 properties, got ${addressData.properties.size}"

  val cityProp = addressData.properties.find { it.name == "city" }
    ?: return "FAIL: city property not found in nested introspectionData"

  // Cross-check: nested introspectionData matches introspectionOf<Address>()
  val directAddressData = introspectionOf<Address>()
  if (addressData.kClass != directAddressData.kClass)
    return "FAIL: nested kClass mismatch with introspectionOf"
  if (addressData.properties.size != directAddressData.properties.size)
    return "FAIL: nested properties size mismatch with introspectionOf"

  // name property type (String) should have null introspectionData
  val nameProp = personData.properties.find { it.name == "name" }
    ?: return "FAIL: name property not found"
  val nameType = nameProp.type as? PTypeDescriptor.Concrete
    ?: return "FAIL: name property type should be Concrete"
  if (nameType.introspectionData != null)
    return "FAIL: String property type introspectionData should be null"

  return "OK"
}
