// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
open class Animal(val species: String)

@Introspectable
class Dog(species: String, val breed: String) : Animal(species)

fun box(): String {
  val dog: Any = Dog("Canine", "Lab")
  val animal: Any = Animal("Feline")

  if (dog !is PIntrospectionProvider) return "FAIL: Dog should be PIntrospectionProvider"
  if (animal !is PIntrospectionProvider) return "FAIL: Animal should be PIntrospectionProvider"

  val dogData = (dog as PIntrospectionProvider).getIntrospectionData()
  val animalData = (animal as PIntrospectionProvider).getIntrospectionData()

  if (dogData.jClass != Dog::class.java) return "FAIL: dog data should be Dog, got ${dogData.jClass}"
  if (animalData.jClass != Animal::class.java) return "FAIL: animal data should be Animal, got ${animalData.jClass}"

  return "OK"
}
