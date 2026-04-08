// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

class Empty : Introspectable

fun box(): String {
  val empty = Empty()
  val data = empty.__PIntrospectionData()

  // Check kClass
  if (data.kClass != Empty::class) return "FAIL: kClass"

  // No properties
  if (data.properties.isNotEmpty()) return "FAIL: should have no properties"

  // No functions (except those inherited from Any)
  // Note: we only include functions declared directly on the class
  if (data.functions.isNotEmpty()) return "FAIL: should have no functions"

  // No annotations
  if (data.annotations.isNotEmpty()) return "FAIL: should have no annotations"

  // No base class (Any doesn't implement Introspectable)
  if (data.baseClass != null) return "FAIL: should have no baseClass"

  return "OK"
}
