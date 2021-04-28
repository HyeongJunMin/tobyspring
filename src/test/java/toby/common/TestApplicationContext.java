package toby.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.mail.MailSender;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import toby.dao.UserDao;
import toby.dao.UserDaoJdbc;
import toby.service.DummyMailSender;
import toby.service.sql.EmbeddedDBSqlRegistry;
import toby.service.sql.OxmSqlService;
import toby.service.sql.SqlRegistry;
import toby.service.sql.SqlService;
import toby.service.user.TestUserServiceImpl;
import toby.service.user.UserService;
import toby.service.user.UserServiceImpl;

import javax.sql.DataSource;
import java.sql.Driver;

@Configuration
@EnableTransactionManagement
@PropertySource("/application.properties")
public class TestApplicationContext {

  @Value("${spring.datasource.url}")
  private String dataSourceUrl;

  @Value("${spring.datasource.driver-class-name}")
  private String dataSourceDriverClass;

  @Value("${spring.datasource.username}")
  private String dbUsername;

  @Value("${spring.datasource.password}")
  private String dbPassword;

  @Autowired
  private SqlService sqlService;

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
  public PlatformTransactionManager transactionManager() {
    return new DataSourceTransactionManager(dataSource());
  }

  @Bean
  public UserDao userDao() {
    UserDaoJdbc userDaoJdbc = new UserDaoJdbc(dataSource());
    userDaoJdbc.setSqlService(sqlService);
    return userDaoJdbc;
  }

  @Bean
  public UserService userService() {
    UserServiceImpl userService = new UserServiceImpl();
    userService.setUserDao(userDao());
    userService.setMailSender(mailSender());
    return userService;
  }

  @Bean
  public UserService testUserService() {
    TestUserServiceImpl testUserService = new TestUserServiceImpl();
    testUserService.setUserDao(userDao());
    testUserService.setMailSender(mailSender());
    return testUserService;
  }

  @Bean
  public MailSender mailSender() {
    return new DummyMailSender();
  }

  @Bean
  public SqlService sqlService() {
    OxmSqlService sqlService = new OxmSqlService();
    sqlService.setUnmarshaller(unmarshaller());
    sqlService.setSqlRegistry(sqlRegistry());
    return sqlService;
  }

  @Bean
  public Unmarshaller unmarshaller() {
    Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    marshaller.setContextPath("toby.service.sql");
    return marshaller;
  }

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
            .addScript("/embedded-schema.sql")
            .build();
  }

}
