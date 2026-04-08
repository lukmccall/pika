// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
open class Animal(val species: String)

@Introspectable
class Dog(species: String, val breed: String) : Animal(species)

fun box(): String {
  val dog = Dog("Canine", "Labrador")
  val data = Dog.__PIntrospectionData()

  // Dog's own properties only
  if (data.properties.size != 1) return "FAIL: Dog should have 1 declared property, got ${data.properties.size}"
  if (data.properties[0].name != "breed") return "FAIL: Dog property should be breed"

  // Base class reference
  val baseData = data.baseClass
    ?: return "FAIL: Dog should have baseClass reference"

  if (baseData.kClass != Animal::class) return "FAIL: baseClass should be Animal"
  if (baseData.properties.size != 1) return "FAIL: Animal should have 1 property"
  if (baseData.properties[0].name != "species") return "FAIL: Animal property should be species"

  // Access inherited property via baseClass
  @Suppress("UNCHECKED_CAST")
  val speciesGetter = baseData.properties[0].getter as (Any) -> String
  if (speciesGetter(dog) != "Canine") return "FAIL: species getter"

  return "OK"
}
