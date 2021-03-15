package toby.common.proxy;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;

public class HelloProxyTest {

  @Test
  public void simpleProxy() {
    Hello hello = new HelloTarget();
    assertThat(hello.sayHello("Kitty")).isEqualTo("Hello Kitty");
    assertThat(hello.sayHi("Kitty")).isEqualTo("Hi Kitty");
    assertThat(hello.sayThankYou("Kitty")).isEqualTo("ThankYou Kitty");
    Hello helloProxy = new HelloUppercaseProxy(hello);
    assertThat(helloProxy.sayHello("Kitty")).isEqualTo("HELLO KITTY");
    assertThat(helloProxy.sayHi("Kitty")).isEqualTo("HI KITTY");
    assertThat(helloProxy.sayThankYou("Kitty")).isEqualTo("THANKYOU KITTY");
  }

  @Test
  public void dynamicProxy() throws Exception {
    Hello proxiedHello = (Hello)Proxy.newProxyInstance(
            // 동적으로 생성되는 프록시 클래스의 로딩에 사용할 클래스 로더
            getClass().getClassLoader(),
            // 구현할 인터페이스
            new Class[]{Hello.class},
            // 부가기능과 위임 코드를 담은 InvocationHandler
            new UppercaseHandler(new HelloTarget()));
    assertThat(proxiedHello.sayHello("Kitty")).isEqualTo("HELLO KITTY");
    assertThat(proxiedHello.sayHi("Kitty")).isEqualTo("HI KITTY");
    assertThat(proxiedHello.sayThankYou("Kitty")).isEqualTo("THANKYOU KITTY");
  }

  @Test
  public void proxyFactoryBean() {
    ProxyFactoryBean pfBean = new ProxyFactoryBean();
    pfBean.setTarget(new HelloTarget()); //타깃 설정
    pfBean.addAdvice(new UppercaseAdvice()); //부가기능 추가
    Hello proxiedHello = (Hello) pfBean.getObject(); //FacotryBean이므로 생성된 프록시를 가져온다.

    assertThat(proxiedHello.sayHello("Toby")).isEqualTo("HELLO TOBY");
    assertThat(proxiedHello.sayHi("Toby")).isEqualTo("HI TOBY");
    assertThat(proxiedHello.sayThankYou("Toby")).isEqualTo("THANKYOU TOBY");
  }

  static class UppercaseAdvice implements MethodInterceptor {
    public Object invoke(MethodInvocation invocation) throws Throwable {
      String ret = (String)invocation.proceed(); //타깃을 알고 있기에 타깃 객체를 전달할 필요가 없다.
      return ret.toUpperCase(); //부가기능 적용
    }
  }

  @Test
  public void pointcutAdvisor() {
    ProxyFactoryBean pfBean = new ProxyFactoryBean();
    pfBean.setTarget(new HelloTarget());
    // 메서드 이름을 비교해서 대상을 선정하는 알고리즘을 제공하는 포인트컷 객체
    NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
    pointcut.setMappedName("sayH*");
    // 포인트컷과 어드바이스를 advisor로 묶어서 한 번에 추가
    pfBean.addAdvisor(new DefaultPointcutAdvisor(pointcut, new UppercaseAdvice()));
    Hello proxiedHello = (Hello) pfBean.getObject();
    assertThat(proxiedHello.sayHello("Toby")).isEqualTo("HELLO TOBY");
    assertThat(proxiedHello.sayHi("Toby")).isEqualTo("HI TOBY");
    assertThat(proxiedHello.sayThankYou("Toby")).isEqualTo("ThankYou Toby");
  }

}