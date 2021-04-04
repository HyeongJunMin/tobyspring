package toby.service.sql;

import lombok.Setter;
import toby.common.exception.SqlNotFoundException;
import toby.common.exception.SqlRetrievalFailureException;

import javax.annotation.PostConstruct;

@Setter
public class BaseSqlService implements SqlService {

  protected SqlRegistry sqlRegistry;
  protected SqlReader sqlReader;

  @PostConstruct
  public void loadSql() {
    this.sqlReader.read(this.sqlRegistry);
  }

  @Override
  public String getSql(String key) throws SqlRetrievalFailureException {
    try {
      return this.sqlRegistry.findSql(key);
    } catch (SqlNotFoundException e) {
      throw new SqlRetrievalFailureException(e.getMessage(), e.getCause());
    }
  }
}
