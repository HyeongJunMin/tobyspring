package toby.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import toby.common.TestApplicationContext;
import toby.common.exception.DuplicateUserIdException;
import toby.common.exception.TestUserServiceException;
import toby.dao.UserDao;
import toby.domain.Level;
import toby.domain.User;
import toby.service.user.UserService;
import toby.service.user.UserServiceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static toby.service.user.UserServiceImpl.LOGIN_COUNT_FOR_SILVER;
import static toby.service.user.UserServiceImpl.RECOMMEND_COUNT_FOR_GOLD;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestApplicationContext.class)
@Slf4j
public class UserServiceTest {

  @Autowired private UserService userService;
  @Autowired private UserDao userDao;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private MailSender mailSender;
  @Autowired private UserService testUserService;

  private List<User> userList;

  @Before
  public void setUp() {
    userList = Arrays.asList(
        new User("0001", "a", "pass", Level.BASIC, LOGIN_COUNT_FOR_SILVER - 1, 0, "user1@naver.com"),
        new User("0002", "b", "pass", Level.BASIC, LOGIN_COUNT_FOR_SILVER, 0, "user2@naver.com"),
        new User("0003", "c", "pass", Level.SILVER, 60, RECOMMEND_COUNT_FOR_GOLD - 1, "user3@naver.com"),
        new User("0004", "d", "pass", Level.SILVER, 60, RECOMMEND_COUNT_FOR_GOLD, "user4@naver.com"),
        new User("0005", "e", "pass", Level.SILVER, 100, 100, "user5@naver.com")
    );
  }

  @Test
  public void beanInjectionHasDone() {
    assertThat(userService).isNotNull();
  }

  @Test
  public void upgradeLevels() throws Exception {
    // 고립된 테스트이므로 대상 객체를 직접 생성한다.
    UserServiceImpl userService = new UserServiceImpl();
    // 목 객체를 set해준다.
    MockUserDao mockUserDao = new MockUserDao(this.userList);
    userService.setUserDao(mockUserDao);
    MockMailSender mockMailSender = new MockMailSender();
    userService.setMailSender(mockMailSender);
    // 목 객체를 가진 UserService의 메서드를 수행한다.
    userService.upgradeLevels();
    // UserService 결과가 담긴 목 객체의 값들을 확인한다.
    List<User> updated = mockUserDao.getUpdated();
    assertThat(updated.size()).isEqualTo(3);
    checkUserAndLevel(updated.get(0), userList.get(1).getId(), Level.SILVER);
    checkUserAndLevel(updated.get(1), userList.get(3).getId(), Level.GOLD);
    checkUserAndLevel(updated.get(2), userList.get(4).getId(), Level.GOLD);
    List<String> requests = mockMailSender.getRequests();
    assertThat(requests.size()).isEqualTo(3);
    assertThat(requests.get(0)).isEqualTo(userList.get(1).getEmail());
    assertThat(requests.get(1)).isEqualTo(userList.get(3).getEmail());
    assertThat(requests.get(2)).isEqualTo(userList.get(4).getEmail());
  }

  private void checkLevelUpgraded(User user, boolean upgraded) {
    User updatedUser = userDao.get(user.getId());
    if (upgraded) {
      assertThat(updatedUser.getLevel()).isEqualTo(user.getLevel().getNextLevel());
    } else {
      assertThat(updatedUser.getLevel()).isEqualTo(user.getLevel());
    }
  }

  private void checkUserAndLevel(User updated, String expectedId, Level expectedLevel) {
    assertThat(updated.getId()).isEqualTo(expectedId);
    assertThat(updated.getLevel()).isEqualTo(expectedLevel);
  }

  @Test
  public void upgradeLevelsWithMockito() {
    UserServiceImpl userService = new UserServiceImpl();
    UserDao mockUserDao = mock(UserDao.class);
    when(mockUserDao.getAll()).thenReturn(this.userList);
    userService.setUserDao(mockUserDao);
    MailSender mockMailSender = mock(MailSender.class);
    userService.setMailSender(mockMailSender);
    userService.upgradeLevels();
    // 목 객체가 제공하는 검증 기능 활용
    // 어떤 메서드가 몇 번 호출됐는지, 파라미터는 무엇인지 확인할 수 있다.
    verify(mockUserDao, times(3)).update(any(User.class));
    verify(mockUserDao).update(userList.get(1));
    assertThat(userList.get(1).getLevel()).isEqualTo(Level.SILVER);
    verify(mockUserDao).update(userList.get(3));
    assertThat(userList.get(3).getLevel()).isEqualTo(Level.GOLD);
    verify(mockUserDao).update(userList.get(4));
    assertThat(userList.get(4).getLevel()).isEqualTo(Level.GOLD);
    // 파라미터를 정밀하게 검사하기 위해 캡처
    ArgumentCaptor<SimpleMailMessage> mailMessageArgs = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mockMailSender, times(3)).send(mailMessageArgs.capture());
    List<SimpleMailMessage> mailMessages = mailMessageArgs.getAllValues();
    assertThat(Objects.requireNonNull(mailMessages.get(0).getTo())[0]).isEqualTo(userList.get(1).getEmail());
    assertThat(Objects.requireNonNull(mailMessages.get(1).getTo())[0]).isEqualTo(userList.get(3).getEmail());
    assertThat(Objects.requireNonNull(mailMessages.get(2).getTo())[0]).isEqualTo(userList.get(4).getEmail());
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
    userDao.deleteAll();
    User userWithoutLevel = userList.get(0);
    userWithoutLevel.setLevel(null);
    userService.add(userWithoutLevel);
    User userWithoutLevelRead = userDao.get(userWithoutLevel.getId());
    assertThat(userWithoutLevel.getLevel()).isEqualTo(userWithoutLevelRead.getLevel());
  }

  @Test
  @Ignore("심심해서 만들어본거")
  public void multiThread() throws ExecutionException, InterruptedException {
    userDao.deleteAll();
    User user = new User("5555", "예림이그패봐봐", "pass", Level.BASIC, 0, 0, "");
    ForkJoinPool myPool = new ForkJoinPool(10);
    myPool.submit(() -> {
      IntStream.range(0, 1000).parallel().forEach(index -> {
        userService.createOrIncreaseRecommend(user);
      });
    }).get();

    for (User resultUser : userDao.getAll()) {
      log.info("result user : {}", resultUser);
    }

  }

  @Test
  public void changeStringInPrivateMEthod() {
    String originString = "origin";
    log.info("origin :{}", originString);
  }

  @Test
  public void upgradeAllOrNothing() {
    userDao.deleteAll();
    userDao.addAll(userList);
    testUserService.setMailSender(mailSender);
    try {
      testUserService.upgradeLevels();
      fail("TestUserServiceException expected");
    } catch (TestUserServiceException e) { }
    checkLevelUpgraded(userList.get(1), false);
  }

  @Test
  public void transactionSync() {
    // UserService를 호출하기 전에 트랜잭션을 시작해주면 트랜잭션이 전파되어 통합된다.
    DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();
    // 트랜잭션 매니저에게 트랜잭션을 요청한다.
    // 기존에 시작된 트랜잭션이 없으므로 새로운 트랜잭션을 시작시키고 트랜잭션 정보를 돌려준다.
    // 동시에 만들어진 트랜잭션을 다른 곳에서도 사용할 수 있도록 동기화한다.
    TransactionStatus txStatus = transactionManager.getTransaction(txDefinition);
    userService.deleteAll();
    userService.add(userList.get(0));
    userService.add(userList.get(1));
    transactionManager.commit(txStatus);
  }

  @Ignore("does not work with h2 db")
  @Test(expected = TransientDataAccessResourceException.class)
  public void transactionSyncReadOnly() {
    DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();
    txDefinition.setReadOnly(true);
    TransactionStatus txStatus = transactionManager.getTransaction(txDefinition);
    userService.deleteAll();
  }

  @Test
  public void transactionSyncRollBack() {
    userDao.deleteAll();
    assertThat(userDao.getAll().size()).isEqualTo(0);
    DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();
    TransactionStatus txStatus = transactionManager.getTransaction(txDefinition);
    userService.add(userList.get(0));
    assertThat(userDao.getAll().size()).isEqualTo(1);
    transactionManager.rollback(txStatus);
    assertThat(userDao.getAll().size()).isEqualTo(0);
  }

//  @Test
//  public void advisorAutoProxyCreator() {
//    assertThat(testUserServiceImpl.getClass().toString().contains("Proxy")).isTrue();
//  }

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
  @DirtiesContext // 컨텐스트의 DI 설정을 변경하는 테스트라는 것을 표현. 내 프로젝트에서는 이 어노테이션이 없어도 실행된다.
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

  @Ignore("does not work with h2 db")
  @Test(expected = TransientDataAccessResourceException.class)
  public void readOnlyTransactionAttribute() {
    testUserService.deleteAll();
    testUserService.add(userList.get(3));
    testUserService.getAll();
  }

  static class MockUserDao implements UserDao {
    private List<User> users;
    private List<User> updated = new ArrayList();
    private MockUserDao(List<User> users) {
      this.users = users;
    }
    private List<User> getUpdated() {
      return this.updated;
    }
    @Override
    public List<User> getAll() {
      return this.users;
    }
    @Override
    public void update(User user) {
      updated.add(user);
    }
    // 미사용 메서드
    public void add(User user) { throw new UnsupportedOperationException(); }
    public void addAll(List<User> userList) { throw new UnsupportedOperationException(); }
    public void addWithDuplicateUserIdException(User user) throws DuplicateUserIdException { throw new UnsupportedOperationException(); }
    public User get(String id) { throw new UnsupportedOperationException(); }
    public User getUserByName(String name) { throw new UnsupportedOperationException(); }
    public void deleteAll() { throw new UnsupportedOperationException(); }
    public int getCount() { throw new UnsupportedOperationException(); }
  }

}