package toby.common.proxy;

import lombok.Setter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TransactionHandler implements InvocationHandler {
  // 부가기능을 제공할 타깃 객체. 어떤 타입의 객체든 적용할 수 있다.
  @Setter private Object target;
  // 트랜잭션을 제공하기 위한 트랜잭션 매니저
  @Setter private PlatformTransactionManager transactionManager;
  // 트랜잭션을 적용할 메서드 이름 패턴
  @Setter private String pattern;
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (method.getName().startsWith(pattern)) {
      return invokeInTransaction(method, args);
    } else {
      return method.invoke(target, args);
    }
  }
  private Object invokeInTransaction(Method method, Object[] args) throws Throwable {
    TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());
    try {
      Object ret = method.invoke(target, args);
      this.transactionManager.commit(status);
      return ret;
    } catch (InvocationTargetException e) {
      this.transactionManager.rollback(status);
      throw e.getTargetException();
    }
  }
}
