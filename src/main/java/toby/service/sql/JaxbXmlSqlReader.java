package toby.service.sql;

import lombok.Setter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;

@Setter
public class JaxbXmlSqlReader implements SqlReader {
  private static final String DEFAULT_SQLMAP_FILE = "/sql/sql-map.xml";
  private String sqlmapFile = DEFAULT_SQLMAP_FILE;

  @Override
  public void read(SqlRegistry sqlRegistry) {
    String contextPath = Sqlmap.class.getPackage().getName();
    try {
      JAXBContext context = JAXBContext.newInstance(contextPath);
      Unmarshaller unmarshaller = context.createUnmarshaller();
      InputStream resourceAsStream = getClass().getResourceAsStream(sqlmapFile);
      Sqlmap unmarshal = (Sqlmap) unmarshaller.unmarshal(resourceAsStream);
      unmarshal.getSql().forEach(map -> sqlRegistry.registerSql(map.getKey(), map.getValue()));
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }
}
