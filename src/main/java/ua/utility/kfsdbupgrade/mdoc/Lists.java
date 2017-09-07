package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Lists.newArrayList;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

import com.google.common.collect.ImmutableList;

public final class Lists {

  private Lists() {}

  public static <T> ImmutableList<T> sample(Iterable<T> iterable, int samples) {
    ImmutableList<T> shuffled = shuffle(iterable);
    if (shuffled.size() > samples) {
      return newList(shuffled.subList(0, samples));
    } else {
      return shuffled;
    }
  }

  public static <T> ImmutableList<T> newList() {
    return of();
  }

  public static <T> ImmutableList<T> newList(Iterable<T> iterable) {
    return ImmutableList.copyOf(iterable);
  }

  /**
   * Return an immutable shuffled list containing the elements from iterable.
   */
  public static <T> ImmutableList<T> shuffle(Iterable<T> iterable) {
    List<T> list = newArrayList(iterable);
    java.util.Collections.shuffle(list);
    return newList(list);
  }

  /**
   * Returns consecutive {@linkplain List#subList(int, int) sublists} of a list, distributing the elements of the list as evenly as possible into the specified number of
   * partitions. For example, distributing a list containing {@code [a, b, c, d, e]} into 3 partitions yields {@code [[a, b], [c, d], [e]]} -- an outer list containing three inner
   * lists of one and two elements, all in the original order. The sizes of the returned lists will differ by at most one from each other.
   *
   * <p>
   * The outer list is unmodifiable, but reflects the latest state of the source list. The inner lists are sublist views of the original list, produced on demand using
   * {@link List#subList(int, int)}, and are subject to all the usual caveats about modification as explained in that API.
   */
  public static <T> List<List<T>> distribute(List<T> list, int partitions) {
    return (list instanceof RandomAccess) ? new RandomAccessDistribution<T>(list, partitions) : new Distribution<T>(list, partitions);
  }

  private static class Distribution<T> extends AbstractList<List<T>> {
    final List<T> list;
    final int partitions;

    Distribution(List<T> list, int partitions) {
      this.list = list;
      this.partitions = partitions;
    }

    @Override
    public List<T> get(int index) {
      checkElementIndex(index, size());
      int listSize = list.size();
      int normalPartitions = listSize % partitions;
      int partialPartitionSize = listSize / partitions;
      int normalPartitionSize = partialPartitionSize + 1;

      // Parts [0, normalPartitions) have size normalPartitionSize, the rest
      // have size partialPartitionSize.
      if (index < normalPartitions) {
        int chunkStart = normalPartitionSize * index;
        return list.subList(chunkStart, chunkStart + normalPartitionSize);
      } else {
        int normalEnd = normalPartitions * normalPartitionSize;
        int chunkStart = normalEnd + (index - normalPartitions) * partialPartitionSize;
        return list.subList(chunkStart, chunkStart + partialPartitionSize);
      }
    }

    @Override
    public int size() {
      return partitions;
    }
  }

  private static class RandomAccessDistribution<T> extends Distribution<T> implements RandomAccess {
    public RandomAccessDistribution(List<T> list, int parts) {
      super(list, parts);
    }
  }

}
