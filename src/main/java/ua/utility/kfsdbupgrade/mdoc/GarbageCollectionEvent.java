package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.management.ManagementFactory.getGarbageCollectorMXBeans;

import java.lang.management.GarbageCollectorMXBean;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public final class GarbageCollectionEvent {

  private final String name;
  private final Optional<Long> count;
  private final Optional<Long> millis;

  private GarbageCollectionEvent(Builder builder) {
    this.name = builder.name;
    this.count = builder.count;
    this.millis = builder.millis;
  }

  public static List<GarbageCollectionEvent> buildList() {
    List<GarbageCollectionEvent> events = newArrayList();
    for (GarbageCollectorMXBean bean : getGarbageCollectorMXBeans()) {
      GarbageCollectionEvent event = copyOf(bean);
      events.add(event);
    }
    return ImmutableList.copyOf(events);
  }

  public static GarbageCollectionEvent copyOf(GarbageCollectorMXBean bean) {
    Builder builder = builder();
    builder.withName(bean.getName());
    if (bean.getCollectionCount() >= 0) {
      builder.withCount(of(bean.getCollectionCount()));
    }
    if (bean.getCollectionTime() >= 0) {
      builder.withCount(of(bean.getCollectionTime()));
    }
    return builder.build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String name;
    private Optional<Long> count = absent();
    private Optional<Long> millis = absent();

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withCount(Optional<Long> count) {
      this.count = count;
      return this;
    }

    public Builder withMillis(Optional<Long> millis) {
      this.millis = millis;
      return this;
    }

    public GarbageCollectionEvent build() {
      return validate(new GarbageCollectionEvent(this));
    }

    public static GarbageCollectionEvent validate(GarbageCollectionEvent instance) {
      checkNotNull(instance.name, "name can't be null");
      checkNotNull(instance.count, "count can't be null");
      checkNotNull(instance.millis, "millis can't be null");
      return instance;
    }
  }

  public String getName() {
    return name;
  }

  public Optional<Long> getCount() {
    return count;
  }

  public Optional<Long> getMillis() {
    return millis;
  }

  @Override
  public String toString() {
    return toStringHelper(this).add("name", name).add("count", count.orNull()).add("millis", millis.orNull()).toString();
  }
}
