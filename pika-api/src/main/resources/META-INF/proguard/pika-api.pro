# ClassValue is guarded at runtime via try-catch and not used on Android < 34.
# See CacheByClass.kt
-dontwarn java.lang.ClassValue
