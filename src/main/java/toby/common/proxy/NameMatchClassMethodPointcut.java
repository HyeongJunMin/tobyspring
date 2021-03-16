package toby.common.proxy;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.util.PatternMatchUtils;

public class NameMatchClassMethodPointcut extends NameMatchMethodPointcut {
  public void setMappedClassName(String mappedClassName) {
    // 매개변수로 받은 클래스 이름을 이용한 필터
    this.setClassFilter(new SimpleClassFilter(mappedClassName));
  }
  static class SimpleClassFilter implements ClassFilter {
    private final String mappedName;
    public SimpleClassFilter(String mappedName) {
      this.mappedName = mappedName;
    }
    @Override
    public boolean matches(Class<?> clazz) {
      if (mappedName.startsWith("Test") || clazz.getSimpleName().startsWith("Test")) {
        System.out.println("!!!!!!!!!!!");
      }
      if (mappedName.startsWith("User") || clazz.getSimpleName().startsWith("User")) {
        System.out.println("!!!!!!!!!!!");
      }
      System.out.println(String.format("mappedName : %s, classSimpleName : %s", mappedName, clazz.getSimpleName()));
      return PatternMatchUtils.simpleMatch(mappedName, clazz.getSimpleName());
    }
  }
}
