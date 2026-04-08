// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Person(
  val publicProp: String,
  private val privateProp: String,
  protected val protectedProp: String,
  internal val internalProp: String
)

fun box(): String {
  val person = Person("pub", "priv", "prot", "intern")
  val data = Person.__PIntrospectionData()

  val publicProp = data.properties.find { it.name == "publicProp" }
    ?: return "FAIL: publicProp not found"
  if (publicProp.visibility != PVisibility.PUBLIC) return "FAIL: publicProp visibility"

  val privateProp = data.properties.find { it.name == "privateProp" }
    ?: return "FAIL: privateProp not found"
  if (privateProp.visibility != PVisibility.PRIVATE) return "FAIL: privateProp visibility"

  val protectedProp = data.properties.find { it.name == "protectedProp" }
    ?: return "FAIL: protectedProp not found"
  if (protectedProp.visibility != PVisibility.PROTECTED) return "FAIL: protectedProp visibility"

  val internalProp = data.properties.find { it.name == "internalProp" }
    ?: return "FAIL: internalProp not found"
  if (internalProp.visibility != PVisibility.INTERNAL) return "FAIL: internalProp visibility"

  return "OK"
}
