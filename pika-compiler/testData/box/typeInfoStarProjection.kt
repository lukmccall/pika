// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

fun box(): String {
  // Test star projections
  val starInfo = typeDescriptorOf<List<*>>()
  if (starInfo !is PTypeDescriptor.Concrete.Parameterized) return "FAIL: List<*> should be Parameterized"
  if (starInfo.pType.jClass != List::class.java) return "FAIL: List<*> kClass"
  if (starInfo.parameters.size != 1) return "FAIL: List<*> should have 1 type argument"
  if (starInfo.parameters[0] !is PTypeDescriptor.Star) return "FAIL: List<*> arg should be Star"

  return "OK"
}
