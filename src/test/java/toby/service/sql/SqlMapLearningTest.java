package toby.service.sql;

import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SqlMapLearningTest {

  @Test
  public void readSqlMap() throws JAXBException {
    String contextPath = Sqlmap.class.getPackage().getName();
    JAXBContext context = JAXBContext.newInstance(contextPath);
    Unmarshaller unmarshaller = context.createUnmarshaller();
    // 경로 : resources/sql/
    InputStream resourceAsStream = getClass().getResourceAsStream("/sql/sql-map-test.xml");
    Sqlmap sqlMap = (Sqlmap) unmarshaller.unmarshal(resourceAsStream);
    List<SqlType> sqlList = sqlMap.getSql();
    assertThat(sqlList.size()).isEqualTo(3);
    assertThat(sqlList.get(0).getKey()).isEqualTo("add");
    assertThat(sqlList.get(0).getValue()).isEqualTo("insert");
  }
}
