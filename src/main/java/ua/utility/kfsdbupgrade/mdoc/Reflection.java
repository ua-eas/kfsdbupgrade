package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Maps.newConcurrentMap;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.ImmutableSet;

public final class Reflection {

  private Reflection() {}

  private static final ConcurrentMap<Class<?>, ImmutableSet<Field>> CACHE = newConcurrentMap();

  /**
   * <p>
   * Recursively examine the type hierarchy and extract all fields encountered anywhere in the hierarchy into an
   * immutable set
   * </p>
   *
   * <p>
   * NOTE: field.getName() is not necessarily unique for the elements in the set
   * </p>
   */
  public static ImmutableSet<Field> getFields(Class<?> type) {
    ImmutableSet<Field> fields = CACHE.get(type);
    if (fields == null) {
      Set<Field> mutable = newLinkedHashSet();
      for (Class<?> c = type; c != null; c = c.getSuperclass()) {
        for (Field field : type.getDeclaredFields()) {
          if (validate(field)) {
            field.setAccessible(true);
            mutable.add(field);
          }
        }
      }
      ImmutableSet<Field> immutable = copyOf(mutable);
      fields = CACHE.putIfAbsent(type, immutable);
      if (fields == null) {
        fields = immutable;
      }
    }
    return fields;
  }

  private static boolean validate(Field field) {
    if (isSwitchTable(field)) {
      return false;
    } else if (field.getAnnotation(IgnoreValidation.class) != null) {
      return false;
    } else {
      return true;
    }
  }

  // switch statements get compiled into 'fields'
  // http://stackoverflow.com/questions/12383881/what-does-maven-emma-mean-with-switch-table
  private static boolean isSwitchTable(Field field) {
    return field.getName().startsWith("$SWITCH_TABLE$");
  }

  /**
   * Return true if type is a super class of other
   */
  public static boolean isSuperClass(Class<?> type, Class<?> other) {
    return type.isAssignableFrom(other);
  }

  /**
   * Return true if type is a sub class of other
   */
  public static boolean isSubClass(Class<?> type, Class<?> other) {
    return isSuperClass(other, type);
  }

  /**
   * Return true if type is NOT a sub class of other
   */
  public static boolean isNotSubClass(Class<?> type, Class<?> other) {
    return !isSuperClass(other, type);
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<T> fromClassName(String className) {
    try {
      return (Class<T>) Class.forName(className);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

}
