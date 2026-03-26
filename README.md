# Pika

A Kotlin compiler plugin that generates complete type information at compile time, providing runtime access to generics, nullability, annotations, and more - without reflection.

## How It Works

Pika operates as a Kotlin IR (Intermediate Representation) compiler plugin:

1. During compilation, the plugin intercepts calls to `typeInfo<T>()` and `fullTypeInfo<T>()`
2. It analyzes the type argument `T` at compile time
3. The call is replaced with IR code that constructs the appropriate `TypeInfo` or `FullTypeInfo` object
4. At runtime, the function returns the pre-constructed type information with zero reflection overhead

## Installation

Add the Pika Gradle plugin to your project:

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
    id("io.github.lukmccall.pika") version "0.0.1-2.3.20"
}
```

The plugin automatically adds the required `pika-api` runtime dependency.

## API Reference

### typeInfo<T>()

Returns basic type metadata as a `TypeInfo` sealed class:

```kotlin
import io.github.lukmccall.pika.typeInfo

// Simple types
val stringType = typeInfo<String>()
// TypeInfo.Simple(fqName="kotlin.String", kClass=String::class, isNullable=false)

// Nullable types
val nullableInt = typeInfo<Int?>()
// TypeInfo.Simple(fqName="kotlin.Int", kClass=Int::class, isNullable=true)

// Generic types
val listType = typeInfo<List<String>>()
// TypeInfo.Parameterized(fqName="kotlin.collections.List", kClass=List::class,
//   typeArguments=[TypeInfo.Simple(...)], isNullable=false)

// Nested generics
val mapType = typeInfo<Map<String, List<Int?>>>()

// Star projections
val anyList = typeInfo<List<*>>()
// Contains TypeInfo.Star for the type argument
```

### fullTypeInfo<T>()

Returns comprehensive metadata including fields, inheritance, and annotations:

```kotlin
import io.github.lukmccall.pika.fullTypeInfo

annotation class Serializable(val name: String)

@Serializable(name = "user")
open class Base(val id: Int)

class User(
    id: Int,
    val name: String,
    var email: String?
) : Base(id)

val info = fullTypeInfo<User>()

// Access class information
info.fqName        // "com.example.User"
info.kClass        // User::class
info.isNullable    // false

// Access fields (declared on User, not inherited)
info.fields.forEach { field ->
    println("${field.name}: ${field.typeInfo}")
    println("  visibility: ${field.visibility}")
    println("  mutable: ${field.isMutable}")
    println("  annotations: ${field.annotations}")
}

// Access inheritance
info.baseClass     // FullTypeInfo for Base class
info.interfaces    // List of implemented interfaces

// Access annotations
info.annotations.forEach { annotation ->
    println("@${annotation.fqName}")
    println("  arguments: ${annotation.arguments}")
}
```

## Supported Kotlin Versions

Pika is tested against the following Kotlin versions:

| Kotlin Version | Plugin Version |
|----------------|----------------|
| 2.1.20         | 0.0.1-2.1.20   |
| 2.2.0          | 0.0.1-2.2.0    |
| 2.2.10         | 0.0.1-2.2.10   |
| 2.2.20         | 0.0.1-2.2.20   |
| 2.2.21         | 0.0.1-2.2.21   |
| 2.3.0          | 0.0.1-2.3.0    |
| 2.3.10         | 0.0.1-2.3.10   |
| 2.3.20         | 0.0.1-2.3.20   |

## Author

**Łukasz Kosmaty** ([@lukmccall](https://github.com/lukmccall))
