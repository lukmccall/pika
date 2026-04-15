# Pika

A Kotlin compiler plugin that generates complete type information at compile time, providing runtime access to generics, nullability, annotations, properties and more — without reflection.

## How It Works

Pika operates as a Kotlin IR (Intermediate Representation) compiler plugin:

1. During compilation, the plugin intercepts calls to `typeDescriptorOf<T>()`, `introspectionOf<T>()`, and `isIntrospectable<T>()`.
2. It analyzes the type argument `T` at compile time.
3. Each call is replaced with IR code that constructs the appropriate descriptor/introspection object (or a constant, for `isIntrospectable`).
4. At runtime, the function returns the pre-constructed data with zero reflection overhead.

Classes annotated with `@Introspectable` also get a synthetic `__PIntrospectionData()` companion function generated at compile time, which carries the full property/annotation/function metadata.

## Installation

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.3.20"
    id("io.github.lukmccall.pika") version "0.1.3-2.3.20"
}
```

The Gradle plugin automatically adds the required `pika-api` runtime dependency.

## API Reference

### `typeDescriptorOf<T>()`

Returns a `PTypeDescriptor` for `T`. The descriptor is a sealed hierarchy:

- `PTypeDescriptor.Concrete` — a non-star type (has `pType: PType`, `isNullable: Boolean`, and optional `introspection: PIntrospectionData<*>?`)
- `PTypeDescriptor.Concrete.Parameterized` — a generic type, adds `parameters: List<PTypeDescriptor>`
- `PTypeDescriptor.Star` — a `*` projection

```kotlin
import io.github.lukmccall.pika.*

// Simple types
typeDescriptorOf<String>()        // Concrete(pType=PType(String), isNullable=false)
typeDescriptorOf<Int?>()          // Concrete(pType=PType(Int), isNullable=true)

// Parameterized types
typeDescriptorOf<List<String>>()          // Concrete.Parameterized(List, [Concrete(String)])
typeDescriptorOf<Map<String, List<Int?>>>()

// Star projections
typeDescriptorOf<List<*>>()       // parameters = [Star]

// Class types (user-defined)
typeDescriptorOf<User>()
typeDescriptorOf<User?>()
```

`PType` exposes `jClass: Class<*>` and `kClass: KClass<*>` (computed as `jClass.kotlin`).

`typeDescriptorOf<T>()` works inside `inline fun <reified T>` chains — the plugin rewrites the call at each inlined call site. It is a runtime error to call it with a non-reified generic type parameter.

### `@Introspectable` and `introspectionOf<T>()`

Mark a class with `@Introspectable` to generate property/annotation/function metadata for it. Retrieve it via `introspectionOf<T>()`:

```kotlin
@Introspectable
class Address(val city: String, val country: String)

@Introspectable
open class Person(val name: String, val address: Address)

val data: PIntrospectionData<Person> = introspectionOf<Person>()

data.jClass                  // Class<Person>
data.annotations             // List<PAnnotation>
data.properties              // List<PProperty<Person, *>>
data.functions               // List<PFunction>
data.baseClass               // PIntrospectionData<*>? — parent's data, if also @Introspectable

for (prop in data.properties) {
  prop.name                  // "name" / "address"
  prop.visibility            // PVisibility.PUBLIC / PRIVATE / PROTECTED / INTERNAL
  prop.type                  // PTypeDescriptor
  prop.annotations           // List<PAnnotation>
  prop.isMutable             // var vs val
  prop.hasBackingField       // false for computed `val x get() = ...`
  prop.getter(person)        // read the value
  prop.setter?.invoke(person, value)   // non-null if there is a backing field
  prop.isDelegated           // true for `val x by lazy { ... }`
  prop.delegateGetter?.invoke(person)  // non-null when delegated — returns e.g. the Lazy<T>
}
```

You can also call the companion function directly:

```kotlin
val data = Person.__PIntrospectionData()
```

#### Inheritance

`baseClass` links to the parent's introspection data when the parent is also `@Introspectable`:

```kotlin
@Introspectable open class Animal(val species: String)
@Introspectable class Dog(species: String, val breed: String) : Animal(species)

val data = introspectionOf<Dog>()
data.properties.map { it.name }             // ["breed"] — only declared on Dog
data.baseClass?.properties?.map { it.name } // ["species"] — from Animal
```

#### Annotations

```kotlin
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class MyAnnotation(val value: String)

@Introspectable
@MyAnnotation("class level")
class Tagged(
  @MyAnnotation("property level") val name: String
)

val data = introspectionOf<Tagged>()
data.annotations.first { it.jClass == MyAnnotation::class.java }.arguments["value"]
// "class level"
```

`PAnnotation.arguments` supports primitives, strings, enums, and `Class` values.

#### Delegated properties

```kotlin
@Introspectable
class Example {
  val lazyValue by lazy { compute() }
  val regular: String = "hi"
  val computed: Int get() = 42
}

val data = introspectionOf<Example>()
val lazyProp = data.properties.first { it.name == "lazyValue" }
lazyProp.isDelegated                     // true
val delegate = lazyProp.delegateGetter!!(instance) as Lazy<*>
delegate.isInitialized()                 // inspect without triggering init
```

### `isIntrospectable<T>()`

Compile-time boolean — replaced with a constant `true`/`false`:

```kotlin
@Introspectable class Yes
class No

isIntrospectable<Yes>()   // true
isIntrospectable<No>()    // false
```

### Introspection inside type descriptors

`typeDescriptorOf<T>()` also carries introspection when available:

```kotlin
val d = typeDescriptorOf<Person>() as PTypeDescriptor.Concrete
d.introspection?.jClass     // Class<Person>

// Nested — the List itself is not introspectable, but its argument is:
val list = typeDescriptorOf<List<Person>>() as PTypeDescriptor.Concrete.Parameterized
list.introspection                                        // null
(list.parameters[0] as PTypeDescriptor.Concrete).introspection?.jClass  // Class<Person>
```

## Supported Kotlin Versions

Pika is tested against the following Kotlin versions. The plugin coordinate is `io.github.lukmccall.pika:<pika-version>-<kotlin-version>`.

| Kotlin Version | Plugin Version  |
|----------------|-----------------|
| 2.1.20         | 0.1.3-2.1.20    |
| 2.2.0          | 0.1.3-2.2.0     |
| 2.2.10         | 0.1.3-2.2.10    |
| 2.2.20         | 0.1.3-2.2.20    |
| 2.2.21         | 0.1.3-2.2.21    |
| 2.3.0          | 0.1.3-2.3.0     |
| 2.3.10         | 0.1.3-2.3.10    |
| 2.3.20         | 0.1.3-2.3.20    |

## Author

**Łukasz Kosmaty** ([@lukmccall](https://github.com/lukmccall))
