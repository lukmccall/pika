// WITH_STDLIB
// Regression test: calling introspectionOf<T>() through a proxy inline function where T is
// substituted with a non-@Introspectable type should compile without a bytecode stack underflow
// (AnalyzerException). The result is null for non-introspectable types.
package test

import io.github.lukmccall.pika.*

@Introspectable
class Foo(val x: Int)

class Bar(val y: String) // NOT @Introspectable

inline fun <reified T> proxy(): PIntrospectionData<T & Any>? {
  @Suppress("UNCHECKED_CAST")
  return introspectionOf<T>() as? PIntrospectionData<T & Any>
}

fun box(): String {
  val fooData = proxy<Foo>()
  if (fooData == null) return "FAIL: introspectable class returned null"
  if (fooData.kClass != Foo::class) return "FAIL: wrong kClass for Foo, got ${fooData.kClass}"
  if (fooData.properties.size != 1) return "FAIL: wrong property count, got ${fooData.properties.size}"

  val barData = proxy<Bar>()
  if (barData != null) return "FAIL: non-introspectable class should return null, got $barData"

  return "OK"
}
