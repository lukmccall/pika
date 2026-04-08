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
  val data = DelegatedProperties.__PIntrospectionData()

  // Test lazy delegated property
  val lazyProp = data.properties.find { it.name == "lazyValue" }
    ?: return "FAIL: lazyValue not found"
  if (!lazyProp.isDelegated) return "FAIL: lazyValue should be delegated"
  if (lazyProp.delegateGetter == null) return "FAIL: lazyValue should have delegateGetter"
  val lazyDelegate = lazyProp.delegateGetter!!(instance)
  if (lazyDelegate !is Lazy<*>) return "FAIL: delegate should be Lazy"

  // Test regular property (not delegated)
  val regularProp = data.properties.find { it.name == "regularValue" }!!
  if (regularProp.isDelegated) return "FAIL: regularValue should not be delegated"
  if (regularProp.delegateGetter != null) return "FAIL: regularValue delegateGetter should be null"

  // Test computed property (not delegated)
  val computedProp = data.properties.find { it.name == "computedValue" }!!
  if (computedProp.isDelegated) return "FAIL: computedValue should not be delegated"

  return "OK"
}
