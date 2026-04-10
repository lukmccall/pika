// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Foo(val x: Int)

class Bar(val x: Int)

inline fun <reified T : Any> proxy() = pIsIntrospectable<T>()

fun box(): String {
  if (!proxy<Foo>()) return "FAIL: expected true for @Introspectable class through proxy"
  if (proxy<Bar>()) return "FAIL: expected false for non-@Introspectable class through proxy"
  return "OK"
}
