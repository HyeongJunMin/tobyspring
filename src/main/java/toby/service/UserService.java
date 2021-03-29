package toby.service;

import org.springframework.mail.MailSender;
import org.springframework.transaction.annotation.Transactional;
import toby.domain.User;

import java.util.List;

@Transactional
public interface UserService {
  void add(User user);
  void upgradeLevels();
  void createOrIncreaseRecommend(User user);
  void setMailSender(MailSender mailsender);
  @Transactional(readOnly = true)
  User get(String id);
  @Transactional(readOnly = true)
  List<User> getAll();
  void deleteAll();
  void update(User user);
}
