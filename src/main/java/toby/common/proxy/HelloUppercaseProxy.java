package toby.common.proxy;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class HelloUppercaseProxy implements Hello {
  // 위임할 타깃 객체.
  Hello hello;
  public String sayHello(String name) { return hello.sayHello(name).toUpperCase(); }
  public String sayHi(String name) { return hello.sayHi(name).toUpperCase(); }
  public String sayThankYou(String name) { return hello.sayThankYou(name).toUpperCase(); }
}
