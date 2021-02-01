package toby.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import toby.dao.UserDao;
import toby.domain.Level;
import toby.domain.User;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static toby.service.UserService.LOGIN_COUNT_FOR_SILVER;
import static toby.service.UserService.RECOMMEND_COUNT_FOR_GOLD;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
class UserServiceTest {

  @Autowired
  private UserService userService;

  @Autowired
  private UserDao userDao;

  private List<User> userList;

  @BeforeEach
  public void setUp() {
    userList = Arrays.asList(
        new User("0001", "병진이형은", "pass", Level.BASIC, LOGIN_COUNT_FOR_SILVER - 1, 0),
        new User("0002", "나가있어", "pass", Level.BASIC, LOGIN_COUNT_FOR_SILVER, 0),
        new User("0003", "뒤지기", "pass", Level.SILVER, 60, RECOMMEND_COUNT_FOR_GOLD - 1),
        new User("0004", "싫으면", "pass", Level.SILVER, 60, RECOMMEND_COUNT_FOR_GOLD),
        new User("0005", "고맙다태식아", "pass", Level.SILVER, 100, 100)
    );
  }

  @Test
  public void beanInjectionHasDone() {
    assertThat(userService).isNotNull();
  }

  @Test
  public void upgradeLevels() {
    userDao.deleteAll();
    userList.forEach(user -> userDao.add(user));

    userService.upgradeLevels();
    checkLevel(userList.get(0).getId(), Level.BASIC);
    checkLevel(userList.get(1).getId(), Level.SILVER);
    checkLevel(userList.get(2).getId(), Level.SILVER);
    checkLevel(userList.get(3).getId(), Level.GOLD);
    checkLevel(userList.get(4).getId(), Level.GOLD);
  }

  private void checkLevel(String userId, Level expectedLevel) {
    User updatedUser = userDao.get(userId);
    assertThat(updatedUser.getLevel()).isEqualTo(expectedLevel);
  }

  @Test
  public void addDefaultLevel() {
    User userWithLevel = userList.get(4);
    userService.add(userWithLevel);
    User userWithLevelRead = userDao.get(userWithLevel.getId());
    assertThat(userWithLevel.getLevel()).isEqualTo(userWithLevelRead.getLevel());
  }

  @Test
  public void addGivenLevel() {
    User userWithoutLevel = userList.get(0);
    userWithoutLevel.setLevel(null);
    userService.add(userWithoutLevel);
    User userWithoutLevelRead = userDao.get(userWithoutLevel.getId());
    assertThat(userWithoutLevel.getLevel()).isEqualTo(userWithoutLevelRead.getLevel());
  }

}