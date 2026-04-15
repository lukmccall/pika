// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
object Singleton {
  val version: String = "1.0"
  var counter: Int = 0

  fun increment() {
    counter++
  }
}

fun box(): String {
  val data = Singleton.__PIntrospectionData()

  // Check kClass
  if (data.jClass != Singleton::class.java) return "FAIL: kClass"

  // Check properties
  if (data.properties.size != 2) return "FAIL: expected 2 properties"

  val versionProp = data.properties.find { it.name == "version" }
    ?: return "FAIL: version not found"
  if (versionProp.isMutable) return "FAIL: version should be val"

  val counterProp = data.properties.find { it.name == "counter" }
    ?: return "FAIL: counter not found"
  if (!counterProp.isMutable) return "FAIL: counter should be var"

  // Check functions
  val incrementFunc = data.functions.find { it.name == "increment" }
    ?: return "FAIL: increment not found"

  // Test getter
  @Suppress("UNCHECKED_CAST")
  val versionGetter = versionProp.getter as (Any) -> String
  if (versionGetter(Singleton) != "1.0") return "FAIL: version getter"

  return "OK"
}
