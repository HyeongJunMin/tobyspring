package toby.service.sql;

import lombok.Setter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Unmarshaller;
import toby.common.exception.SqlRetrievalFailureException;
import toby.dao.UserDao;

import javax.annotation.PostConstruct;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;

public class OxmSqlService implements SqlService {

  private Resource sqlmap = new ClassPathResource("/sql/sql-map.xml", UserDao.class);

  public void setSqlmap(Resource sqlmap) {
    this.sqlmap = sqlmap;
  }

  private final BaseSqlService baseSqlService = new BaseSqlService();

  private SqlRegistry sqlRegistry = new HashMapSqlRegistry();

  public void setSqlRegistry(SqlRegistry sqlRegistry) {
    this.sqlRegistry = sqlRegistry;
  }

  public void setUnmarshaller(Unmarshaller unmarshaller) {
    this.oxmSqlReader.setUnmarshaller(unmarshaller);
  }

  public void sqlmapFile(String sqlmapFile) {
    this.oxmSqlReader.setSqlmapFile(sqlmapFile);
  }

  @PostConstruct
  public void loadSql() {
    this.baseSqlService.setSqlReader(this.oxmSqlReader);
    this.baseSqlService.setSqlRegistry(this.sqlRegistry);
    this.baseSqlService.loadSql();
  }

  public String getSql(String key) throws SqlRetrievalFailureException {
    return this.baseSqlService.getSql(key);
  }

  private final OxmSqlReader oxmSqlReader = new OxmSqlReader();

  @Setter
  private class OxmSqlReader implements SqlReader {
    private Unmarshaller unmarshaller;
    private static final String DEFAULT_SQLMAP_FILE = "/sql/sql-map.xml";
    private String sqlmapFile = DEFAULT_SQLMAP_FILE;

    @Override
    public void read(SqlRegistry sqlRegistry) {
      try {
//         StreamSource source = new StreamSource(getClass().getResourceAsStream(sqlmapFile));
        StreamSource source = new StreamSource(sqlmap.getInputStream());
        Sqlmap sqlMap = (Sqlmap) unmarshaller.unmarshal(source);
        sqlMap.getSql().forEach((sql) -> sqlRegistry.registerSql(sql.getKey(), sql.getValue()));
      } catch (IOException e) {
        // throw new RuntimeException(e);
        throw new RuntimeException(sqlmap.getFilename() + "을 가져올 수 없습니다.", e);
      }
    }
  }

}
