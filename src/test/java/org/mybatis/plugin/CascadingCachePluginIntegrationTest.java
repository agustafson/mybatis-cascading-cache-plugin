package org.mybatis.plugin;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import domain.blog.Author;
import domain.blog.DatabaseUtils;
import domain.blog.mappers.AuthorMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

public class CascadingCachePluginIntegrationTest {

  private SqlSession sqlSession;
  private AuthorMapper authorMapper;

  @Before
  public void setUp() throws Exception {
    DatabaseUtils.createDataSource();

    InputStream inputStream = this.getClass().getResourceAsStream("/domain/blog/mappers/MapperConfig.xml");
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    sqlSession = sqlSessionFactory.openSession();
    authorMapper = sqlSession.getMapper(AuthorMapper.class);
  }

  @After
  public void tearDown() throws Exception {
    try {
      sqlSession.close();
    } catch (Exception ignore) {
    }
  }

  @Test
  public void whenFindingAllAuthorsThenFindingAuthorByPropertiesShouldBeCached() {
    final List<Author> authors = authorMapper.selectAllAuthors();
    assertThat("authors", authors, hasSize(greaterThan(0)));
  }
}
