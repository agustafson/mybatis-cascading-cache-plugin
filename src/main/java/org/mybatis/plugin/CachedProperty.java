package org.mybatis.plugin;

class CachedProperty {
  private final String property;
  private final String parameterName;

  CachedProperty(String property, String parameterName) {
    this.property = AssertUtils.ensureNotBlank(property);
    if (parameterName != null && !parameterName.trim().isEmpty()) {
      this.parameterName = parameterName;
    } else {
      this.parameterName = this.property;
    }
  }

  public String getProperty() {
    return property;
  }

  public String getParameterName() {
    return parameterName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CachedProperty)) {
      return false;
    }

    CachedProperty that = (CachedProperty) o;

    return property.equals(that.property)
        && parameterName.equals(that.parameterName);
  }

  @Override
  public int hashCode() {
    int result = property.hashCode();
    result = 31 * result + parameterName.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "CachedProperty{" +
            "property='" + property + '\'' +
            ", parameterName='" + parameterName + '\'' +
            '}';
  }
}
