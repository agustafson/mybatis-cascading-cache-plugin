package org.mybatis.plugin;

import java.util.List;

class MappedStatementCacheMapping {
  private final String mappedStatementNamespace;
  private final List<CascadeQueryCacheMapping> cascadeQueryCacheMappings;

  MappedStatementCacheMapping(String mappedStatementNamespace, List<CascadeQueryCacheMapping> cascadeQueryCacheMappings) {
    this.mappedStatementNamespace = AssertUtils.ensureNotBlank(mappedStatementNamespace);
    this.cascadeQueryCacheMappings = AssertUtils.ensureNotNull(cascadeQueryCacheMappings);
  }

  public String getMappedStatementNamespace() {
    return mappedStatementNamespace;
  }

  public List<CascadeQueryCacheMapping> getCascadeQueryCacheMappings() {
    return cascadeQueryCacheMappings;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MappedStatementCacheMapping)) {
      return false;
    }

    MappedStatementCacheMapping that = (MappedStatementCacheMapping) o;

    return mappedStatementNamespace.equals(that.mappedStatementNamespace)
        && cascadeQueryCacheMappings.equals(that.cascadeQueryCacheMappings);

  }

  @Override
  public int hashCode() {
    int result = mappedStatementNamespace.hashCode();
    result = 31 * result + cascadeQueryCacheMappings.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "MappedStatementCacheMapping{" +
            "mappedStatementNamespace='" + mappedStatementNamespace + '\'' +
            ", cascadeQueryCacheMappings=" + cascadeQueryCacheMappings +
            '}';
  }
}
