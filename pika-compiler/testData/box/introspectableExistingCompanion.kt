// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Config(val host: String, val port: Int) {
  companion object {
    const val DEFAULT_HOST = "localhost"
    const val DEFAULT_PORT = 8080

    fun default(): Config = Config(DEFAULT_HOST, DEFAULT_PORT)
  }
}

fun box(): String {
  // Verify companion object still works normally
  val config = Config.default()
  if (config.host != "localhost") return "FAIL: default host"
  if (config.port != 8080) return "FAIL: default port"

  // Verify introspection works
  val data = introspectionOf<Config>()
  if (data.jClass != Config::class.java) return "FAIL: jClass"
  if (data.properties.size != 2) return "FAIL: expected 2 properties but got ${data.properties.size}"

  val hostProp = data.properties.find { it.name == "host" }
    ?: return "FAIL: host property not found"
  if (hostProp.get(config) != "localhost") return "FAIL: host getter"

  val portProp = data.properties.find { it.name == "port" }
    ?: return "FAIL: port property not found"
  if (portProp.get(config) != 8080) return "FAIL: port getter"

  // Verify typeDescriptorOf works too
  val td = typeDescriptorOf<Config>()
  if (td !is PTypeDescriptor.Concrete) return "FAIL: typeDescriptor not Concrete"
  if (td.pType.jClass != Config::class.java) return "FAIL: typeDescriptor jClass"

  return "OK"
}
