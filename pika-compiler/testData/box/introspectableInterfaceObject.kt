// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
object Config {
  val version: String = "1.0"
}

fun box(): String {
  val obj: Any = Config

  if (obj !is PIntrospectionProvider) return "FAIL: object should implement PIntrospectionProvider"

  val data = (obj as PIntrospectionProvider).getIntrospectionData()
  if (data.jClass != Config::class.java) return "FAIL: jClass should be Config, got ${data.jClass}"
  if (data.properties.size != 1) return "FAIL: expected 1 property, got ${data.properties.size}"

  val staticData = introspectionOf<Config>()
  if (data !== staticData) return "FAIL: should return same cached instance"

  return "OK"
}
