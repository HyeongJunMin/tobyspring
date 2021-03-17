package toby.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import toby.common.proxy.NameMatchClassMethodPointcut;
import toby.service.TransactionAdvice;
import toby.service.UserService;

@Configuration
@RequiredArgsConstructor
public class AopConfig {

  private final PlatformTransactionManager transactionManager;
  private final UserService userService;

  @Bean
  public TransactionAdvice transactionAdvice() {
    return new TransactionAdvice(transactionManager);
  }

//  @Bean
//  public NameMatchClassMethodPointcut transactionPointcut() {
//    NameMatchClassMethodPointcut pointcut = new NameMatchClassMethodPointcut();
//    // 클래스 이름 패턴
//    pointcut.setMappedClassName("*ServiceImpl");
//    // 메서드 이름 패턴
//    pointcut.setMappedName("upgrade*");
//    return pointcut;
//  }
  @Bean
  public AspectJExpressionPointcut transactionPointcut() {
    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
    pointcut.setExpression("execution(* *..*ServiceImpl.upgrade*(..))");
    return pointcut;
  }

  @Bean
  public DefaultPointcutAdvisor transactionAdvisor() {
    DefaultPointcutAdvisor pointcutAdvisor = new DefaultPointcutAdvisor();
    pointcutAdvisor.setAdvice(transactionAdvice());
    pointcutAdvisor.setPointcut(transactionPointcut());
    return pointcutAdvisor;
  }

  @Bean
  public ProxyFactoryBean userServiceProxyFactoryBean() {
    transactionAdvisor();
    ProxyFactoryBean factoryBean = new ProxyFactoryBean();
    factoryBean.setTarget(userService);
    factoryBean.setInterceptorNames("transactionAdvisor");
    return factoryBean;
  }

  @Bean
  public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
    return new DefaultAdvisorAutoProxyCreator();
  }

}
