package toby.service.sql;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import toby.common.exception.SqlNotFoundException;
import toby.common.exception.SqlUpdateFailureException;

import javax.sql.DataSource;
import java.util.Map;

public class EmbeddedDBSqlRegistry implements UpdatableSqlRegistry {

  JdbcTemplate jdbcTemplate;
  TransactionTemplate transactionTemplate;

  public void setDataSource(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
    this.transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
  }

  public void registerSql(String key, String sql) {
    jdbcTemplate.update("insert into sqlmap(key_, sql_) values(?, ?)", key, sql);
  }

  public String findSql(String key) throws SqlNotFoundException {
    try {
      return jdbcTemplate.queryForObject("select sql_ from sqlmap where key_ = ?", String.class, key);
    } catch (EmptyResultDataAccessException e) {
      throw new SqlNotFoundException("not found. key : " + key);
    }
  }

  public void updateSql(String key, String sql) throws SqlUpdateFailureException {
    int affected = jdbcTemplate.update("update sqlmap set sql_ = ? where key_ = ?", sql, key);
    if (affected == 0) {
      throw new SqlUpdateFailureException("update failed. key : " + key);
    }
  }

  public void updateSql(Map<String, String> sqlmap) throws SqlUpdateFailureException {
    transactionTemplate.execute(new TransactionCallbackWithoutResult() {
      @Override
      protected void doInTransactionWithoutResult(TransactionStatus status) {
        sqlmap.forEach((k, v) -> updateSql(k, v));
      }
    });
  }
}
