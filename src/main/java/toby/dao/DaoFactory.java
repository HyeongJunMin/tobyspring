package toby.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import toby.service.sql.JaxbXmlSqlReader;
import toby.service.sql.SqlReader;
import toby.service.sql.SqlService;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class DaoFactory {

  private final SqlService sqlService;
  private final DataSource dataSource;

  @Bean
  public UserDao userDao() {
    UserDaoJdbc userDaoJdbc = new UserDaoJdbc(dataSource);
    userDaoJdbc.setSqlService(sqlService);
    return userDaoJdbc;
  }

  @Bean
  public SqlReader sqlReader() {
    return new JaxbXmlSqlReader();
  }

  @Bean
  public ConnectionMaker connectionMaker() {
    return new ConnectionMaker();
  }

}
