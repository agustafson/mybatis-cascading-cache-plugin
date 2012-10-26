package org.mybatis.plugin;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.util.*;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheKey;
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
  private final Map<Class<?>, Map<String, PropertyDescriptor>> typeToPropertyDescriptorMap = new HashMap<Class<?>, Map<String, PropertyDescriptor>>();

  public Object intercept(Invocation invocation) throws Throwable {
    Object result = invocation.proceed();
    final Object invocationTarget = invocation.getTarget();
    if (invocationTarget instanceof CachingExecutor && result instanceof List && !mappedStatementCacheMappings.isEmpty()) {
      CachingExecutor cachingExecutor = (CachingExecutor) invocationTarget;

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
                    putItemForAllPropertiesInCaches(cachingExecutor, cachedProperties, mappedStatement, item, cache);
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

  private void putItemForAllPropertiesInCaches(CachingExecutor cachingExecutor, List<CachedProperty> cachedProperties,
      MappedStatement mappedStatement, Object item, Cache cache) throws IntrospectionException {
    Map<String, PropertyDescriptor> propertyDescriptorMap = getPropertyDescriptorMap(item.getClass());
    for (CachedProperty cachedProperty : cachedProperties) {
      PropertyDescriptor propertyDescriptor = findPropertyDescriptor(cachedProperty, propertyDescriptorMap);
      if (propertyDescriptor != null) {
        putItemByPropertyIntoCaches(cachingExecutor, mappedStatement, item, cachedProperty, propertyDescriptor, cache);
      } else {
        LOG.warn("Could not find property for cache: " + cachedProperty.getProperty());
      }
    }
  }

  private void putItemByPropertyIntoCaches(CachingExecutor cachingExecutor, MappedStatement mappedStatement, Object item,
      CachedProperty cachedProperty, PropertyDescriptor propertyDescriptor, Cache cache) {
    try {
      Object propertyValue = propertyDescriptor.getReadMethod().invoke(item);
      Map<String,Object> parameterMap = Collections.singletonMap(cachedProperty.getParameterName(), propertyValue);

      BoundSql itemBoundSql = getBoundSql(mappedStatement, parameterMap);
      if (itemBoundSql != null) {
        CacheKey cacheKey = cachingExecutor.createCacheKey(mappedStatement, parameterMap, RowBounds.DEFAULT, itemBoundSql);
        List<?> itemAsList = Collections.singletonList(item);
        mappedStatement.getCache().putObject(cacheKey, itemAsList);
        cache.putObject(cacheKey, itemAsList);
      }
    } catch (Exception e) {
      LOG.error("Could not find property for cache: " + cachedProperty.getProperty(), e);
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

  private InputStream getInputStream(String configLocation) {
    if (configLocation != null) {
      return this.getClass().getResourceAsStream(configLocation);
    } else {
      LOG.warn("Could not load cascade cache config from " + configLocation);
      return null;
    }
  }

  private PropertyDescriptor findPropertyDescriptor(CachedProperty cachedProperty, Map<String, PropertyDescriptor> propertyDescriptorMap) {
    return propertyDescriptorMap.get(cachedProperty.getProperty().toLowerCase());
  }

  private Map<String, PropertyDescriptor> getPropertyDescriptorMap(Class<?> itemType) throws IntrospectionException {
    Map<String, PropertyDescriptor> propertyDescriptorMap = typeToPropertyDescriptorMap.get(itemType);
    if (propertyDescriptorMap == null) {
      propertyDescriptorMap = new HashMap<String, PropertyDescriptor>();
      for (PropertyDescriptor propertyDescriptor : getPropertyDescriptors(itemType)) {
        propertyDescriptorMap.put(propertyDescriptor.getDisplayName().toLowerCase(), propertyDescriptor);
      }
      typeToPropertyDescriptorMap.put(itemType, propertyDescriptorMap);
    }
    return propertyDescriptorMap;
  }

  private Set<PropertyDescriptor> getPropertyDescriptors(Class<?> itemType) throws IntrospectionException {
    BeanInfo beanInfo = Introspector.getBeanInfo(itemType);
    Set<PropertyDescriptor> propertyDescriptors = new LinkedHashSet(Arrays.asList(beanInfo.getPropertyDescriptors()));

    final Class<?>[] interfaces = itemType.getInterfaces();
    for (Class<?> superClass : interfaces) {
      propertyDescriptors.addAll(getPropertyDescriptors(superClass));
    }
    return propertyDescriptors;
  }
}
