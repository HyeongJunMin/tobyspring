package toby.service;

import lombok.AllArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@AllArgsConstructor
public class TransactionAdvice implements MethodInterceptor {
  private final PlatformTransactionManager transactionManager;
  // 타깃을 호출하는 기능을 가진 콜백 객체를 프록시로부터 받는다.
  // 덕분에 어드바이스는 특정 타깃에 의존하지 않고 재사용할 수 있다.
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());
    try {
      // 콜백을 호출해서 타깃의 메서드를 실행
      // 타깃 메서드 호출 전후로 필요한 부가기능을 넣을 수 있다.
      // 경우에 따라 타깃이 아예 호출되지 않게끔 하거나 재시도를 위한 반복적 호출도 가능하다.
      Object ret = methodInvocation.proceed();
      this.transactionManager.commit(status);
      return ret;
      // JDK 다이내픽 프록시가 제공하는 Method와 달리
      // 스프링의 MethodInvocation을 통한 타깃 호출은 예외가 포장되지 않고 타깃에서 보낸 그대로 전달된다.
    } catch (RuntimeException e) {
      this.transactionManager.rollback(status);
      throw e;
    }
  }
}
