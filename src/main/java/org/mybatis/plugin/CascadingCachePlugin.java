package org.mybatis.plugin;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.BaseExecutor;
import org.apache.ibatis.executor.CachingExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;

@Intercepts({
  @Signature(type = Executor.class, method = "query",
             args = { MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class }),
  @Signature(type = Executor.class, method = "query",
             args = { MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class })
})
public class CascadingCachePlugin implements Interceptor {
  private static final Log LOG = LogFactory.getLog(CascadingCachePlugin.class);
  private static final String DEFAULT_CONFIG_LOCATION = "cascading-cache-config.xml";
  private static final String PARAMETER_CASCADING_CACHE_CONFIG_LOCATION = "cascading.cache.config.location";
  private List<MappedStatementCacheMapping> mappedStatementCacheMappings = Collections.emptyList();
  private final Map<Class<?>, PropertyDescriptor[]> typeToPropertyDescriptorMap = new HashMap<Class<?>, PropertyDescriptor[]>();

  public Object intercept(Invocation invocation) throws Throwable {
    Object result = invocation.proceed();
    if (result instanceof List && !mappedStatementCacheMappings.isEmpty()) {
      Executor executor = (Executor) invocation.getTarget();
      if (executor instanceof CachingExecutor) {
        CachingExecutor cachingExecutor = (CachingExecutor) executor;

        // TODO: this is a hack to get access to the local cache.
        final BaseExecutor baseExecutor = getFieldValue(CachingExecutor.class, cachingExecutor, "delegate");
        final Cache localCache = getFieldValue(BaseExecutor.class, baseExecutor, "localCache");

        MappedStatement mappedStatementIncoming = (MappedStatement) invocation.getArgs()[0];
        Configuration configuration = mappedStatementIncoming.getConfiguration();
        List<?> items = (List<?>) result;

        for (MappedStatementCacheMapping mappedStatementCacheMapping : mappedStatementCacheMappings) {
          String namespace = mappedStatementCacheMapping.getMappedStatementNamespace();
          List<CascadeQueryCacheMapping> cascadeQueryCacheMappings = mappedStatementCacheMapping.getCascadeQueryCacheMappings();
          if (!cascadeQueryCacheMappings.isEmpty()) {
            if (configuration.hasCache(namespace)) {
              Cache cache = configuration.getCache(namespace);

              for (CascadeQueryCacheMapping cascadeQueryCacheMapping : cascadeQueryCacheMappings) {
                String incomingQueryId = namespace + '.' + cascadeQueryCacheMapping.getIncomingQueryId();
                if (mappedStatementIncoming.getId().equals(incomingQueryId)) {
                  List<CascadeQueryMapping> cascadeQueryMappings = cascadeQueryCacheMapping.getCascadeQueryMappings();
                  for (CascadeQueryMapping cascadeQueryMapping : cascadeQueryMappings) {
                    String cascadedQueryId = namespace + '.' + cascadeQueryMapping.getCascadedQueryId();
                    List<CachedProperty> cachedProperties = cascadeQueryMapping.getCachedProperties();

                    MappedStatement mappedStatement = configuration.getMappedStatement(cascadedQueryId);
                    for (Object item : items) {
                      PropertyDescriptor[] propertyDescriptors = getPropertyDescriptors(item.getClass());
                      for (CachedProperty cachedProperty : cachedProperties) {
                        PropertyDescriptor propertyDescriptor = findPropertyDescriptor(cachedProperty, propertyDescriptors);
                        if (propertyDescriptor != null) {
                          try {
                            Object propertyValue = propertyDescriptor.getReadMethod().invoke(item);
                            Map<String,Object> parameterMap = Collections.singletonMap(cachedProperty.getParameterName(), propertyValue);

                            BoundSql itemBoundSql = getBoundSql(mappedStatement, parameterMap);
                            if (itemBoundSql != null) {
                              CacheKey cacheKey = executor.createCacheKey(mappedStatement, parameterMap, RowBounds.DEFAULT, itemBoundSql);
                              List<?> itemAsList = Collections.singletonList(item);
                              mappedStatement.getCache().putObject(cacheKey, itemAsList);
                              localCache.putObject(cacheKey, itemAsList);

                              cache.putObject(cacheKey, itemAsList);
                            }
                          } catch (Exception e) {
                            LOG.error("Could not find property for cache: " + cachedProperty.getProperty(), e);
                          }
                        } else {
                          LOG.warn("Could not find property for cache: " + cachedProperty.getProperty());
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    return result;
  }

  public Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  public void setProperties(Properties properties) {
    InputStream xml = null;
    if (properties.containsKey(PARAMETER_CASCADING_CACHE_CONFIG_LOCATION)) {
      String configLocation = properties.getProperty(PARAMETER_CASCADING_CACHE_CONFIG_LOCATION);
      xml = getInputStream(configLocation);
    }
    if (xml == null) {
      xml = getInputStream(DEFAULT_CONFIG_LOCATION);
    }
    if (xml != null) {
      mappedStatementCacheMappings = new XmlConfigurationParser().parseXml(xml);
    }
  }

  private BoundSql getBoundSql(MappedStatement mappedStatement, Map<String, Object> parameterMap) {
    try {
      return mappedStatement.getBoundSql(parameterMap);
    } catch (Exception e) {
      LOG.warn(e.getMessage());
      return null;
    }
  }

  private <T, F> F getFieldValue(Class<T> type, T item, final String fieldName) throws NoSuchFieldException, IllegalAccessException {
    final Field field = type.getDeclaredField(fieldName);
    field.setAccessible(true);
    return (F) field.get(item);
  }

  private InputStream getInputStream(String configLocation) {
    if (configLocation != null) {
      return this.getClass().getResourceAsStream(configLocation);
    } else {
      LOG.warn("Could not load cascade cache config from " + configLocation);
      return null;
    }
  }

  private PropertyDescriptor findPropertyDescriptor(CachedProperty cachedProperty, PropertyDescriptor[] propertyDescriptors) {
    for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
      if (cachedProperty.getProperty().equalsIgnoreCase(propertyDescriptor.getName())) {
        return propertyDescriptor;
      }
    }
    return null;
  }

  private PropertyDescriptor[] getPropertyDescriptors(Class<?> itemType) throws IntrospectionException {
    PropertyDescriptor[] propertyDescriptors = typeToPropertyDescriptorMap.get(itemType);
    if (propertyDescriptors == null) {
      BeanInfo beanInfo = Introspector.getBeanInfo(itemType);
      propertyDescriptors = beanInfo.getPropertyDescriptors();
      typeToPropertyDescriptorMap.put(itemType, propertyDescriptors);
    }
    return propertyDescriptors;
  }
}
