package toby.service.sql;

import toby.common.exception.SqlNotFoundException;
import toby.common.exception.SqlUpdateFailureException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashMapSqlRegistry implements UpdatableSqlRegistry {

  private final ConcurrentHashMap<String, String> sqlMap = new ConcurrentHashMap<>();

  @Override
  public String findSql(String key) throws SqlNotFoundException {
    String sql = sqlMap.get(key);
    if (sql == null) {
      throw new SqlNotFoundException("not found. key : " + key);
    }
    return sql;
  }

  @Override
  public void registerSql(String key, String value) {
    sqlMap.put(key, value);
  }

  @Override
  public void updateSql(String key, String newValue) throws SqlUpdateFailureException {
    String foundSql = sqlMap.get(key);
    if (foundSql == null) {
      throw new SqlUpdateFailureException("update failed. key : " + key);
    }
    sqlMap.put(key, newValue);
  }

  @Override
  public void updateSql(Map<String, String> sqlMap) throws SqlUpdateFailureException {
    sqlMap.forEach((k, v) -> updateSql(k, v));
  }

}