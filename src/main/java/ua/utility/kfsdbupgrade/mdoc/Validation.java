package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.collect.ImmutableMultimap.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static ua.utility.kfsdbupgrade.mdoc.Exceptions.illegalArgument;
import static ua.utility.kfsdbupgrade.mdoc.Exceptions.illegalState;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;

public final class Validation {

  private Validation() {
  }

  private static final String UNSUPPORTED_NUMBER_TYPE = "%s is not a supported number type [%s]";

  /**
   * No nulls
   */
  public static <T> T checkNoNulls(T instance) {
    Multimap<Field, String> violations = ArrayListMultimap.create();
    for (Field field : Reflection.getFields(instance.getClass())) {
      checkNotNull(instance, field, violations);
    }
    return checkViolations(instance, violations);
  }

  /**
   * No negative numbers
   */
  public static <T> T checkNoNegatives(T instance) {
    Multimap<Field, String> violations = ArrayListMultimap.create();
    for (Field field : Reflection.getFields(instance.getClass())) {
      Object value = checkNotNull(instance, field, violations);
      if (value != null) {
        handleNegatives(instance, field, value, violations);
        handleOptionalNegative(instance, field, value, violations);
      }
    }
    return checkViolations(instance, violations);
  }

  /**
   * All numbers must be greater than zero
   */
  public static <T> T checkAllPositives(T instance) {
    Multimap<Field, String> violations = getAllPositivesViolations(instance, true);
    return checkViolations(instance, violations);
  }

  public static <T> ImmutableMultimap<Field, String> getAllPositivesViolations(T instance, boolean includeNullViolations) {
    Multimap<Field, String> violations = ArrayListMultimap.create();
    for (Field field : Reflection.getFields(instance.getClass())) {
      Object value = null;
      if (includeNullViolations) {
        value = checkNotNull(instance, field, violations);
      } else {
        value = getValue(field, instance);
      }
      if (value != null) {
        handleNegatives(instance, field, value, violations);
        handleOptionalNegative(instance, field, value, violations);
        handleZeros(instance, field, value, violations);
        handleOptionalZero(instance, field, value, violations);
      }
    }
    return copyOf(violations);
  }

  private static void handleOptionalNegative(Object instance, Field field, Object value, Multimap<Field, String> violations) {
    if (isOptionalNumber(field, value)) {
      Optional<?> optional = (Optional<?>) value;
      Number number = (Number) optional.get();
      handleNegatives(instance, field, number, violations);
    }
  }

  private static void handleOptionalZero(Object instance, Field field, Object value, Multimap<Field, String> violations) {
    if (isOptionalNumber(field, value)) {
      Optional<?> optional = (Optional<?>) value;
      Number number = (Number) optional.get();
      handleZeros(instance, field, number, violations);
    }
  }

  private static <T> T handleZeros(T instance, Field field, Object value, Multimap<Field, String> violations) {
    if (!(value instanceof Number)) {
      return instance;
    }
    if (field.getAnnotation(IgnoreZeros.class) != null) {
      return instance;
    }
    Number number = (Number) value;
    if (isZero(number)) {
      violations.put(field, noZeros(instance, field));
    }
    return instance;
  }

  private static <T> T handleNegatives(T instance, Field field, Object value, Multimap<Field, String> violations) {
    if (!(value instanceof Number)) {
      return instance;
    }
    if (field.getAnnotation(IgnoreNegatives.class) != null) {
      return instance;
    }
    Number number = (Number) value;
    if (isNegative(number)) {
      violations.put(field, noNegatives(instance, field));
    }
    return instance;
  }

  private static boolean useIntValue(Number number) {
    if (number instanceof Byte) {
      return true;
    } else if (number instanceof Short) {
      return true;
    } else if (number instanceof Integer) {
      return true;
    } else {
      return false;
    }
  }

  private static boolean isZero(Number number) {
    if (useIntValue(number)) {
      return number.intValue() == 0;
    } else if (number instanceof Long) {
      return number.longValue() == 0;
    } else if ((number instanceof Float) || (number instanceof Double)) {
      return number.doubleValue() == 0;
    } else {
      throw unsupportedType(number);
    }
  }

  private static boolean isNegative(Number number) {
    if (useIntValue(number)) {
      return number.intValue() < 0;
    } else if (number instanceof Long) {
      return number.longValue() < 0;
    } else if ((number instanceof Float) || (number instanceof Double)) {
      return number.doubleValue() < 0;
    } else {
      throw unsupportedType(number);
    }
  }

  private static IllegalArgumentException unsupportedType(Number number) {
    return illegalArgument(UNSUPPORTED_NUMBER_TYPE, number.getClass().getSimpleName(), number);
  }

  private static <T> String noNegatives(T instance, Field field) {
    return format("%s.%s cannot be negative", instance.getClass().getSimpleName(), field.getName());
  }

  private static <T> String noZeros(T instance, Field field) {
    return format("%s.%s cannot be zero", instance.getClass().getSimpleName(), field.getName());
  }

  /**
   * No nulls and no blank strings
   */
  public static <T> T checkNoBlanks(T instance) {
    Multimap<Field, String> violations = getNoBlanksViolations(instance);
    return checkViolations(instance, violations);
  }

  public static <T> ImmutableMultimap<Field, String> getNoBlanksViolations(T instance) {
    Multimap<Field, String> violations = ArrayListMultimap.create();
    for (Field field : Reflection.getFields(instance.getClass())) {
      Object value = checkNotNull(instance, field, violations);
      if (value != null) {
        checkNotBlankString(instance, field, value, violations);
        checkNotBlankOptionalString(instance, field, value, violations);
      }
    }
    return copyOf(violations);
  }

  public static <T> T checkViolations(T instance, Multimap<Field, String> violations) {
    if (violations.isEmpty()) {
      return instance;
    } else {
      throw illegalArgument(buildErrorMessage(instance, violations));
    }
  }

  private static <T> T checkNotBlankString(T instance, Field field, Object value, Multimap<Field, String> violations) {
    if (field.getAnnotation(IgnoreBlanks.class) != null) {
      return instance;
    }
    if (!field.getType().equals(String.class)) {
      return instance;
    }
    if (isBlank(value.toString())) {
      violations.put(field, noBlanks(instance, field));
    }
    return instance;
  }

  private static boolean isOptionalNumber(Field field, Object value) {
    if (Optional.class.isAssignableFrom(field.getType())) {
      Optional<?> optional = (Optional<?>) value;
      Object contained = optional.orNull();
      return (contained != null) && (contained instanceof Number);
    } else {
      return false;
    }
  }

  private static <T> T checkNotBlankOptionalString(T instance, Field field, Object value, Multimap<Field, String> violations) {
    if (field.getAnnotation(IgnoreBlanks.class) != null) {
      return instance;
    }
    if (Optional.class.isAssignableFrom(field.getType())) {
      Optional<?> optional = (Optional<?>) value;
      Object contained = optional.orNull();
      if (contained != null && contained instanceof String && isBlank(contained.toString())) {
        violations.put(field, noBlanks(instance, field));
      }
    }
    return instance;
  }

  private static <T> String noBlanks(T instance, Field field) {
    return format("%s.%s cannot be blank", instance.getClass().getSimpleName(), field.getName());
  }

  private static <T> Object checkNotNull(T instance, Field field, Multimap<Field, String> violations) {
    Object value = getValue(field, instance);
    if (value == null) {
      violations.put(field, format("%s.%s cannot be null", instance.getClass().getSimpleName(), field.getName()));
    }
    return value;
  }

  /**
   * Unconditionally attempt to get the value of this field on this instance.
   */
  private static <T> Object getValue(Field field, T instance) {
    try {
      return field.get(instance);
    } catch (IllegalAccessException e) {
      throw illegalState(e);
    }
  }

  private static Comparator<Field> fieldNameComparator() {
    return FieldNameComparator.INSTANCE;
  }

  private enum FieldNameComparator implements Comparator<Field> {
    INSTANCE;

    @Override
    public int compare(Field one, Field two) {
      ComparisonChain chain = ComparisonChain.start();
      chain = chain.compare(one.getDeclaringClass().getCanonicalName(), two.getDeclaringClass().getCanonicalName());
      chain = chain.compare(one.getName(), two.getName());
      chain = chain.compare(one.getType().getCanonicalName(), two.getType().getCanonicalName());
      return chain.result();
    }
  }

  private static <T> String buildErrorMessage(T instance, Multimap<Field, String> violations) {
    List<String> lines = newArrayList();
    lines.add(format("%s error(s) validating '%s'", violations.size(), instance.getClass().getCanonicalName()));
    for (Field field : Ordering.from(fieldNameComparator()).immutableSortedCopy(violations.keySet())) {
      lines.addAll(violations.get(field));
    }
    return Joiner.on('\n').join(lines);
  }

}
