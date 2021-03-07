package toby.common.proxy;

import lombok.AllArgsConstructor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

@AllArgsConstructor
public class UppercaseHandler implements InvocationHandler {
  Object target;
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Object ret = method.invoke(target, args);
    if (ret instanceof String && method.getName().startsWith("say")) {
      return ((String)ret).toUpperCase();
    } else {
      return ret;
    }
  }
}
