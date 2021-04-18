package toby.service.sql;

import org.junit.Before;
import org.junit.Test;
import toby.common.exception.SqlNotFoundException;
import toby.common.exception.SqlUpdateFailureException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractUpdatableSqlRegistryTest {

  UpdatableSqlRegistry updatableSqlRegistry;

  protected final String KEY_1 = "KEY1";
  protected final String KEY_2 = "KEY2";
  protected final String KEY_3 = "KEY3";
  protected final String SQL_1 = "SQL1";
  protected final String SQL_2 = "SQL2";
  protected final String SQL_3 = "SQL3";

  @Before
  public void setUp() throws Exception {
    updatableSqlRegistry = createUpdatableSqlRegistry();
    updatableSqlRegistry.registerSql(KEY_1, SQL_1);
    updatableSqlRegistry.registerSql(KEY_2, SQL_2);
    updatableSqlRegistry.registerSql(KEY_3, SQL_3);
  }

  abstract protected UpdatableSqlRegistry createUpdatableSqlRegistry();

  protected void checkFindResult(String expected1, String expected2, String expected3) {
    assertThat(updatableSqlRegistry.findSql(KEY_1)).isEqualTo(expected1);
    assertThat(updatableSqlRegistry.findSql(KEY_2)).isEqualTo(expected2);
    assertThat(updatableSqlRegistry.findSql(KEY_3)).isEqualTo(expected3);
  }

  @Test(expected = SqlNotFoundException.class)
  public void unknownKey() {
    updatableSqlRegistry.findSql("unknown");
  }

  @Test
  public void updateSingle() {
    String modifiedSql = "MODIFIED";
    updatableSqlRegistry.updateSql(KEY_2, modifiedSql);
    checkFindResult(SQL_1, modifiedSql, SQL_3);
  }

  @Test
  public void updateMulti() {
    String modifiedSql1 = "modified1";
    String modifiedSql2 = "modified2";
    Map<String, String> sqlMap = new HashMap();
    sqlMap.put(KEY_1, modifiedSql1);
    sqlMap.put(KEY_3, modifiedSql2);
    updatableSqlRegistry.updateSql(sqlMap);
    checkFindResult(modifiedSql1, SQL_2, modifiedSql2);
  }

  @Test(expected = SqlUpdateFailureException.class)
  public void updateWithNotExistingKey() {
    updatableSqlRegistry.updateSql("notExistingKey", "modified2");
  }

}
