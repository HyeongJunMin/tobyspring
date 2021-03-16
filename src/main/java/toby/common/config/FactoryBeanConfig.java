package toby.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import toby.common.factorybean.Message;
import toby.common.factorybean.MessageFactoryBean;
import toby.common.factorybean.TxProxyFactoryBean;
import toby.service.UserService;

@Configuration
public class FactoryBeanConfig {

  @Autowired private UserService userService;
  @Autowired private PlatformTransactionManager transactionManager;

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

  @Bean(name = "txProxy")
  public TxProxyFactoryBean txProxyFactoryBean() {
    TxProxyFactoryBean factoryBean = new TxProxyFactoryBean();
    factoryBean.setTarget(userService);
    factoryBean.setTransactionManager(transactionManager);
    factoryBean.setPattern("upgradeLevels");
    factoryBean.setServiceInterface(UserService.class);
    return factoryBean;
  }

}
