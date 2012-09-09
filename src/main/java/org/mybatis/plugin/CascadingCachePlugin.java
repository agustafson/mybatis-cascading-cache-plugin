package org.mybatis.plugin;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.CachingExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.beans.PropertyDescriptor;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Intercepts({
  @Signature(type = Executor.class, method = "query",
             args = { MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class }),
  @Signature(type = Executor.class, method = "query",
             args = { MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class })
})
public class CascadingCachePlugin implements Interceptor {
  private final Properties properties = new Properties();
  private boolean initialised;

  public Object intercept(Invocation invocation) throws Throwable {
    Object result = invocation.proceed();
    if (initialised && result instanceof List) {
      CachingExecutor cachingExecutor = (CachingExecutor) invocation.getTarget();
      MappedStatement mappedStatementIncoming = (MappedStatement) invocation.getArgs()[0];
      Configuration configuration = mappedStatementIncoming.getConfiguration();

      String namespace = getProperty(mappedStatementIncoming, "namespace");
      if (namespace != null) {
        Cache cache = configuration.getCache(namespace);

        List<?> items = (List<?>) result;
        for (Object item : items) {
          MappedStatement mappedStatement = configuration.getMappedStatement(getProperty(mappedStatementIncoming, "cascaded.mappedStatementId"));
          Object itemProperty;
          try {
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor("id", item.getClass());
            itemProperty = propertyDescriptor.getReadMethod().invoke(item);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
          BoundSql itemBoundSql = mappedStatement.getBoundSql(itemProperty);
          CacheKey cacheKey = cachingExecutor.createCacheKey(mappedStatement, itemProperty, RowBounds.DEFAULT, itemBoundSql);
          List<?> itemAsList = Collections.singletonList(item);
          mappedStatement.getCache().putObject(cacheKey, itemAsList);

          cache.putObject(cacheKey, itemAsList);
        }
      }
    }
    return result;
  }

  public Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  public void setProperties(Properties properties) {
    this.properties.put("incoming.com.domain.CachedAuthorMapper.selectAllAuthors.namespace", "com.domain.CachedAuthorMapper");
    this.properties.put("incoming.com.domain.CachedAuthorMapper.selectAllAuthors.cascaded.mappedStatementId", "com.domain.CachedAuthorMapper.selectAuthorWithInlineParams");
    initialised = true;

    this.properties.putAll(properties);
  }

  private String getProperty(MappedStatement mappedStatementIncoming, final String propertyName) {
    String propertyPrefix = "incoming." + mappedStatementIncoming.getId() + ".";
    return properties.getProperty(propertyPrefix + propertyName);
  }
}
