package io.github.lukmccall.pika

import java.util.concurrent.ConcurrentHashMap

private val useClassValue = runCatching {
  Class.forName("java.lang.ClassValue")
}.map { true }.getOrDefault(false)

internal abstract class CacheByClass<V> {
  abstract fun get(key: Class<*>): V
}

internal fun <V : Any> createCache(compute: (Class<*>) -> V): CacheByClass<V> {
  return if (useClassValue) {
    ClassValueCache(compute)
  } else {
    ConcurrentHashMapCache(compute)
  }
}

private class ClassValueCache<V>(compute: (Class<*>) -> V) : CacheByClass<V>() {
  private val classValue = object : ClassValue<V>() {
    override fun computeValue(type: Class<*>): V = compute(type)
  }

  override fun get(key: Class<*>): V = classValue[key]
}

private class ConcurrentHashMapCache<V : Any>(private val compute: (Class<*>) -> V) : CacheByClass<V>() {
  private val cache = ConcurrentHashMap<Class<*>, V>()

  override fun get(key: Class<*>): V = cache.getOrPut(key) { compute(key) }
}
