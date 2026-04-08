// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

fun box(): String {
  // Test star projections
  val starInfo = pTypeDescriptorOf<List<*>>()
  if (starInfo !is PTypeDescriptor.Concrete.Parameterized) return "FAIL: List<*> should be Parameterized"
  if (starInfo.pType.kClass != List::class) return "FAIL: List<*> kClass"
  if (starInfo.argumentsPTypes.size != 1) return "FAIL: List<*> should have 1 type argument"
  if (starInfo.argumentsPTypes[0] !is PTypeDescriptor.Star) return "FAIL: List<*> arg should be Star"

  return "OK"
}
