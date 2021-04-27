package toby.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import toby.common.factorybean.Message;
import toby.common.factorybean.MessageFactoryBean;

@Configuration
public class FactoryBeanConfig {

  @Bean(name = "message")
  public MessageFactoryBean messageFactoryBean() {
    MessageFactoryBean factoryBean = new MessageFactoryBean();
    factoryBean.setText("Factory Bean");
    return factoryBean;
  }

  @Bean
  public Message message() throws Exception {
    return messageFactoryBean().getObject();
  }

}
