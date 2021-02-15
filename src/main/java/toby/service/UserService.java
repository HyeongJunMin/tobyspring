package toby.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import toby.dao.UserDao;
import toby.domain.Level;
import toby.domain.User;

import java.util.List;

@Setter
@Service
@Slf4j
public class UserService {

  public static int LOGIN_COUNT_FOR_SILVER = 50;
  public static int RECOMMEND_COUNT_FOR_GOLD = 30;

  @Autowired
  private UserDao userDao;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private MailSender mailSender;

  public void upgradeLevels() {
    // 트랜잭션 시작
    TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
    try {
      List<User> userList = userDao.getAll();
      userList.forEach(user -> {
        if (canUpgradeLevel(user)) {
          upgradeLevel(user);
        }
      });
      transactionManager.commit(status);
    } catch (Exception e) {
      transactionManager.rollback(status);
      throw e;
    }
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

}