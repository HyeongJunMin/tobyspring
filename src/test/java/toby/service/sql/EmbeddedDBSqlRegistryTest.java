package toby.service.sql;

import org.junit.After;
import org.junit.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import toby.common.exception.SqlUpdateFailureException;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.fail;

public class EmbeddedDBSqlRegistryTest extends AbstractUpdatableSqlRegistryTest {
  EmbeddedDatabase db;

  @Override
  protected UpdatableSqlRegistry createUpdatableSqlRegistry() {
    db = new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.HSQL)
            .addScript("/embedded-schema.sql")
            .build();

    EmbeddedDBSqlRegistry sqlRegistry = new EmbeddedDBSqlRegistry();
    sqlRegistry.setDataSource(db);

    return sqlRegistry;
  }

  @After
  public void tearDown() {
    db.shutdown();
  }

  @Test
  public void transactionalUpdate() {
    checkFindResult(SQL_1, SQL_2, SQL_3);
    Map<String, String> sqlmap = new HashMap();
    sqlmap.put(KEY_1, "modified1");
    sqlmap.put("unknownKey", "modified2");

    try {
      updatableSqlRegistry.updateSql(sqlmap);
      fail();
    } catch (SqlUpdateFailureException e) {
      // ignore
    }
    checkFindResult(SQL_1, SQL_2, SQL_3);
  }

}
