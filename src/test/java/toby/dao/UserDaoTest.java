package toby.dao;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import toby.domain.User;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class UserDaoTest {

//  @Ignore
  @Test
  @Rollback(false)
  public void addTest() throws SQLException, ClassNotFoundException {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
    String userId = "id01";
    UserDao userDao = context.getBean("userDao", UserDao.class);
    userDao.deleteAll();
    User user = new User(userId, "name1", "password1");
    userDao.add(user);
    User user2 = userDao.get(userId);
    assertThat(userId.equals(user2.getId()));
    log.info("user 2 : {}", user.toString());
  }
}