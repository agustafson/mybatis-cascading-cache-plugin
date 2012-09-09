package org.mybatis.plugin;

import java.util.ArrayList;
import java.util.List;

class CascadeQueryCacheMapping {
  private final String incomingQueryId;
  private final List<CascadeQueryMapping> cascadeQueryMappings;

  CascadeQueryCacheMapping(String incomingQueryId, List<CascadeQueryMapping> cascadeQueryMappings) {
    this.incomingQueryId = AssertUtils.ensureNotBlank(incomingQueryId);
    this.cascadeQueryMappings = AssertUtils.ensureNotNull(cascadeQueryMappings);
  }

  public String getIncomingQueryId() {
    return incomingQueryId;
  }

  public List<CascadeQueryMapping> getCascadeQueryMappings() {
    return cascadeQueryMappings;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CascadeQueryCacheMapping)) {
      return false;
    }

    CascadeQueryCacheMapping that = (CascadeQueryCacheMapping) o;

    return incomingQueryId.equals(that.incomingQueryId)
        && cascadeQueryMappings.equals(that.cascadeQueryMappings);

  }

  @Override
  public int hashCode() {
    int result = incomingQueryId.hashCode();
    result = 31 * result + cascadeQueryMappings.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "CascadeQueryCacheMapping{" +
            "incomingQueryId='" + incomingQueryId + '\'' +
            ", cascadeQueryMappings=" + cascadeQueryMappings +
            '}';
  }
}
