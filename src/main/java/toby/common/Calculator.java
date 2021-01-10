package toby.common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Calculator {
  public String concatenate(String filePath) throws IOException {
    LineCallback<String> concatenateCallback = new LineCallback<String>() {
      public String doSomethingWithLine(String line, String value) {
        return value + line;
      }
    };
    return lineReadTemplate(filePath, concatenateCallback, "");
  }
  public Integer calcMultiply(String filePath) throws IOException {
    LineCallback<Integer> sumCallback = new LineCallback<Integer>() {
      public Integer doSomethingWithLine(String line, Integer value) {
        return value * Integer.valueOf(line);
      }
    };
    return lineReadTemplate(filePath, sumCallback, 1);
  }
  public Integer calcSum(String filePath) throws IOException {
    LineCallback<Integer> sumCallback = new LineCallback<Integer>() {
      public Integer doSomethingWithLine(String line, Integer value) {
        return value + Integer.valueOf(line);
      }
    };
    return lineReadTemplate(filePath, sumCallback, 0);
  }
  public <T> T lineReadTemplate(String filePath, LineCallback<T> lineCallback, T initValue) throws IOException {
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(filePath));
      T result = initValue;
      String line = null;
      while ((line = br.readLine()) != null) {
        result = lineCallback.doSomethingWithLine(line, result);
      }
      return result;
    } catch (IOException e) {
      throw e;
    } finally {
      if (br != null) { try { br.close(); } catch (Exception e) { throw e; } }
    }
  }
//  // lineReadTemplate 적용 전
//  public Integer calcMultiply(String filePath) throws IOException {
//    BufferedReaderCallback callback = new BufferedReaderCallback() {
//      @Override
//      public Integer doSomethingWithReader(BufferedReader br) throws IOException {
//        Integer multiply = 1;
//        String line = null;
//        while((line = br.readLine()) != null) {
//          multiply *= Integer.valueOf(line);
//        }
//        return multiply;
//      }
//    };
//    return fileReadTemplate(filePath, callback);
//  }
//  public Integer calcSum(String filePath) throws IOException {
//    BufferedReaderCallback callback = new BufferedReaderCallback() {
//      @Override
//      public Integer doSomethingWithReader(BufferedReader br) throws IOException {
//        Integer sum = 0;
//        String line = null;
//        while((line = br.readLine()) != null) {
//          sum += Integer.valueOf(line);
//        }
//        return sum;
//      }
//    };
//    return fileReadTemplate(filePath, callback);
//  }
//  // 제너릭 적용 전
//  public Integer lineReadTemplate(String filePath, LineCallback lineCallback, int initValue) throws IOException {
//    BufferedReader br = null;
//    try {
//      br = new BufferedReader(new FileReader(filePath));
//      Integer result = initValue;
//      String line = null;
//      while ((line = br.readLine()) != null) {
//        result = lineCallback.doSomethingWithLine(line, result);
//      }
//      return result;
//    } catch (IOException e) {
//      throw e;
//    } finally {
//      if (br != null) { try { br.close(); } catch (Exception e) { throw e; } }
//    }
//  }
  public Integer fileReadTemplate(String filePath, BufferedReaderCallback callback) throws IOException {
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(filePath));
      return callback.doSomethingWithReader(br);
    } catch (IOException e) {
      throw e;
    } finally {
      if (br != null) { try { br.close(); } catch (Exception e) { throw e; } }
    }
  }

//  // 템플릿/콜백 적용 전
//  public static Integer calcSum(String filePath) throws IOException {
//    BufferedReader br = null;
//    try {
//      br = new BufferedReader(new FileReader(filePath));
//      Integer sum = 0;
//      String line = null;
//      while((line = br.readLine()) != null) {
//        sum += Integer.valueOf(line);
//      }
//      return sum;
//    } catch (IOException e) {
//      throw e;
//    } finally {
//      if (br != null) { try { br.close(); } catch (Exception e) { throw e; } }
//    }
//  }
}
