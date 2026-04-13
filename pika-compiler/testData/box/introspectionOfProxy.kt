// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Foo(val x: Int)

inline fun <reified T> proxy() = introspectionOf<T>()

fun box(): String {
  val data = proxy<Foo>()
  if (data.kClass != Foo::class) return "FAIL: wrong kClass, got ${data.kClass}"
  if (data.properties.size != 1) return "FAIL: wrong property count, got ${data.properties.size}"
  val foo = Foo(42)
  if (data.properties[0].getter(foo) != 42) return "FAIL: getter returned ${data.properties[0].getter(foo)}"
  return "OK"
}
