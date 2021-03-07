package toby.common;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

public class ReflectionTest {

  @Test
  public void invokeMethod() throws Exception {
    String name = "Spring";
    // length
    int nameLength = name.length();
    Method lengthMethod = String.class.getMethod("length");
    assertThat(lengthMethod.invoke(name)).isEqualTo(nameLength);
    // charAt
    int index = 0;
    char charAt = name.charAt(index);
    Method charAtMethod = String.class.getMethod("charAt", int.class);
    assertThat(charAtMethod.invoke(name, index)).isEqualTo(charAt);
  }
}
