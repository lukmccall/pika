// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Foo(val x: Int)

inline fun <reified T> descriptor() = typeDescriptorOf<T>()

fun box(): String {
  // Reified inline proxy with introspectable type
  val fooDescriptor = descriptor<Foo>()
  if (fooDescriptor !is PTypeDescriptor.Concrete) return "FAIL: Foo should be Concrete"
  if (fooDescriptor.introspectionData == null) return "FAIL: Foo introspectionData should not be null"

  val data = fooDescriptor.introspectionData!!
  if (data.kClass != Foo::class) return "FAIL: introspectionData.kClass should be Foo"
  if (data.properties.size != 1) return "FAIL: should have 1 property, got ${data.properties.size}"

  // Cross-check with introspectionOf
  val direct = introspectionOf<Foo>()
  if (data.kClass != direct.kClass) return "FAIL: kClass mismatch"
  if (data.properties.size != direct.properties.size) return "FAIL: properties size mismatch"

  // Reified inline proxy with non-introspectable type
  val stringDescriptor = descriptor<String>()
  if (stringDescriptor !is PTypeDescriptor.Concrete) return "FAIL: String should be Concrete"
  if (stringDescriptor.introspectionData != null) return "FAIL: String introspectionData should be null"

  return "OK"
}
