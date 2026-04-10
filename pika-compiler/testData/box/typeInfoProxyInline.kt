// WITH_STDLIB
// CHECK_ASM_LIKE_INSTRUCTIONS
// CURIOUS_ABOUT: box
package test

import io.github.lukmccall.pika.*

inline fun <reified T> proxy() = typeDescriptorOf<T>()

fun box(): String {
  val info = proxy<String>()
  if (info !is PTypeDescriptor.Concrete) return "FAIL: should be Concrete"
  if (info.pType.kClass != String::class) return "FAIL: expected String::class got ${info.pType.kClass}"
  return "OK"
}
