package toby.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import toby.common.exception.TestUserServiceException;
import toby.dao.UserDao;
import toby.domain.Level;
import toby.domain.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static toby.service.UserService.LOGIN_COUNT_FOR_SILVER;
import static toby.service.UserService.RECOMMEND_COUNT_FOR_GOLD;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
class UserServiceTest {

  @Autowired private UserService userService;
  @Autowired private UserDao userDao;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private MailSender mailSender;

  private List<User> userList;

  @BeforeEach
  public void setUp() {
    userList = Arrays.asList(
        new User("0001", "병진이형은", "pass", Level.BASIC, LOGIN_COUNT_FOR_SILVER - 1, 0, "user1@naver.com"),
        new User("0002", "나가있어", "pass", Level.BASIC, LOGIN_COUNT_FOR_SILVER, 0, "user2@naver.com"),
        new User("0003", "뒤지기", "pass", Level.SILVER, 60, RECOMMEND_COUNT_FOR_GOLD - 1, "user3@naver.com"),
        new User("0004", "싫으면", "pass", Level.SILVER, 60, RECOMMEND_COUNT_FOR_GOLD, "user4@naver.com"),
        new User("0005", "고맙다태식아", "pass", Level.SILVER, 100, 100, "user5@naver.com")
    );
  }

  @Test
  public void beanInjectionHasDone() {
    assertThat(userService).isNotNull();
  }

  @Test
  public void upgradeLevels() throws Exception {
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

  private void checkLevelUpgraded(User user, boolean upgraded) {
    User updatedUser = userDao.get(user.getId());
    if (upgraded) {
      assertThat(updatedUser.getLevel()).isEqualTo(user.getLevel().getNextLevel());
    } else {
      assertThat(updatedUser.getLevel()).isEqualTo(user.getLevel());
    }
  }

  @Test
  public void addDefaultLevel() {
    userDao.deleteAll();
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

  static class TestUserService extends UserService {

    private String id;

    public TestUserService(String id) {
      this.id = id;
    }

    protected void upgradeLevel(User user) {
      if (user.getId().equals(this.id)) {
        throw new TestUserServiceException();
      }
      super.upgradeLevel(user);
    }
  }

  @Test
  public void upgradeAllOrNothing() {
    TestUserService testUserService = new TestUserService(userList.get(3).getId());
    testUserService.setUserDao(this.userDao);
    testUserService.setTransactionManager(transactionManager);
    testUserService.setMailSender(mailSender);
    userDao.deleteAll();
    userList.forEach(user -> userDao.add(user));
    try {
      testUserService.upgradeLevels();
      fail("TestUserServiceException expected");
    } catch (TestUserServiceException e) {
    }
    checkLevelUpgraded(userList.get(1), false);
  }

  @Getter
  static class MockMailSender implements MailSender {
    private List<String> requests = new ArrayList();
    @Override
    public void send(SimpleMailMessage simpleMailMessage) throws MailException {
      // 전송요청이 들어온 메일 주소를 List에 저장한다.
      requests.add(simpleMailMessage.getTo()[0]);
    }
    @Override
    public void send(SimpleMailMessage... simpleMailMessages) throws MailException {
      // ignore
    }
  }

  @Test
  @DirtiesContext // 컨텐스트의 DI 설정을 변경하는 테스트라는 것을 표현
  public void upgradeLevelsWithMockMailSender() throws Exception {
    userDao.deleteAll();
    userList.forEach(user -> userDao.add(user));
    MockMailSender mockMailSender = new MockMailSender();
    userService.setMailSender(mockMailSender);
    userService.upgradeLevels();
    checkLevelUpgraded(userList.get(0), false);
    checkLevelUpgraded(userList.get(1), true);
    checkLevelUpgraded(userList.get(2), false);
    checkLevelUpgraded(userList.get(3), true);
    checkLevelUpgraded(userList.get(4), true);
    List<String> requests = mockMailSender.getRequests();
    assertThat(requests.size()).isEqualTo(3);
    assertThat(requests.containsAll(Arrays.asList(userList.get(1).getEmail(), userList.get(3).getEmail(), userList.get(4).getEmail())));
  }

}