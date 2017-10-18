package ua.utility.kfsdbupgrade;

import static com.google.common.base.Optional.absent;

import org.apache.commons.beanutils.PropertyUtils;

import com.google.common.base.Optional;
import com.google.common.cache.CacheLoader;

public final class PropertyClassLoader extends CacheLoader<PropertyClassKey, Optional<Class<?>>> {

  @Override
  public Optional<Class<?>> load(PropertyClassKey key) {
    try {
      Object bean = key.getType().newInstance();
      Class<?> propertyClass = PropertyUtils.getPropertyType(bean, key.getPropertyName());
      if (propertyClass == null) {
        return absent();
      } else {
        return Optional.<Class<?>>of(propertyClass);
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

}
