package ua.utility.kfsdbupgrade;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.hash;

public final class PropertyClassKey {

  public PropertyClassKey(Class<?> type, String propertyName) {
    this.type = checkNotNull(type);
    this.propertyName = checkNotNull(propertyName);
    this.hash = hash(type, propertyName);
  }

  private final Class<?> type;
  private final String propertyName;
  private final int hash;

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    } else if (object == null || object.getClass() != getClass()) {
      return false;
    } else {
      PropertyClassKey other = (PropertyClassKey) object;
      return (hash == other.hash) && equal(type, other.type) && equal(propertyName, other.propertyName);
    }
  }

  public Class<?> getType() {
    return type;
  }

  public String getPropertyName() {
    return propertyName;
  }

}
