// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Foo(val x: Int)

class Bar(val x: Int)

fun box(): String {
  if (!pIsIntrospectable<Foo>()) return "FAIL: expected true for @Introspectable class"
  if (pIsIntrospectable<Bar>()) return "FAIL: expected false for non-@Introspectable class"
  return "OK"
}
