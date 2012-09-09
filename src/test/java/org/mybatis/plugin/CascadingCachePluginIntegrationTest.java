package org.mybatis.plugin;

import domain.blog.Author;
import domain.blog.Blog;
import domain.blog.DatabaseUtils;
import domain.blog.mappers.AuthorMapper;
import domain.blog.mappers.BlogMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class CascadingCachePluginIntegrationTest {

  private SqlSession sqlSession;
  private AuthorMapper authorMapper;
  private BlogMapper blogMapper;

  @Before
  public void setUp() throws Exception {
    DatabaseUtils.createDataSource();

    InputStream inputStream = this.getClass().getResourceAsStream("/domain/blog/mappers/MapperConfig.xml");
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    sqlSession = sqlSessionFactory.openSession();
    authorMapper = sqlSession.getMapper(AuthorMapper.class);
    blogMapper = sqlSession.getMapper(BlogMapper.class);
  }

  @After
  public void tearDown() throws Exception {
    try {
      sqlSession.close();
    } catch (Exception ignore) {
    }
  }

  @Test
  public void whenFindingAllAuthorsThenFindingAuthorByPropertiesShouldHaveAuthorsCached() {
    final List<Author> authors = authorMapper.selectAllAuthors();
    assertThat("authors", authors, hasSize(greaterThan(0)));

    for (Author author : authors) {
      final int authorId = author.getId();
      final Author authorFoundById = authorMapper.selectAuthorById(authorId);
      assertThat("author by id " + authorId, author, sameInstance(authorFoundById));
    }
  }

  @Test
  public void whenFindingAllBlogsThenFindingBlogByPropertiesShouldHaveBlogsAndAuthorsCached() {
    final List<Author> authors = authorMapper.selectAllAuthors();
    assertThat("authors", authors, hasSize(greaterThan(0)));
    final List<Blog> blogs = blogMapper.selectAllBlogs();
    assertThat("blogs", blogs, hasSize(greaterThan(0)));

    for (Blog blog : blogs) {
      final int blogId = blog.getId();
      final Blog blogFoundById = blogMapper.selectBlogById(blogId);
      assertThat("blog by id " + blogId, blog, sameInstance(blogFoundById));
      assertThat("author is cached for blog " + blogId, authors, Matchers.<Author>hasItem(Matchers.<Author>sameInstance(blog.getAuthor())));
    }
  }
}
