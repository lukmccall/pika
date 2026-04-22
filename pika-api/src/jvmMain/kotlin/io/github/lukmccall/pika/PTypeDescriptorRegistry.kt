package io.github.lukmccall.pika

import java.util.concurrent.ConcurrentHashMap

public object PTypeDescriptorRegistry {
  // Fast path for non-parameterized, non-introspectable types.
  // Pure ClassValue lookups - same structure as Kotlin's CACHE_FOR_BASE_CLASSIFIERS in typeOf.
  // The JIT can fold these to direct object references after warmup.
  private val concreteNonNullableCache = object : ClassValue<PTypeDescriptor.Concrete>() {
    override fun computeValue(type: Class<*>) =
      PTypeDescriptor.Concrete(PType(type), false, null)
  }

  private val concreteNullableCache = object : ClassValue<PTypeDescriptor.Concrete>() {
    override fun computeValue(type: Class<*>) =
      PTypeDescriptor.Concrete(PType(type), true, null)
  }

  // Slow path for non-parameterized types with introspection data.
  private data class ConcreteKey(val isNullable: Boolean, val introspection: PIntrospectionData<*>?)

  private val concreteWithIntrospectionCache =
    object : ClassValue<ConcurrentHashMap<ConcreteKey, PTypeDescriptor.Concrete>>() {
      override fun computeValue(type: Class<*>) =
        ConcurrentHashMap<ConcreteKey, PTypeDescriptor.Concrete>()
    }

  private data class ParameterizedKey(
    val isNullable: Boolean,
    val parameters: List<PTypeDescriptor>,
    val introspection: PIntrospectionData<*>?
  )

  private val parameterizedCache =
    object : ClassValue<ConcurrentHashMap<ParameterizedKey, PTypeDescriptor.Concrete.Parameterized>>() {
      override fun computeValue(type: Class<*>) =
        ConcurrentHashMap<ParameterizedKey, PTypeDescriptor.Concrete.Parameterized>()
    }

  @JvmStatic
  public fun getOrCreateConcrete(
    jClass: Class<*>,
    isNullable: Boolean,
    introspection: PIntrospectionData<*>?
  ): PTypeDescriptor.Concrete =
    if (introspection == null) {
      if (isNullable) {
        concreteNullableCache.get(jClass)
      } else {
        concreteNonNullableCache.get(jClass)
      }
    } else {
      val key = ConcreteKey(isNullable, introspection)
      val map = concreteWithIntrospectionCache.get(jClass)

      return map.getOrPut(key) {
        PTypeDescriptor.Concrete(PType(jClass), isNullable, introspection)
      }
    }

  @JvmStatic
  public fun getOrCreateParameterized(
    jClass: Class<*>,
    isNullable: Boolean,
    parameters: List<PTypeDescriptor>,
    introspection: PIntrospectionData<*>?
  ): PTypeDescriptor.Concrete.Parameterized {
    val key = ParameterizedKey(isNullable, parameters, introspection)
    val map = parameterizedCache.get(jClass)

    return map.getOrPut(key) {
      PTypeDescriptor.Concrete.Parameterized(PType(jClass), isNullable, parameters, introspection)
    }
  }
}
