// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class DelegatedProperties {
  val lazyValue by lazy { 42 }
  val regularValue: String = "hello"
  val computedValue: Int get() = 100
}

fun box(): String {
  val instance = DelegatedProperties()
  val data = introspectionOf<DelegatedProperties>()

  // Test lazy delegated property
  val lazyProp = data.properties.find { it.name == "lazyValue" }
    ?: return "FAIL: lazyValue not found"
  if (!lazyProp.isDelegated) return "FAIL: lazyValue should be delegated"

  // Test regular property (not delegated)
  val regularProp = data.properties.find { it.name == "regularValue" }!!
  if (regularProp.isDelegated) return "FAIL: regularValue should not be delegated"

  // Test computed property (not delegated)
  val computedProp = data.properties.find { it.name == "computedValue" }!!
  if (computedProp.isDelegated) return "FAIL: computedValue should not be delegated"

  // Test getter via get()
  if (lazyProp.get(instance) != 42) return "FAIL: lazyValue getter"
  if (regularProp.get(instance) != "hello") return "FAIL: regularValue getter"
  if (computedProp.get(instance) != 100) return "FAIL: computedValue getter"

  return "OK"
}
