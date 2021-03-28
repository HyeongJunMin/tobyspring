package toby.service;

import org.springframework.mail.MailSender;
import toby.domain.User;

import java.util.List;

public interface UserService {
  void add(User user);
  void upgradeLevels();
  void createOrIncreaseRecommend(User user);
  void setMailSender(MailSender mailsender);
  User get(String id);
  List<User> getAll();
  void deleteAll();
  void update(User user);
}
