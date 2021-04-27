package toby.dao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import toby.service.sql.*;

import javax.sql.DataSource;
import java.sql.Driver;

@Configuration
public class DaoFactory {

  @Value("${spring.datasource.url}")
  private String dataSourceUrl;

  @Value("${spring.datasource.driver-class-name}")
  private String dataSourceDriverClass;

  @Value("${spring.datasource.username}")
  private String dbUsername;

  @Value("${spring.datasource.password}")
  private String dbPassword;

  @Bean
  public UserDao userDao() {
    UserDaoJdbc userDaoJdbc = new UserDaoJdbc(dataSource());
    userDaoJdbc.setSqlService(sqlService());
    return userDaoJdbc;
  }

  @Bean
  public SqlService sqlService() {
//    SimpleSqlService sqlService = new SimpleSqlService();
//    Map<String, String> sqlMap = new HashMap();
//    sqlMap.put(SqlService.USER_ADD, "insert into users(id, name, password, level, login, recommend, email) values(?, ?, ?, ?, ?, ?, ?)");
//    sqlMap.put(SqlService.USER_GET, "select * from users where id = ?");
//    sqlMap.put(SqlService.USER_GET_ALL, "select * from users order by id");
//    sqlMap.put(SqlService.USER_DELETE_ALL, "delete from users");
//    sqlMap.put(SqlService.USER_GET_COUNT, "select count(*) from users");
//    sqlMap.put(SqlService.USER_UPDATE, "update users set name = ?, password = ?, level = ?, login = ?, recommend = ?, email = ? where id = ?");
//    sqlService.setSqlMap(sqlMap);
//    return sqlService;
//    XmlSqlService sqlService = new XmlSqlService(sqlService(), sqlService(), "/sql/sql-map.xml");
//    HashMapSqlRegistry sqlRegistry = new HashMapSqlRegistry();
//    JaxbXmlSqlReader sqlReader = new JaxbXmlSqlReader("/sql/sql-map.xml");
//    XmlSqlService sqlService = new XmlSqlService(sqlRegistry, sqlReader);

//    DefaultSqlService sqlService = new DefaultSqlService();
//    return sqlService;
    OxmSqlService sqlService = new OxmSqlService();
    sqlService.setUnmarshaller(unmarshaller());
    sqlService.setSqlRegistry(sqlRegistry());
    return sqlService;
  }

//  @Bean
//  public SqlRegistry sqlRegistry() {
//    return new HashMapSqlRegistry();
//  }

//  @Bean
//  public SqlRegistry sqlRegistry() {
//    return new MyUpdatableSqlRegistry();
//  }

//  @Bean
//  public SqlRegistry sqlRegistry() {
//    return new ConcurrentHashMapSqlRegistry();
//  }

  @Bean
  public SqlRegistry sqlRegistry() {
    EmbeddedDBSqlRegistry sqlRegistry = new EmbeddedDBSqlRegistry();
    sqlRegistry.setDataSource(embeddedDatabase());
    return sqlRegistry;
  }

  @Bean
  public DataSource embeddedDatabase() {
    return new EmbeddedDatabaseBuilder()
            .setName("embeddedDatabase")
            .setType(EmbeddedDatabaseType.HSQL)
            .addScript("/sql/embedded-schema.sql")
            .build();
  }

  @Bean
  public SqlReader sqlReader() {
    return new JaxbXmlSqlReader();
  }

  @Bean
  public ConnectionMaker connectionMaker() {
    return new ConnectionMaker();
  }

  @Bean
  public DataSource dataSource() {
    SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
    try {
      dataSource.setDriverClass((Class<? extends Driver>) Class.forName(dataSourceDriverClass));
    } catch (Exception e) { }
    dataSource.setUrl(dataSourceUrl);
    dataSource.setUsername(dbUsername);
    dataSource.setPassword(dbPassword);
    return dataSource;
  }

  @Bean
  public DataSourceTransactionManager transactionManager() {
    return new DataSourceTransactionManager(dataSource());
  }

//  @Bean
//  public JdbcContext jdbcContext() {
//    return new JdbcContext(dataSource());
//  }

//  @Bean
//  public Unmarshaller unmarshaller() {
//    Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
//    marshaller.setContextPath("toby.service.sql");
//    return marshaller;
//  }

  @Bean
  public Unmarshaller unmarshaller() {
    Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    marshaller.setContextPath("toby.service.sql");
    return marshaller;
  }

//  @Bean
//  public EmbeddedDatabase embeddedDatabase() {
//    return new EmbeddedDatabaseBuilder()
//            .setType(EmbeddedDatabaseType.HSQL)
//            .addScript("/toby/service/sql/embedded-schema.sql")
//            .build();
//  }

}
