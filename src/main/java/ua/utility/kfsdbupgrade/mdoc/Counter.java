package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkArgument;

public final class Counter {

  public Counter() {
    this(0);
  }

  public Counter(long value) {
    checkArgument(value >= 0, "value can't be negative");
    this.value = value;
  }

  private long value;

  public synchronized long increment() {
    return increment(1);
  }

  public synchronized long increment(long amount) {
    checkArgument(value < Long.MAX_VALUE - amount, "can only count to %s", Long.MAX_VALUE);
    return value += amount;
  }

  public synchronized long getValue() {
    return value;
  }

}
