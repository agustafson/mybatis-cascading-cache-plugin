package org.mybatis.plugin;

import java.util.List;

class CascadeQueryMapping {
  private final String cascadedQueryId;
  private final List<CachedProperty> cachedProperties;

  CascadeQueryMapping(String cascadedQueryId, List<CachedProperty> cachedProperties) {
    this.cascadedQueryId = AssertUtils.ensureNotBlank(cascadedQueryId);
    this.cachedProperties = AssertUtils.ensureNotNull(cachedProperties);
  }

  public String getCascadedQueryId() {
    return cascadedQueryId;
  }

  public List<CachedProperty> getCachedProperties() {
    return cachedProperties;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CascadeQueryMapping)) {
      return false;
    }

    CascadeQueryMapping that = (CascadeQueryMapping) o;

    return cascadedQueryId.equals(that.cascadedQueryId)
        && cachedProperties.equals(that.cachedProperties);

  }

  @Override
  public int hashCode() {
    int result = cascadedQueryId.hashCode();
    result = 31 * result + cachedProperties.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "CascadeQueryMapping{" +
            "cascadedQueryId='" + cascadedQueryId + '\'' +
            ", cachedProperties=" + cachedProperties +
            '}';
  }
}
