package toby.service.user;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import toby.domain.User;

import java.util.List;

@Service("userServiceTx")
@Setter
public class UserServiceTx implements UserService {

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private UserServiceImpl userService;

  @Autowired
  private MailSender mailSender;

  public void add(User user) {
    userService.add(user);
  }

  public void upgradeLevels() {
    TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
    try {
      userService.upgradeLevels();
      transactionManager.commit(status);
    } catch (Exception e) {
      transactionManager.rollback(status);
      throw e;
    }
  }

  public void createOrIncreaseRecommend(User user) {
    userService.createOrIncreaseRecommend(user);
  }

  @Override
  public User get(String id) {
    return null;
  }

  @Override
  public List<User> getAll() {
    return null;
  }

  @Override
  public void deleteAll() {

  }

  @Override
  public void update(User user) {

  }
}
