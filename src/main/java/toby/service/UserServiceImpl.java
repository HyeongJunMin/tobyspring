package toby.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import toby.dao.UserDao;
import toby.domain.Level;
import toby.domain.User;

import java.util.List;
import java.util.Optional;

@Setter
@Service("userService")
@Slf4j
public class UserServiceImpl implements UserService {

  public static int LOGIN_COUNT_FOR_SILVER = 50;
  public static int RECOMMEND_COUNT_FOR_GOLD = 30;

  @Autowired
  private UserDao userDao;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private MailSender mailSender;

  // upgradeLevels에서 비즈니스로직만 두고 트랜잭션 경계설정 코드 제거
  public void upgradeLevels() {
    List<User> userList = userDao.getAll();
    userList.forEach(user -> {
      if (canUpgradeLevel(user)) {
        upgradeLevel(user);
      }
    });
  }

  // upgradeLevels에서 비즈니스로직만 분리
  private void upgradeLevelsInternal() {
    List<User> userList = userDao.getAll();
    userList.forEach(user -> {
      if (canUpgradeLevel(user)) {
        upgradeLevel(user);
      }
    });
  }

  private boolean canUpgradeLevel(User user) {
    Level currentLevel = user.getLevel();
    switch (currentLevel) {
      case GOLD: return false;
      case BASIC: return (user.getLogin() >= LOGIN_COUNT_FOR_SILVER);
      case SILVER: return (user.getRecommend() >= RECOMMEND_COUNT_FOR_GOLD);
      default: throw new IllegalArgumentException("Unknown level : " + currentLevel);
    }
  }

  protected void upgradeLevel(User user) {
    user.upgradeLevel();
    userDao.update(user);
    sendUpgradeEMail(user);
  }

  private void sendUpgradeEMail(User user) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(user.getEmail());
    message.setFrom("useradmin@admin.com");
    message.setSubject("등업이다");
    message.setSubject("너 이제 " + user.getLevel().name());
    mailSender.send(message);
    log.debug("email sent successfully");
  }

  public void add(User user) {
    if (user.getLevel() == null) {
      user.setLevel(Level.BASIC);
    }
    userDao.add(user);
  }

  public void createOrIncreaseRecommend(User user) {
    final Optional<String> any = userDao.getAll()
        .stream()
        .map(User::getId)
        .filter(id -> id.equals(user.getId()))
        .findAny();
    if (any.isPresent()) {
      user.setRecommend(user.getRecommend() + 1);
      userDao.update(user);
    } else {
      try {
        userDao.add(user);
      } catch (Exception e) {
        System.out.println("user id : " + user.getId());
      }
    }
  }

}