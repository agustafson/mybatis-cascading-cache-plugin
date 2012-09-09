package org.mybatis.plugin;

import org.apache.ibatis.io.Resources;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

class XmlConfigurationParser {

  private final XPath xpath;

  XmlConfigurationParser() {
    final XPathFactory xpathFactory = XPathFactory.newInstance();
    xpath = xpathFactory.newXPath();
  }

  List<MappedStatementCacheMapping> parseXml(InputStream xml) {
    try {
      return parse(xml);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private List<MappedStatementCacheMapping> parse(InputStream xml) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
    DocumentBuilder documentBuilder = createDocumentBuilder();
    Document document = documentBuilder.parse(xml);

    NodeList mappedStatementMappingNodes = getNodes(document, "cascading-cache-config/mapped-statement");
    List<MappedStatementCacheMapping> mappedStatementCacheMappings = new ArrayList<MappedStatementCacheMapping>(mappedStatementMappingNodes.getLength());
    for (int m = 0; m < mappedStatementMappingNodes.getLength(); m++) {
      Node mappedStatementMappingNode = mappedStatementMappingNodes.item(m);
      final String namespace = getAttribute(mappedStatementMappingNode, "namespace");
      final NodeList cascadeQueryCacheNodes = getNodes(mappedStatementMappingNode, "cascade-query-cache");
      List<CascadeQueryCacheMapping> cascadeQueryCacheMappings = new ArrayList<CascadeQueryCacheMapping>(cascadeQueryCacheNodes.getLength());
      for (int c = 0; c < cascadeQueryCacheNodes.getLength(); c++) {
        Node cascadeQueryCacheNode = cascadeQueryCacheNodes.item(c);
        String incomingQueryId = getAttribute(cascadeQueryCacheNode, "incoming-query-id");
        final NodeList cascadedQueryNodes = getNodes(cascadeQueryCacheNode, "cascaded-query");
        List<CascadeQueryMapping> cascadeQueryMappings = new ArrayList<CascadeQueryMapping>(cascadedQueryNodes.getLength());
        for (int q=0; q<cascadedQueryNodes.getLength(); q++) {
          final Node cascadedQueryNode = cascadedQueryNodes.item(q);
          final String cascadedQueryId = getAttribute(cascadedQueryNode, "cascaded-query-id");
          NodeList cachedPropertyNodes = getNodes(cascadedQueryNode, "cached-property");
          List<CachedProperty> cachedProperties = new ArrayList<CachedProperty>();
          for (int p = 0; p < cachedPropertyNodes.getLength(); p++) {
            Node cachedPropertyNode = cachedPropertyNodes.item(p);
            String property = getAttribute(cachedPropertyNode, "property");
            String parameterName = getAttribute(cachedPropertyNode, "parameter-name");
            CachedProperty cachedProperty = new CachedProperty(property, parameterName);
            cachedProperties.add(cachedProperty);
          }
          final CascadeQueryMapping cascadeQueryMapping = new CascadeQueryMapping(cascadedQueryId, cachedProperties);
          cascadeQueryMappings.add(cascadeQueryMapping);
        }
        CascadeQueryCacheMapping cascadeQueryCacheMapping = new CascadeQueryCacheMapping(incomingQueryId, cascadeQueryMappings);
        cascadeQueryCacheMappings.add(cascadeQueryCacheMapping);
      }
      MappedStatementCacheMapping mappedStatementCacheMapping = new MappedStatementCacheMapping(namespace, cascadeQueryCacheMappings);
      mappedStatementCacheMappings.add(mappedStatementCacheMapping);
    }

    return mappedStatementCacheMappings;
  }

  private DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
    final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    documentBuilderFactory.setValidating(true);
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    documentBuilder.setEntityResolver(new CascadeCacheConfigEntityResolver());
    return documentBuilder;
  }

  private NodeList getNodes(Node node, String xpathExpression) throws XPathExpressionException {
    return (NodeList) this.xpath.evaluate(xpathExpression, node, XPathConstants.NODESET);
  }

  private String getAttribute(Node node, final String attributeName) throws XPathExpressionException {
    return this.xpath.evaluate("@" + attributeName, node);
  }

  private static class CascadeCacheConfigEntityResolver implements EntityResolver {

    private static final Map<String, String> doctypeMap = new HashMap<String, String>();

    private static final String CASCADING_CACHE_DOCTYPE = "-//mybatis.org//DTD Cascading Cache Plugin 1.0//EN".toUpperCase(Locale.ENGLISH);
    private static final String CASCADING_CACHE_URL = "http://www.mybatis.com/plugin/cascading-cache-plugin.dtd".toUpperCase(Locale.ENGLISH);
    private static final String CASCADING_CACHE_DTD = "org/mybatis/plugin/cascading-cache-plugin.dtd".toUpperCase(Locale.ENGLISH);

    static {
      doctypeMap.put(CASCADING_CACHE_DOCTYPE, CASCADING_CACHE_DTD);
      doctypeMap.put(CASCADING_CACHE_URL, CASCADING_CACHE_DTD);
    }

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException {

      if (publicId != null) {
        publicId = publicId.toUpperCase(Locale.ENGLISH);
      }
      if (systemId != null) {
        systemId = systemId.toUpperCase(Locale.ENGLISH);
      }

      InputSource source = null;
      try {
        String path = doctypeMap.get(publicId);
        source = getInputSource(path, source);
        if (source == null) {
          path = doctypeMap.get(systemId);
          source = getInputSource(path, source);
        }
      } catch (Exception e) {
        throw new SAXException(e.toString());
      }
      return source;
    }

    private InputSource getInputSource(String path, InputSource source) {
      if (path != null) {
        InputStream in;
        try {
          in = Resources.getResourceAsStream(path);
          source = new InputSource(in);
        } catch (IOException e) {
          // ignore, null is ok
        }
      }
      return source;
    }
  }
}
