package toby.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import toby.service.user.UserService;

@Configuration
@RequiredArgsConstructor
public class AopConfig {

  private final PlatformTransactionManager transactionManager;
  private final UserService userService;

//  @Bean
//  public TransactionAdvice transactionAdvice() {
//    return new TransactionAdvice(transactionManager);
//  }

//  @Bean
//  public NameMatchClassMethodPointcut transactionPointcut() {
//    NameMatchClassMethodPointcut pointcut = new NameMatchClassMethodPointcut();
//    // 클래스 이름 패턴
//    pointcut.setMappedClassName("*ServiceImpl");
//    // 메서드 이름 패턴
//    pointcut.setMappedName("upgrade*");
//    return pointcut;
//  }
//  @Bean
//  public AspectJExpressionPointcut transactionPointcut() {
//    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
//    pointcut.setExpression("execution(* *..*ServiceImpl.upgrade*(..))");
//    return pointcut;
//  }

//  @Bean
//  public DefaultPointcutAdvisor transactionAdvisor() {
//    DefaultPointcutAdvisor pointcutAdvisor = new DefaultPointcutAdvisor();
//    pointcutAdvisor.setAdvice(transactionAdvice());
//    pointcutAdvisor.setPointcut(transactionPointcut());
//    return pointcutAdvisor;
//  }

//  @Bean
//  public ProxyFactoryBean userServiceProxyFactoryBean() {
//    ProxyFactoryBean factoryBean = new ProxyFactoryBean();
//    factoryBean.setTarget(userService);
//    factoryBean.setInterceptorNames("transactionAdvisor");
//    return factoryBean;
//  }
//
//  @Bean
//  public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
//    return new DefaultAdvisorAutoProxyCreator();
//  }

//  @Bean
//  public TransactionInterceptor transactionInterceptor() {
//    TransactionInterceptor interceptor = new TransactionInterceptor();
//    interceptor.setTransactionManager(transactionManager);
//
//    NameMatchTransactionAttributeSource txAttributeSource = new NameMatchTransactionAttributeSource();
//    Map<String, TransactionAttribute> txMethods = new HashMap();
//
//    RuleBasedTransactionAttribute txAttributeForAll = new RuleBasedTransactionAttribute();
//    txAttributeForAll.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
//    txMethods.put("*", txAttributeForAll);
//
//    RuleBasedTransactionAttribute txAttributeForGet = new RuleBasedTransactionAttribute();
//    txAttributeForGet.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
//    txAttributeForGet.setReadOnly(true);
//    txAttributeForGet.setTimeout(30);
//    txMethods.put("get*", txAttributeForGet);
//
//    RuleBasedTransactionAttribute txAttributeForUpgrade = new RuleBasedTransactionAttribute();
//    txAttributeForUpgrade.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
//    txAttributeForUpgrade.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
//    txMethods.put("upgrade*", txAttributeForGet);
//
//    txAttributeSource.setNameMap(txMethods);
//    interceptor.setTransactionAttributeSources(txAttributeSource);
//    return interceptor;
//  }

}
