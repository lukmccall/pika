// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
open class Animal(val species: String)

@Introspectable
open class Mammal(species: String, val warmBlooded: Boolean) : Animal(species)

@Introspectable
class Dog(species: String, warmBlooded: Boolean, val breed: String) : Mammal(species, warmBlooded)

fun box(): String {
  val dog = Dog("Canine", true, "Labrador")
  val data = Dog.__PIntrospectionData()

  // Dog's own properties
  if (data.properties.size != 1) return "FAIL: Dog should have 1 property"
  if (data.properties[0].name != "breed") return "FAIL: Dog property should be breed"

  // Mammal base class
  val mammalData = data.baseClass
    ?: return "FAIL: Dog should have baseClass"
  if (mammalData.jClass != Mammal::class.java) return "FAIL: baseClass should be Mammal"
  if (mammalData.properties.size != 1) return "FAIL: Mammal should have 1 property"
  if (mammalData.properties[0].name != "warmBlooded") return "FAIL: Mammal property should be warmBlooded"

  // Animal base class (grandparent)
  val animalData = mammalData.baseClass
    ?: return "FAIL: Mammal should have baseClass"
  if (animalData.jClass != Animal::class.java) return "FAIL: grandparent should be Animal"
  if (animalData.properties.size != 1) return "FAIL: Animal should have 1 property"
  if (animalData.properties[0].name != "species") return "FAIL: Animal property should be species"

  // Animal has no further base class (except Any which doesn't implement Introspectable)
  if (animalData.baseClass != null) return "FAIL: Animal should not have baseClass"

  return "OK"
}
