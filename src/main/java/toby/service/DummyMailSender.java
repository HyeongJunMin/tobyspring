package toby.service;

import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

public class DummyMailSender implements MailSender {
  @Override
  public void send(SimpleMailMessage simpleMailMessage) throws MailException {
    // ignore for test
  }
  @Override
  public void send(SimpleMailMessage... simpleMailMessages) throws MailException {
    // ignore for test
  }
}