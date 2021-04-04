package toby.service.sql;

public class DefaultSqlService extends BaseSqlService {
  public DefaultSqlService() {
    setSqlRegistry(new HashMapSqlRegistry());
    setSqlReader(new JaxbXmlSqlReader());
  }
}
