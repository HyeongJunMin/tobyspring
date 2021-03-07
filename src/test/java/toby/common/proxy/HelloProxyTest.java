package toby.common.proxy;

import org.junit.Test;

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

}