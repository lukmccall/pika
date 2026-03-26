// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

fun box(): String {
  // Test star projections
  val starInfo = typeInfo<List<*>>()
  if (starInfo !is TypeInfo.Parameterized) return "FAIL: List<*> should be Parameterized"
  if (starInfo.typeName != "kotlin.collections.List") return "FAIL: List<*> typeName"
  if (starInfo.typeArguments.size != 1) return "FAIL: List<*> should have 1 type argument"
  if (starInfo.typeArguments[0] !is TypeInfo.Star) return "FAIL: List<*> arg should be Star"

  return "OK"
}
