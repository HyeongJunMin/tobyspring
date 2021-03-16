package toby.service;

import org.springframework.mail.MailSender;
import toby.domain.User;

public interface UserService {
  void add(User user);
  void upgradeLevels();
  void createOrIncreaseRecommend(User user);
  void setMailSender(MailSender mailsender);
}
