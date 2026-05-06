package io.github.lukmccall.pika

public interface PIntrospectionProvider {
  public fun getIntrospectionData(): PIntrospectionData<*>
}
