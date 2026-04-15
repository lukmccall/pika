// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
open class Parent(val a: Int)

@Introspectable
class Child(val b: Int) : Parent(0)

@Introspectable
object Singleton {
  val version: String = "1.0"
}

fun box(): String {
  val a1 = introspectionOf<Parent>()
  val a2 = introspectionOf<Parent>()
  if (a1 !== a2) return "FAIL: introspectionOf<Parent>() not cached"

  val c1 = Parent.__PIntrospectionData()
  val c2 = Parent.__PIntrospectionData()
  if (c1 !== c2) return "FAIL: Parent.__PIntrospectionData() not cached"

  if (a1 !== c1) return "FAIL: introspectionOf<Parent>() and Parent.__PIntrospectionData() differ"

  val s1 = Singleton.__PIntrospectionData()
  val s2 = Singleton.__PIntrospectionData()
  if (s1 !== s2) return "FAIL: Singleton.__PIntrospectionData() not cached"

  val ch1 = introspectionOf<Child>()
  val ch2 = introspectionOf<Child>()
  if (ch1 !== ch2) return "FAIL: introspectionOf<Child>() not cached"

  val base1 = ch1.baseClass
  val base2 = ch2.baseClass
  if (base1 !== base2) return "FAIL: Child.baseClass not stable across calls"
  if (base1 !== a1) return "FAIL: Child.baseClass is not the cached Parent instance"

  return "OK"
}
