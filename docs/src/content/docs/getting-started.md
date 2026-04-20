---
title: Getting Started
description: Installation and basic usage of the Pika Kotlin compiler plugin.
---

## Installation

```kotlin title="settings.gradle.kts"
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

```kotlin title="build.gradle.kts"
plugins {
    kotlin("jvm") version "<kotlin-version>"
    id("io.github.lukmccall.pika") version "<pika-version>-<kotlin-version>"
}
```

The Gradle plugin automatically adds the required `pika-api` runtime dependency.

For the latest version and supported Kotlin versions, see the [GitHub repository](https://github.com/lukmccall/pika).

## Basic Usage

### Type Descriptors

Use `typeDescriptorOf<T>()` to get a complete type descriptor at runtime, with full generic information and zero reflection:

```kotlin
val descriptor = typeDescriptorOf<List<String>>()
// descriptor.pType.jClass    == List::class.java
// descriptor.parameters[0]   is Concrete(String, nullable=false)
```

### Introspection

Annotate classes with `@Introspectable` to enable compile-time introspection:

```kotlin
@Introspectable
class Person(val name: String, var age: Int)

val data = introspectionOf<Person>()
data.properties.map { it.name } // ["name", "age"]
```

### Checking Introspectability

```kotlin
isIntrospectable<Person>()  // true
isIntrospectable<String>()  // false
```
