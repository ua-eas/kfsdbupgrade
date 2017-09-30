package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.lang.Runtime.getRuntime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects.ToStringHelper;

@JsonDeserialize(builder = Memory.Builder.class)
public final class Memory {

  private final long used;
  private final long free;
  private final long allocated;
  private final long max;

  private Memory(Builder builder) {
    this.used = builder.used;
    this.free = builder.free;
    this.allocated = builder.allocated;
    this.max = builder.max;
  }

  public static Memory build() {
    // Total amount of memory the JVM is allowed to use
    long max = getRuntime().maxMemory();

    // Total amount of memory currently allocated
    long allocated = getRuntime().totalMemory();

    // The JDK method "freeMemory()" reports what is free in the currently allocated heap
    // The amount of memory currently being used by the JVM is the difference between what has been allocated and what
    // is still free
    long used = allocated - getRuntime().freeMemory();

    // The true amount of free memory is the difference between max and what is currently being used
    long free = max - used;

    return builder().withAllocated(allocated).withFree(free).withMax(max).withUsed(used).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    // Total amount of memory the JVM is allowed to use
    private long max;

    // Total amount of memory currently allocated
    private long allocated;

    // The amount of memory currently being used by the JVM
    private long used;

    // The total amount of memory still available to the JVM
    private long free;

    public Builder withUsed(long used) {
      this.used = used;
      return this;
    }

    public Builder withFree(long free) {
      this.free = free;
      return this;
    }

    public Builder withAllocated(long allocated) {
      this.allocated = allocated;
      return this;
    }

    public Builder withMax(long max) {
      this.max = max;
      return this;
    }


    public Memory build() {
      return validate(new Memory(this));
    }

    private static Memory validate(Memory instance) {
      return instance;
    }

  }

  public long getUsed() {
    return used;
  }

  public long getFree() {
    return free;
  }

  public long getAllocated() {
    return allocated;
  }

  public long getMax() {
    return max;
  }

  @Override
  public String toString() {
    ToStringHelper helper = toStringHelper(this);
    helper.add("used", used);
    helper.add("free", free);
    helper.add("allocated", allocated);
    helper.add("max", max);
    return helper.toString();
  }
}
