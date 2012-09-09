package org.mybatis.plugin;

import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class XmlConfigurationParserTest {
  @Test
  public void whenParsingSimpleXmlFileThenMappingStatementsAreCorrect() throws Exception {
    final InputStream inputStream = this.getClass().getResourceAsStream("/CascadingCache-test.xml");

    final XmlConfigurationParser xmlConfigurationParser = new XmlConfigurationParser();
    final List<MappedStatementCacheMapping> mappedStatementCacheMappings = xmlConfigurationParser.parseXml(inputStream);

    assertThat(mappedStatementCacheMappings, equalTo(
      listOf(
        new MappedStatementCacheMapping("com.domain.AuthorMapper", listOf(
          new CascadeQueryCacheMapping("selectAllAuthors",listOf(
            new CascadeQueryMapping("selectAuthorById", listOf(
              new CachedProperty("id", "id")
            )),
            new CascadeQueryMapping("findAuthorsByCriteria", listOf(
              new CachedProperty("username", "criteria.username"),
              new CachedProperty("email", "criteria.email")
            ))
          ))
        ))
      )
    ));
  }

  private static <T> List<T> listOf(T... items) {
    return Arrays.asList(items);
  }
}
