package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.base.Functions.identity;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.alwaysTrue;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.reverse;
import static com.google.common.collect.Ordering.natural;
import static java.lang.Math.min;
import static java.lang.reflect.Array.newInstance;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Doubles;

public final class Lists {


  private Lists() {
  }

  public static <T> ImmutableList<T> sample(Iterable<T> iterable, int samples) {
    ImmutableList<T> shuffled = shuffle(iterable);
    if (shuffled.size() > samples) {
      return newList(shuffled.subList(0, samples));
    } else {
      return shuffled;
    }
  }

  public static <T> ImmutableList<T> upcast(Iterable<? extends T> iterable, Class<T> type) {
    List<T> list = newArrayList();
    for (T element : iterable) {
      list.add(element);
    }
    return newList(list);
  }

  public static <T> ImmutableList<T> newList(T[] array) {
    return nullSafeCopy(array);
  }

  public static <T> ImmutableList<T> newList() {
    return of();
  }

  public static <T> ImmutableList<T> newList(Iterable<T> iterable) {
    return ImmutableList.copyOf(iterable);
  }

  /**
   * Sort the elements from {@code iterable} according to the comparator
   */
  public static <T> ImmutableList<T> sort(Iterable<T> iterable, Comparator<T> comparator) {
    return Ordering.from(comparator).immutableSortedCopy(iterable);
  }

  /**
   * Sort the elements from {@code iterable} according to their natural order.
   */
  public static <C extends Comparable<? super C>> ImmutableList<C> sort(Iterable<C> iterable) {
    Function<C, C> function = identity();
    return sort(iterable, function);
  }

  /**
   * Sort the elements from {@code iterable} according to the natural ordering of the result of applying {@code function} to each element
   */
  public static <T, C extends Comparable<? super C>> ImmutableList<T> sort(Iterable<T> iterable, Function<T, C> function) {
    Predicate<T> predicate = alwaysTrue();
    return sort(iterable, predicate, function);
  }

  /**
   * Sort elements from {@code iterable} that match {@code predicate} according to their natural order.
   */
  public static <C extends Comparable<? super C>> ImmutableList<C> sort(Iterable<C> iterable, Predicate<C> predicate) {
    return sort(filter(iterable, predicate));
  }

  /**
   * Sort the elements from {@code iterable} that match {@code predicate} according to the natural ordering of the result of applying {@code function} to each element
   */
  public static <T, C extends Comparable<? super C>> ImmutableList<T> sort(Iterable<T> iterable, Predicate<T> predicate, Function<T, C> function) {
    return natural().onResultOf(function).immutableSortedCopy(filter(iterable, predicate));
  }

  public static <T> ImmutableList<T> nullSafeCopy(T[] elements) {
    if (elements == null) {
      return of();
    } else {
      return ImmutableList.copyOf(elements);
    }
  }

  public static <T> ImmutableList<T> nullSafeCopy(Iterable<T> elements) {
    if (elements == null) {
      return newList();
    } else {
      return newList(elements);
    }
  }

  public static <T> ImmutableList<T> nullSafeCast(Object[] objects, Class<T> type) {
    return cast(nullSafeCopy(objects), type);
  }

  public static <T> ImmutableList<T> cast(Iterable<?> iterable, Class<T> type) {
    List<T> list = newArrayList();
    for (Object element : iterable) {
      list.add(type.cast(element));
    }
    return newList(list);
  }

  public static String[] toStringArray(List<String> list) {
    return toArray(list, String.class);
  }

  public static <T> T[] toArray(List<T> list, Class<T> type) {
    @SuppressWarnings("unchecked")
    T[] array = (T[]) newInstance(type, list.size());
    return list.toArray(array);
  }

  /**
   * Return a transformed, immutable, copy of the elements from {@code array}
   */
  public static <F, T> ImmutableList<T> transform(F[] array, Function<? super F, ? extends T> function) {
    return newList(com.google.common.collect.Lists.transform(nullSafeCopy(array), function));
  }

  /**
   * Return a transformed, immutable, copy of the elements from {@code iterable}
   */
  public static <F, T> ImmutableList<T> transform(Iterable<F> iterable, Function<? super F, ? extends T> function) {
    return newList(com.google.common.collect.Lists.transform(newList(iterable), function));
  }

  /**
   * Filter and then transform {@code iterable} using {@code predicate} and {@code function}
   */
  public static <F, T> ImmutableList<T> transmute(Iterable<F> iterable, Predicate<F> predicate, Function<F, T> function) {
    return transform(filter(iterable, predicate), function);
  }

  /**
   * Filter {@code iterable} using {@code predicate}
   */
  public static <T> ImmutableList<T> filter(Iterable<T> iterable, Predicate<T> predicate) {
    return newList(com.google.common.collect.Iterables.filter(iterable, predicate));
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
   * Return an ImmutableList from {@code iterable}. The elements in the returned list are guaranteed to be non-blank
   * 
   * @throws IllegalArgumentException
   *           if any element in {@code iterable} is blank
   */
  public static ImmutableList<String> checkNoBlanks(Iterable<String> iterable) {
    List<String> list = newArrayList();
    int count = 0;
    for (String element : iterable) {
      checkArgument(isNotBlank(element), "element %s is blank", count++);
      list.add(element);
    }
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
    checkNotNull(list);
    return (list instanceof RandomAccess) ? new RandomAccessDistribution<T>(list, partitions) : new Distribution<T>(list, partitions);
  }

  /**
   * Creates a new, immutable, partitioned, list containing all of the elements from {@code list}. Elements are pseudo-evenly placed into sublists according to weight. The
   * algorithm used to distribute the elements by weight is greedy and non-perfect, but fast, and good enough for many real-life situations.
   *
   * <p>
   * Algorithm description:
   * <li>Calculate the weight of each element using the supplied function</li>
   * <li>Sort the elements in descending order by weight</li>
   * <li>Create {@code partitions} empty sublists</li>
   * <li>Add each element one at a time to the sublist with the smallest total weight</li>
   *
   * <p>
   * The size of each partition can vary (and likely will vary in real world situations)
   *
   * <p>
   * The ordering from the original {@code list} is not considered when producing the distribution.
   *
   * <p>
   * The distribution is an immutable copy. Changes to the original {@code list} have no affect on the distribution.
   */
  public static <T, N extends Number> List<List<T>> scatter(Iterable<T> iterable, int partitions, Function<T, N> weigher) {
    checkNotNull(iterable, "iterable");
    checkNotNull(weigher, "weigher");
    List<Weighed<T, N>> weighed = reverse(sort(weighElements(iterable, weigher)));
    List<List<T>> container = newContainer(min(weighed.size(), partitions));
    if (weighed.isEmpty()) {
      return immutableCopy(container);
    } else {
      fillContainer(container, weighed);
      return immutableCopy(container);
    }
  }

  public static <T> ImmutableList<T> concat(Iterable<? extends Collection<T>> container) {
    List<T> list = newArrayList();
    for (Collection<T> collection : container) {
      list.addAll(collection);
    }
    return newList(list);
  }

  /**
   * @deprecated Use concat(container) instead
   */
  @Deprecated
  public static <T> ImmutableList<T> consolidate(Iterable<? extends Collection<T>> container) {
    return concat(container);
  }

  private static <T> List<List<T>> newContainer(int size) {
    List<List<T>> container = newArrayList();
    for (int i = 0; i < size; i++) {
      List<T> list = newArrayList();
      container.add(list);
    }
    return container;
  }

  private static <T, N extends Number> void fillContainer(List<List<T>> container, List<Weighed<T, N>> sorted) {
    List<WeightIndex> weightIndexes = newArrayList();
    for (int i = 0; i < container.size(); i++) {
      weightIndexes.add(new WeightIndex(i));
    }
    Ordering<WeightIndex> ordering = Ordering.from(WeightIndexComparator.INSTANCE);
    WeightIndex min = ordering.min(weightIndexes);
    for (Weighed<T, N> element : sorted) {
      int index = min.getIndex();
      List<T> list = container.get(index);
      T value = element.getElement();
      list.add(value);
      double weight = min.getWeight() + element.getWeight().doubleValue();
      min.setWeight(weight);
      min = ordering.min(weightIndexes);
    }
  }

  private static <T> List<List<T>> immutableCopy(List<List<T>> container) {
    List<List<T>> list = newArrayList();
    for (List<T> element : container) {
      list.add(newList(element));
    }
    return newList(list);
  }

  private static <T, N extends Number> List<Weighed<T, N>> weighElements(Iterable<T> list, Function<T, N> weigher) {
    List<Weighed<T, N>> weighted = newArrayList();
    for (T element : list) {
      N weight = weigher.apply(element);
      Weighed<T, N> weighed = new Weighed<T, N>(element, weight);
      weighted.add(weighed);
    }
    return weighted;
  }

  private static class Weighed<T, N extends Number> implements Comparable<Weighed<T, N>> {

    public Weighed(T element, N weight) {
      this.element = checkNotNull(element);
      this.weight = weight;
    }

    private final T element;
    private final N weight;

    @Override
    public int compareTo(Weighed<T, N> other) {
      return Doubles.compare(weight.doubleValue(), other.getWeight().doubleValue());
    }

    public T getElement() {
      return element;
    }

    public Number getWeight() {
      return weight;
    }

  }

  private enum WeightIndexComparator implements Comparator<WeightIndex> {
    INSTANCE;

    @Override
    public int compare(WeightIndex one, WeightIndex two) {
      return Double.compare(one.getWeight(), two.getWeight());
    }

  }

  private static class WeightIndex {

    public WeightIndex(int index) {
      this.index = checkElementIndex(index, Integer.MAX_VALUE);
    }

    private final int index;
    private double weight;

    public int getIndex() {
      return index;
    }

    public double getWeight() {
      return weight;
    }

    public void setWeight(double weight) {
      this.weight = weight;
    }

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
