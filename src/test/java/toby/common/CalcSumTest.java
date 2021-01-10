package toby.common;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CalcSumTest {

  private Calculator calculator;
  private String filePath;

  @Before
  public void setUp() {
    calculator = new Calculator();
    filePath = "src\\test\\resources\\numbers.txt";
  }

  @Test
  public void sumOfNumbers() throws IOException {
    assertThat(calculator.calcSum(filePath).equals(10));
  }

  @Test
  public void multiplyOfNumbers() throws IOException {
    assertThat(calculator.calcMultiply(filePath).equals(24));
  }

  @Test
  public void concatenate() throws IOException {
    assertThat("1234".equals(calculator.concatenate(filePath)));
  }
}
