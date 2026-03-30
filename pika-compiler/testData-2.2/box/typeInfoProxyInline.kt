// WITH_STDLIB
// CHECK_ASM_LIKE_INSTRUCTIONS
// CURIOUS_ABOUT: box
package test

import io.github.lukmccall.pika.*

inline fun <reified T> proxy() = typeInfo<T>()

fun box(): String {
  val info = proxy<String>()
  if (info !is TypeInfo.Simple) return "FAIL: should be Simple"
  if (info.typeName != "kotlin.String") return "FAIL: expected kotlin.String got ${info.typeName}"
  return "OK"
}
