package toby.common.factorybean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import toby.common.config.FactoryBeanConfig;

import javax.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

// 참고 : https://www.baeldung.com/spring-factorybean
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = FactoryBeanConfig.class)
public class MessageFactoryBeanTest {

  @Autowired
  private ApplicationContext context;

  @Qualifier("message")
  @Autowired
  private Message message;

  @Resource(name = "&message")
  private MessageFactoryBean messageFactoryBean;

  @Test
  public void getMessageFromFactoryBean() throws Exception {
    Object messageFromContext = context.getBean("message");
    assertThat(messageFromContext).isInstanceOf(Message.class);
    assertThat(((Message) messageFromContext).getText()).isEqualTo("Factory Bean");
    assertThat((message).getText()).isEqualTo("Factory Bean");
    Message messageFromFactoryBean = messageFactoryBean.getObject();
    assertThat(messageFromFactoryBean.getText()).isEqualTo("Factory Bean");
  }
}