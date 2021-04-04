package toby.service.sql;

import toby.common.exception.SqlNotFoundException;
import toby.common.exception.SqlRetrievalFailureException;

import java.util.HashMap;
import java.util.Map;

public class HashMapSqlRegistry implements SqlRegistry {

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
