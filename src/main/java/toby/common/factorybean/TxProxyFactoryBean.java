package toby.common.factorybean;

import lombok.Setter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import toby.common.proxy.TransactionHandler;

import java.lang.reflect.Proxy;

@Setter
public class TxProxyFactoryBean implements FactoryBean<Object> {
  private Object target;
  private PlatformTransactionManager transactionManager;
  private String pattern;
  private Class<?> serviceInterface;

  // FactoryBean 구현 메서드
  @Override
  public Object getObject() throws Exception {
    TransactionHandler txHandler = new TransactionHandler();
    txHandler.setTarget(target);
    txHandler.setTransactionManager(transactionManager);
    txHandler.setPattern(pattern);
    return Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class[]{serviceInterface},
            txHandler);
  }

  @Override
  public Class<?> getObjectType() {
    return serviceInterface;
  }

  @Override
    public boolean isSingleton () {
      // 싱글톤 빈이 아니라는 뜻이 아님
      // getObject가 매 번 같은 객체를 리턴하지 않는다는 의미
      return false;
  }
}
