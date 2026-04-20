---
title: Pika
description: A Kotlin compiler plugin that generates complete type information at compile time.
template: splash
hero:
  title: Pika
  tagline: A Kotlin compiler plugin that generates complete type information at compile time, providing runtime access to generics, nullability, annotations, properties and more — without reflection.
  actions:
    - text: Getting Started
      link: /pika/getting-started/
      icon: right-arrow
    - text: GitHub
      link: https://github.com/lukmccall/pika
      icon: external
      variant: minimal
---

## How It Works

Pika operates as a Kotlin IR (Intermediate Representation) compiler plugin:

1. During compilation, the plugin intercepts calls to `typeDescriptorOf<T>()`, `introspectionOf<T>()`, and `isIntrospectable<T>()`.
2. It analyzes the type argument `T` at compile time.
3. Each call is replaced with IR code that constructs the appropriate descriptor/introspection object (or a constant, for `isIntrospectable`).
4. At runtime, the function returns the pre-constructed data with zero reflection overhead.

Classes annotated with `@Introspectable` also get a synthetic `__PIntrospectionData()` companion function generated at compile time, which carries the full property/annotation/function metadata.

## Quick Example

```kotlin
val descriptor = typeDescriptorOf<Map<String, List<Int?>>>()

// Full generic type tree — no reflection needed
descriptor.parameters[0]  // Concrete(String, nullable=false)
descriptor.parameters[1]  // Parameterized(List, [Concrete(Int, nullable=true)])
```
