// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Empty

fun box(): String {
  val empty = Empty()
  val data = introspectionOf<Empty>()

  // Check kClass
  if (data.jClass != Empty::class.java) return "FAIL: kClass"

  // No properties
  if (data.properties.isNotEmpty()) return "FAIL: should have no properties"

  // No functions (except those inherited from Any)
  // Note: we only include functions declared directly on the class
  if (data.functions.isNotEmpty()) return "FAIL: should have no functions"

  // Only @Introspectable annotation
  if (data.annotations.size != 1) return "FAIL: should have 1 annotation (@Introspectable)"
  if (data.annotations[0].jClass != Introspectable::class.java) return "FAIL: annotation should be @Introspectable"

  // No base class (Any doesn't implement Introspectable)
  if (data.baseClass != null) return "FAIL: should have no baseClass"

  return "OK"
}
