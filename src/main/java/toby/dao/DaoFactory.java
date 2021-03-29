package toby.dao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

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
    return new UserDaoJdbc(dataSource());
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

}
