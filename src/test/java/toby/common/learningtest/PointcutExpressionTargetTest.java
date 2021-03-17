package toby.common.learningtest;

import org.junit.Test;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;

import static org.assertj.core.api.Assertions.assertThat;

public class PointcutExpressionTargetTest {

  @Test
  public void showTargetMethod() throws Exception {
    System.out.println("test start");
    System.out.println(Target.class.getMethod("minus", int.class, int.class));
    System.out.println("test end");
    // public int toby.common.learningtest.Target.minus(int,int)
    // public : 접근제한자
    // int : 리턴 타입
    // toby.common.learningtest.Target : 클래스 타입 패턴
    // minus : 메서드 이름 패턴
    // (int, int) : 메서드 파라미터 타입 패턴
    // exception 생략 가능
  }

  @Test
  public void methodSignaturePointcut() throws SecurityException, NoSuchMethodException {
    // aspectjweaver 디펜던시 필요
    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
    pointcut.setExpression("execution(public int toby.common.learningtest.Target.minus(int,int)) ");
    // Target.minus() : 성공
    assertThat(pointcut.getClassFilter().matches(Target.class) &&
            pointcut.getMethodMatcher().matches(Target.class.getMethod("minus", int.class, int.class), null)).isTrue();
    // Target.plus() : 메서드 매처에서 실패
    assertThat(pointcut.getClassFilter().matches(Target.class) &&
        pointcut.getMethodMatcher().matches(Target.class.getMethod("plus", int.class, int.class), null)).isFalse();
    // Bean.method() : 클래스 매처에서 실패
    assertThat(pointcut.getClassFilter().matches(Bean.class) &&
        pointcut.getMethodMatcher().matches(Target.class.getMethod("method"), null)).isFalse();
  }

  @Test
  public void pointcut() throws NoSuchMethodException {
    targetClassPointcutMatches("execution(* *(..))", true, true, true, true, true, true);
    targetClassPointcutMatches("execution(* *(int, int))", false, false, true, true, false, false);
    targetClassPointcutMatches("execution(* *..*get.*(..))", true, true, true, true, true, false);
    targetClassPointcutMatches("execution(void *(..))", true, true, false, false, true, true);
  }

  // 타깃 클래스 메서드에 대해 포인트컷 선정여부를 검사하는 헬퍼 메서드
  public void targetClassPointcutMatches(String expression, boolean... expected) throws NoSuchMethodException {
    pointcutMatches(expression, expected[0], Target.class, "hello");
    pointcutMatches(expression, expected[1], Target.class, "hello", String.class);
    pointcutMatches(expression, expected[2], Target.class, "plus", int.class, int.class);
    pointcutMatches(expression, expected[3], Target.class, "minus", int.class, int.class);
    pointcutMatches(expression, expected[4], Target.class, "method");
    pointcutMatches(expression, expected[5], Bean.class, "method");
  }

  // 포인트컷과 메서드를 비교해주는 테스트 헬퍼 메서드
  private void pointcutMatches(String expression, Boolean expected, Class<?> clazz, String methodName, Class<?>... args) throws NoSuchMethodException {
    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
    pointcut.setExpression(expression);
    assertThat(pointcut.getClassFilter().matches(clazz) &&
        pointcut.getMethodMatcher().matches(clazz.getMethod(methodName, args), null)).isEqualTo(expected);
  }
}