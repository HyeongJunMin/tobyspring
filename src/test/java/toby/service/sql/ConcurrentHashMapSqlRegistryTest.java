package toby.service.sql;

public class ConcurrentHashMapSqlRegistryTest extends AbstractUpdatableSqlRegistryTest {

  protected UpdatableSqlRegistry createUpdatableSqlRegistry() {
    return new ConcurrentHashMapSqlRegistry();
  }

}