package toby.service.sql;

import toby.common.exception.SqlNotFoundException;
import toby.common.exception.SqlRetrievalFailureException;

import javax.annotation.PostConstruct;

public class XmlSqlService implements SqlService {

  private SqlRegistry sqlRegistry;

  private SqlReader sqlReader;

  public XmlSqlService(SqlRegistry sqlRegistry, SqlReader sqlReader) {
    this.sqlRegistry = sqlRegistry;
    this.sqlReader = sqlReader;
  }

  @PostConstruct  // 빈 객체를 생성하고 DI작업을 마친 뒤에 @PostConstruct 메서드를 실행함
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
