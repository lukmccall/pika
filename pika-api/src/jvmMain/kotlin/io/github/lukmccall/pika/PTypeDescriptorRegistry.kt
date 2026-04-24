package io.github.lukmccall.pika

import java.util.concurrent.ConcurrentHashMap

public object PTypeDescriptorRegistry {
  @JvmField public val INT: PTypeDescriptor.Concrete = PTypeDescriptor.Concrete(PType(Int::class.java), false, null)
  @JvmField public val INT_NULLABLE: PTypeDescriptor.Concrete = PTypeDescriptor.Concrete(PType(Int::class.java), true, null)
  @JvmField public val LONG: PTypeDescriptor.Concrete = PTypeDescriptor.Concrete(PType(Long::class.java), false, null)
  @JvmField public val LONG_NULLABLE: PTypeDescriptor.Concrete = PTypeDescriptor.Concrete(PType(Long::class.java), true, null)
  @JvmField public val FLOAT: PTypeDescriptor.Concrete = PTypeDescriptor.Concrete(PType(Float::class.java), false, null)
  @JvmField public val FLOAT_NULLABLE: PTypeDescriptor.Concrete = PTypeDescriptor.Concrete(PType(Float::class.java), true, null)
  @JvmField public val SHORT: PTypeDescriptor.Concrete = PTypeDescriptor.Concrete(PType(Short::class.java), false, null)
  @JvmField public val SHORT_NULLABLE: PTypeDescriptor.Concrete = PTypeDescriptor.Concrete(PType(Short::class.java), true, null)
  @JvmField public val DOUBLE: PTypeDescriptor.Concrete = PTypeDescriptor.Concrete(PType(Double::class.java), false, null)
  @JvmField public val DOUBLE_NULLABLE: PTypeDescriptor.Concrete = PTypeDescriptor.Concrete(PType(Double::class.java), true, null)
  @JvmField public val STRING: PTypeDescriptor.Concrete = PTypeDescriptor.Concrete(PType(String::class.java), false, null)
  @JvmField public val STRING_NULLABLE: PTypeDescriptor.Concrete = PTypeDescriptor.Concrete(PType(String::class.java), true, null)
  @JvmField public val BOOLEAN: PTypeDescriptor.Concrete = PTypeDescriptor.Concrete(PType(Boolean::class.java), false, null)
  @JvmField public val BOOLEAN_NULLABLE: PTypeDescriptor.Concrete = PTypeDescriptor.Concrete(PType(Boolean::class.java), true, null)

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
  ): PTypeDescriptor.Concrete {
    return if (introspection == null) {
      if (isNullable) {
        concreteNullableCache.get(jClass)
      } else {
        concreteNonNullableCache.get(jClass)
      }
    } else {
      val key = ConcreteKey(isNullable, introspection)
      val map = concreteWithIntrospectionCache.get(jClass)

      map.getOrPut(key) {
        PTypeDescriptor.Concrete(PType(jClass), isNullable, introspection)
      }
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
