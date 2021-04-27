package toby.service.sql;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class EmbeddedDBTest {

  EmbeddedDatabase db;
  JdbcTemplate jdbcTemplate;

  @Before
  public void setUp() {
    db = new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.HSQL)
            .addScript("/embedded-schema.sql")
            .addScript("/embedded-data.sql")
            .build();
    jdbcTemplate = new JdbcTemplate(db);
  }

  @After
  public void tearDown() {
    // 테스트 후 DB 종료
    db.shutdown();
  }

  @Test
  public void initData() {
    Integer sqlmapCount = jdbcTemplate.queryForObject("select count(*) from sqlmap", Integer.class);
    assertThat(sqlmapCount).isEqualTo(2);
    List<Map<String, Object>> list = jdbcTemplate.queryForList("select * from sqlmap order by key_");
    Map<String, Object> firstObject = list.get(0);
    assertThat(firstObject.get("key_")).isEqualTo("KEY1");
    assertThat(firstObject.get("sql_")).isEqualTo("SQL1");
    Map<String, Object> secondObject = list.get(1);
    assertThat(secondObject.get("key_")).isEqualTo("KEY2");
    assertThat(secondObject.get("sql_")).isEqualTo("SQL2");
  }

  @Test
  public void insert() {
    jdbcTemplate.update("insert into sqlmap(key_, sql_) values(?, ?)", "KEY3", "SQL3");
    Integer sqlmapCount = jdbcTemplate.queryForObject("select count(*) from sqlmap", Integer.class);
    assertThat(sqlmapCount).isEqualTo(3);
  }

}
