package toby.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import toby.dao.UserDao;
import toby.service.sql.EmbeddedDBSqlRegistry;
import toby.service.sql.OxmSqlService;
import toby.service.sql.SqlRegistry;
import toby.service.sql.SqlService;

import javax.sql.DataSource;

@Configuration
public class SqlServiceContext {

  @Bean
  public SqlService sqlService() {
    OxmSqlService sqlService = new OxmSqlService();
    sqlService.setUnmarshaller(unmarshaller());
    sqlService.setSqlRegistry(sqlRegistry());
    sqlService.setSqlmap(new ClassPathResource("/sql/sql-map.xml", UserDao.class));
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
            .addScript("/sql/embedded-schema.sql")
            .build();
  }

}
