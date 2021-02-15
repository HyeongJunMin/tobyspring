package toby.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSender;
import toby.service.DummyMailSender;

@Configuration
public class MailConfig {

  @Bean
  public MailSender mailSender() {
    DummyMailSender mailSender = new DummyMailSender();
    return mailSender;
  }

}
