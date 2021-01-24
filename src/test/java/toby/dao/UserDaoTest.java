package toby.dao;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.test.context.junit4.SpringRunner;
import toby.common.exception.DuplicateUserIdException;
import toby.domain.User;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class UserDaoTest {

  UserDao userDao;

  @Before
  public void setUp() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
    userDao = context.getBean("userDao", UserDao.class);
  }

  @Test(expected = DuplicateUserIdException.class)
  public void add() throws SQLException, ClassNotFoundException {
    String userId = "id01";
    userDao.deleteAll();
    User user = new User(userId, "name1", "password1");
    userDao.add(user);
    User user2 = userDao.get(userId);
    assertThat(userId.equals(user2.getId()));
    log.info("user 2 : {}", user.toString());
  }

  @Test
  public void getCount() {
    log.info("count : {}", userDao.getCount());
  }

  @Test(expected = EmptyResultDataAccessException.class)
  public void getUserByNameWithNotExistingName() throws Exception {
    userDao.deleteAll();
    User user = new User("userId1", "name1", "password1");
    userDao.add(user);
    userDao.getUserByName("name21");
  }

  @Test
  public void getAll() throws Exception {
    userDao.deleteAll();
    User user1 = new User("001", "name1", "password1");
    User user2 = new User("002", "name2", "password2");
    User user3 = new User("003", "name3", "password3");
    List<User> originUserList = Arrays.asList(user1, user2, user3);
    for(User user : originUserList) { userDao.add(user); }

    List<User> userList = userDao.getAll();

    assertThat(userList.size()).isEqualTo(originUserList.size());
    for(int i = 0; i < userList.size(); i++) {
      checkSameUser(originUserList.get(i), userList.get(i));
    }
  }

  private void checkSameUser(User originUser, User resultUser) {
    assertThat(originUser.getId()).isEqualTo(resultUser.getId());
    assertThat(originUser.getName()).isEqualTo(resultUser.getName());
    assertThat(originUser.getPassword()).isEqualTo(resultUser.getPassword());
  }

  @Test
  public void getAllFromEmptyTable() throws Exception {
    userDao.deleteAll();
    List<User> userList = userDao.getAll();
    assertThat(userList.size()).isEqualTo(0);
  }
}