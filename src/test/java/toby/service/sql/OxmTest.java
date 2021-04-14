package toby.service.sql;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.oxm.Unmarshaller;
import org.springframework.test.context.junit4.SpringRunner;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class OxmTest {

  @Autowired
  private Unmarshaller unmarshaller;

  @Test
  public void unmarshallSqlMap() throws IOException {
    StreamSource source = new StreamSource(getClass().getResourceAsStream("/sql/sql-map.xml"));
    Sqlmap sqlMap = (Sqlmap) unmarshaller.unmarshal(source);
    List<SqlType> sqlList = sqlMap.getSql();
    assertThat(sqlList.size()).isEqualTo(6);
    assertThat(sqlList.get(0).getKey()).isEqualTo("userAdd");
    assertThat(sqlList.get(0).getValue()).isEqualTo("insert into users(id, name, password, level, login, recommend, email) values(?, ?, ?, ?, ?, ?, ?)");
  }

}
