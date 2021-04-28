package toby.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import toby.dao.UserDao;
import toby.service.DummyMailSender;
import toby.service.user.TestUserServiceImpl;
import toby.service.user.UserService;
import toby.service.user.UserServiceImpl;

import javax.sql.DataSource;
import java.sql.Driver;

@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = "toby.common.config")
@Import({AppContext.ProductionAppContext.class
        , AppContext.TestAppContext.class
        , SqlServiceContext.class
})
@PropertySource("/application.properties")
public class AppContext {

  @Value("${spring.datasource.url}")
  private String dataSourceUrl;

  @Value("${spring.datasource.driver-class-name}")
  private String dataSourceDriverClass;

  @Value("${spring.datasource.username}")
  private String dbUsername;

  @Value("${spring.datasource.password}")
  private String dbPassword;

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

  @Configuration
  @Profile("production")
  @RequiredArgsConstructor
  public static class ProductionAppContext {

    private final UserDao userDao;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public MailSender mailSender() {
      JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
      javaMailSender.setHost("localhost");
      return javaMailSender;
    }

    @Bean
    public UserService userService() {
      UserServiceImpl userService = new UserServiceImpl();
      userService.setUserDao(userDao);
      userService.setTransactionManager(transactionManager);
      userService.setMailSender(mailSender());
      return userService;
    }

  }

  @Configuration
  @Profile("test")
  @RequiredArgsConstructor
  public static class TestAppContext {

    private final UserDao userDao;

    @Bean
    public UserService testUserService() {
      TestUserServiceImpl testUserService = new TestUserServiceImpl();
      testUserService.setUserDao(userDao);
      testUserService.setMailSender(mailSender());
      return testUserService;
    }

    @Bean
    public MailSender mailSender() {
      return new DummyMailSender();
    }

  }

}
