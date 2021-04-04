package toby.dao;

import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.test.context.junit4.SpringRunner;
import toby.common.exception.DuplicateUserIdException;
import toby.domain.Level;
import toby.domain.User;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class UserDaoTest {

  @Autowired
  private UserDao userDao;

  @Autowired
  private DataSource dataSource;

  @Test(expected = DuplicateUserIdException.class)
  public void add() throws SQLException, ClassNotFoundException {
    String userId = "id01";
    userDao.deleteAll();
    User user = new User(userId, "name1", "password1", Level.BASIC, 1, 0, "me@naver.com");
    userDao.addWithDuplicateUserIdException(user);
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
    User user = new User("userId1", "name1", "password1", Level.BASIC, 1, 0, "me@naver.com");
    userDao.add(user);
    userDao.getUserByName("name21");
  }

  @Test
  public void getAll() throws Exception {
    userDao.deleteAll();
    User user1 = new User("001", "name1", "password1", Level.BASIC, 1, 0, "me@naver.com");
    User user2 = new User("002", "name2", "password2", Level.BASIC, 1, 0, "me@naver.com");
    User user3 = new User("003", "name3", "password3", Level.BASIC, 1, 0, "me@naver.com");
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

  @Test(expected = DataAccessException.class)
  public void addUsersHavingDuplicateKeyThrowsDataAccessException() {
    addUsersHavingDuplicateKey();
  }

  private void addUsersHavingDuplicateKey() {
    User user = new User("duplicatedKey", "name1", "password1", Level.BASIC, 1, 0, "me@naver.com");
    userDao.deleteAll();
    userDao.add(user);
    userDao.add(user);
  }

  @Test(expected = DuplicateKeyException.class)
  public void addUsersHavingDuplicateKeyThrowsDuplicateKeyException() {
    addUsersHavingDuplicateKey();
  }

  // SQLException 전환 기능의 학습 테스트
  // 어쩜 이렇게 복습까지 할 수 있도록 배려를 하셨을까
  @Test
  public void translateSqlExecption() {
    userDao.deleteAll();
    try {
      addUsersHavingDuplicateKey();
    } catch (DuplicateKeyException e) {
      SQLException sqlException = (SQLException) e.getRootCause();
      // 코드를 이용한 SQLException 전환
      SQLErrorCodeSQLExceptionTranslator translator = new SQLErrorCodeSQLExceptionTranslator(this.dataSource);
      assertThat(translator.translate(null, null, sqlException).getClass()).isEqualTo(DuplicateKeyException.class);
    }
  }

  @Test
  public void update() {
    // when
    userDao.deleteAll();
    User user = new User("001", "name1", "password1", Level.BASIC, 1, 0, "me@naver.com");
    User finalUser = new User("002", "name1", "password1", Level.BASIC, 1, 0, "me@naver.com");
    userDao.add(user);
    userDao.add(finalUser);
    user.setName("히히힣");
    user.setPassword("dkaghghkehlsqlalfqjsgh");
    user.setLevel(Level.SILVER);
    user.setLogin(1000);
    user.setRecommend(999);
    userDao.update(user);
    // then
    User updatedUser = userDao.get(user.getId());
    checkSameUser(user, updatedUser);
    User updatedFinalUser = userDao.get(finalUser.getId());
    checkSameUser(finalUser, updatedFinalUser);
  }

  @Ignore("h2 cannot treat transactional")
  @Test
  public void addAllIsTransactional() {
    userDao.deleteAll();
    User user1 = new User("001", "name1", "password1", Level.BASIC, 1, 0, "me@naver.com");
    User user2 = new User("002", "name2", "password2", Level.BASIC, 1, 0, "me@naver.com");
    User duplicatedUser = new User("002", "name3", "password3", Level.BASIC, 1, 0, "me@naver.com");
    List<User> originUserList = Arrays.asList(user1, user2, duplicatedUser);
    try {
      userDao.addAll(originUserList);
    } catch (Exception e) {
      // ignore
    }
    List<User> userList = userDao.getAll();
    assertThat(userList.size()).isEqualTo(0);
  }

  @Test
  public void multiThread() {
    userDao.deleteAll();
  }
}