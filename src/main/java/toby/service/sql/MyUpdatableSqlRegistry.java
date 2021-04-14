package toby.service.sql;

import lombok.extern.slf4j.Slf4j;
import toby.common.exception.SqlNotFoundException;
import toby.common.exception.SqlRetrievalFailureException;
import toby.common.exception.SqlUpdateFailureException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MyUpdatableSqlRegistry implements UpdatableSqlRegistry {

  @Override
  public void updateSql(String key, String sql) throws SqlUpdateFailureException {
    log.info("update sql. key : {}, value : {}", key, sql);
  }

  @Override
  public void updateSql(Map<String, String> sqlmap) throws SqlUpdateFailureException {
    log.info("update sql. sqlmap : {}", sqlmap);
  }

  private Map<String, String> sqlMap = new HashMap();

  @Override
  public void registerSql(String key, String sql) {
    sqlMap.put(key, sql);
  }

  @Override
  public String findSql(String key) throws SqlNotFoundException {
    String sql = sqlMap.get(key);
    if (sql == null) {
      throw new SqlRetrievalFailureException(key + "에 대한 SQL을 찾을 수 없습니다.");
    }
    return sql;
  }

}
