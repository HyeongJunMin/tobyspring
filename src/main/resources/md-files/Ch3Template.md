#3장. 템플릿
---

## 요약 및 결론
> 템플릿/콜백 : 콜백이라는 이름의 의미처럼 다시 불려지는 기능을 만들어서 보낸다.

## 책 내용
> 템플릿 기법 : 변경이 거의 없는 부분을 독립시켜서 활용성을 높이는 방법
>
> 전략 패턴을 활용한 템플릿 기법
> 



### 1. 다시 보는 초난감 DAO
-   지금까지의 DAO에는 예외상황에 대한 처리가 없음
1. 예외처리 기능을 갖춘 DAO
    - 예외가 발생한 경우 사용한 리소스를 반드시 반환하도록 만들어야 한다.(심각한 문제를 일으킬 수 있기 때문)
    - close() 메서드를 통해 자원을 반환하지 않으면 리소스가 모자란다는 오류를 내며 서버가 중단될 수 있음
    - JDBC 수정 기능의 예외처리 코드
    - ```
      // 예외처리 전
      public void deleteAll() throws SQLException {
        Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement("delete from users");
        ps.executeUpdate();
        // 위에서 예외가 발생하면 아래 close메서드가 실행되지 않는다.
        ps.close();
        c.close();
      }
      // 예외처리 후
      public void deleteAll() throws SQLException {
        Connection c = null;
        PreparedStatement ps = null;
          try {
            // 예외발생 가능성이 있는 코드
            c = dataSource.getConnection();
            ps = c.prepareStatement("delete from users");
            ps.executeUpdate();
          } catch (SQLException e) {
            throw e;
          } finally {
            if (ps != null) {
              try {
                // ps.close()에서 예외가 발생하면 아래 c.close()가 실행되지 않기 때문에 예외처리
                ps.close();
              // 예외가 발생하면 로그처리 등
              } catch (SQLException ee) { }
            }
            if (ps != null) {
              try {
                // 그럼 얘는 왜 try-catch?
                c.close();
              } catch (SQLException ee) { }
            }
          }
      }
      ```
    - JDBC 조회 기능의 예외처리
    - ```
      public int getCount() throws SQLException {
        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null
        try {
          c = dataSource.getConnection();
          ps = c.prepareStatement("select count(*) from users");
          rs = ps.executeQuery();
          rs.next();
          return rs.getInt(1);
        } catch (SQLException e) {
          throw e;
        } finally {
          if (rs != null) {
            try {
              rs.close();
            } catch (SQLException ee) { }
          }
          if (ps != null) {
            try {
              ps.close();
            } catch (SQLException ee) { }
          }
          if (c != null) {
            try {
              c.close();
            } catch (SQLException ee) { }
          }
        }
      }
      ```
    
### 2. 변하는 것과 변하지 않는 것
1. JDBC try-catch-finally 코드의 문제점
    - 지저분하고 중복코드가 많다.
    - close() 메서드가 빠질 가능성이 높기 때문에 위험하다.
2. 분리와 재사용을 위한 디자인 패턴 적용
    - 변하는 부분과 변하지 않는 부분 구분
        - 변하는 부분 : 쿼리 수행
        - 변하지 않는 부분 : 예외처리 코드와 close()메서드
    - 방법1. 메소드 추출 : 재사용성에 이득이 없으므로 부적절
    - 방법2. 템플릿 메소드 패턴 적용
        - 상속을 통해 기능을 확장해서 사용
        - 변하지 않는 부분을 슈퍼클래스에 두고 변하는 부분은 추상메소드로 정의
        - 단점이 많아서 부적절
            - DAO 로직마다 새로운 클래스 필요
            - 관계 유연성이 없음
    - 방법3. 전략 패턴의 적용
        - 일정한 구조를 갖는 Context와 특정 확장 기능인 Starategy의 관계
        - Context가 변하지 않는 부분, Strategy가 변하는 부분
        - ```
          // Context가 만들어둔 Connection을 전달받아서 PreparedStatement를 만들고 반환
          public interface StatementStrategy {
            PreparedStatement makePreparedStatement(Connection c) throws SQLException;
          }
          // 실제 젼략
          public class DeleteAllStatement implements StatementStrategy {
            public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
              PreparedStatement ps = c.prepareStatement("delete from users");
              return ps;
            }
          }
          // 실제 전략을 사용하는 부분
          public class UserDao {
            ...
            public void deleteAll() throws SQLException {
            ...
              try {                
                c = dataSource.getConnection();
                // 전략 패턴을 적용했다는건 알겠다.
                // 그런데 장점이 뭔지 전혀 모르겠는데...
                StatementStrategy strategy = new DeleteAllStatement();
                ps = strategy.makePrepraredStatement(c);                
                ps.executeUpdate();
              } catch (SQLException e) {
                throw e;
              }
          }
          ```
        - DI 적용을 위한 클라이언트/컨텍스트 분리
            - 전략 패턴에서 어떤 전략을 사용할것인가에 대한 결정을 내리는 주체는 Client다.
            - ```
              // 컨텍스트를 매개변수로 받아서 컨텍스트가 갖는 쿼리를 수행하는 메서드
              // executeUpdate만 사용하니까 메서드이름을 바꾸는게 좋겠다.
              // executeUpdateWithStrategy()?
              public void jdbcContextWithStatementStrategy(StatementStrategy stmt) throws SQLException {
                Connection c = null;
                PreparesStatement ps = null;
                try {                
                  c = dataSource.getConnection();
                  ps = stmt.makePrepraredStatement(c);                
                  ps.executeUpdate();
                } catch (SQLException e) {
                ...
                } finally {
                if (ps != null) { try { ps.close(); } catch (SQLException ee) { } }
                if (c != null) { try { c.close(); } catch (SQLException ee) { } }
              }
              // 클라이언트 책임을 담당할 deleteAll() 메서드
              public void deleteAll() throws SQLException {
                // 클라이언트가 전략 클래스를 결정하고 객체 생성
                StatementStrategy strategy = new DeleteAllStatement();
                // 컨텍스트를 호출하고 전략 객체 전달
                jdbcContextWithStatementStrategy(strategy);
              }
              ```
3. 마이크로 DI
    - DI의 가장 중요한 개념 : 제3자의 도움을 통해 두 객체 사이의 유연한 관계가 설정되도록 만든다.
    - DI는 매우 작은 단위의 코드와 메소드 사이에서 일어나기도 한다.

### 3. JDBC 전략 패턴의 최적화
- 전략 : 변하는 부분, 바뀌는 로직
- 컨텍스트 : 변하지 않는 부분
1. 전략 클래스의 추가 정보
    - add()메서드에 전략패턴 적용
    - ```
      public class AddStatement implements StatementStrategy {
        User user;
        public AddStatement(User user) {
          this.user = user;
        }
        public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
          PreparedStatement ps = c.prepareStatement("insert into users(id, name, password) values(?, ?, ?)");
          ps.setString(1, user.getId());
          ps.setString(2, user.getName());
          ps.setString(3, user.getPassword());
          return ps;
        }
      }
      // UserDao에서 사용
      public class UserDao {
        ...
        public void add(User user) thrwos SQLException {
          StatementStrategy strategy = new AddStatement(user);
          jdbcContextWithStatementStrategy(st);
        }
        ...
      }
      ```
2. 전략과 클라이언트의 동거
    - 남은 문제들
        1. DAO 메서드 마다 새로운 StatementStrategy 구현 클래스를 만들어야 함
        2. 부가적인 정보가 있을 때 전달만을 위한 객체를 사용해야함
    - 로컬 클래스
        - 클래스 파일이 많아지는 문제를 해결하기 위한 방법
        - UserDao 안에 내부 클래스로 정의하기
        - 클래스 안에 있기 때문에 User 객체를 전달해 줄 필요도 없어졌다.
        - ```
          public void add(final User user) throws SQLException {
            class AddStatement implements StatementStrategy {
              User user;
              public AddStatement(User user) {
                this.user = user;
              }
              public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
                PreparedStatement ps = c.prepareStatement("insert into users(id, name, password) values(?, ?, ?)");
                ps.setString(1, user.getId());
                ps.setString(2, user.getName());
                ps.setString(3, user.getPassword());
                return ps;
              }
            } // 로컬 클래스 끝
            StatementStrategy strategy = new AddStatement(user);
            jdbcContextWithStatementStrategy(strategy);
          }
          ```
    - 익명 내부 클래스
        - 선언과 동시에 객체를 생성
        - ```
          public void add(final User user) throws SQLException {
            StatementStrategy strategy = new AddStatement() {
              public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
                PreparedStatement ps = c.prepareStatement("insert into users(id, name, password) values(?, ?, ?)");
                ps.setString(1, user.getId());
                ps.setString(2, user.getName());
                ps.setString(3, user.getPassword());
                return ps;
              }
            }
            jdbcContextWithStatementStrategy(strategy);
          }
          ```
        - ```
          public void add(final User user) throws SQLException {
            // 메서드 파라미터 위치에서 바로 생성
            jdbcContextWithStatementStrategy(new AddStatement() {
               public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
                 PreparedStatement ps = c.prepareStatement("insert into users(id, name, password) values(?, ?, ?)");
                 ps.setString(1, user.getId());
                 ps.setString(2, user.getName());
                 ps.setString(3, user.getPassword());
                 return ps;
               }
            });
          }
          ```
3. 여기까지 정리
- 예외처리 적용
- 예외처리 중 중복되는 부분에 전략 패턴 적용(변하지 않는 부분 - 변하는 부분)
- 전략 선언 방법을 일반 클래스 -> 내부클래스 -> 익명클래스로 변경 

### 4. 컨텍스트와 DI
1. JdbcContext의 분리
    - 전략패턴의 구조
        - 클라이언트 : UserDao의 메서드
        - 개별적인 전략 : 메서드 안에있는 익명 내부클래스
        - 컨텍스트 : jdbcContextWithStatementStrategy()
    - 이 상태에서 컨텍스트를 다른 DAO에서도 사용할 수 있도록 독립시킨다.
    - 클래스 분리
        - UserDao에 있던 jdbcContextWithStatementStrategy 메서드를 JdbcContext클래스로 분리
        - 그 다음 UserDao에서 jdbcContext를 받아주고
        - 빈 의존관계를 수정해준다
            - 빈 의존관계는 기존에 UserDao와 DataSource 사이에 JdbcContext가 끼도록 바뀐다
            - 그러나 아직 모든 UserDao의 메서드가 JdbcContext를 사용하는게 아니기 때문에 우선 DataSource는 그대로 둔다.(234p)
        - ```
          // JdbcContext
          public class JdbcContext {          
            private DataSource dataSource;
            // DataSource타입 빈을 DI받을 수 있도록 준비
            public void setDataSource(DataSource dataSource) {
              this.dataSource = dataSource;
            }          
            // UserDao에 있던 jdbcContextWithStatementStrategy를 이쪽으로 가져왔음(232p)
            public void workWithStatementStrategy(StatementStrategy strategy) throws SQLException {
              Connection c = null;
              PreparedStatement ps = null;
              try {          
                c = dataSource.getConnection();
                ps = strategy.makePreparedStatement(c);
                ps.executeUpdate();
              } catch (SQLException e) {
                throw e;
              } finally {
                if(ps != null) { try { ps.close() } catch (Exception e) { } }
                if(c != null) { try { c.close() } catch (Exception e) { } }
              }
            }
          }
          ```
          ```
          public class UserDao {
            private DataSource dataSource;
            private JdbcContext jdbcContext;
            public UserDao(DataSource dataSource, JdbcContext jdbcContext) {
              this.dataSource = dataSource;
              this.jdbcContext = jdbcContext;
            }          
            public void add(User user) throws ClassNotFoundException, SQLException {
              this.jdbcContext.workWithStatementStrategy(c -> {
                        PreparedStatement ps = c.prepareStatement("insert into users(id, name, password) values (?, ?, ?)");
                        ps.setString(1, user.getId());
                        ps.setString(2, user.getName());
                        ps.setString(3, user.getPassword());
                        return ps;
              });
            }
            public void deleteAll() throws SQLException {
              this.jdbcContext.workWithStatementStrategy(c -> c.prepareStatement("delete from users"));
            }
          }
          ```
2. JdbcContext의 특별한 DI
    - 예제코드에서 JdbcContext는 인터페이스 없이 DI를 적용했다.
        - 이전까지는 클래스 레벨에서 의존관계가 만들어지지 않도록 인터페이스를 사용했는데?
    - 스프링 빈으로 DI
        - 인터페이스 없이 DI를 적용하는건 문제가 있지 않나? 
        - 요약 : UserDao는 항상 JdbcContext 클래스와 함께 사용돼야 하기 때문에 괜찮다. 그래도 인터페이스 두고싶으면 그래도 된다.
3. 코드를 이용하는 수동 DI
    - jdbcContext 빈 없애고 UserDao에서 직접 jdbcContext 생성해서 사용
    - ```
      public class UserDao {      
        private DataSource dataSource;
        private JdbcContext jdbcContext;      
        public UserDao(DataSource dataSource) {
          this.dataSource = dataSource;
          // 의존 객체 생성 및 주입
          this.jdbcContext = new JdbcContext(dataSource);
        }
      }
      ```
4. 두 방법의 장단점
    - JdbcContext를 빈으로 등록
        - 장점 : 객체 사이의 실제 의존관계가 설정 파일에 명확하게 드러남
        - 단점 : DI 근본 원칙에 부합하지 않는(no interface) 구체적인 클래스와의 관계가 설정에 직접 노출된다.
    - DAO 내부에서 수동으로 DI
        - 장점 : 관계를 외부에 드러내지 않음
        - 단점 : 싱글톤 사용 어려움, DI를 위한 부가적인 코드 필요

### 5. 템플릿과 콜백
> 전략패턴 같은 방식을 스프링에서는 템플릿/콜백 패턴이라고 한다.
>
> 템플릿 : 
> ```
> 어떤 목적을 위해 미리 만들어둔 모양이 있는 틀
> 전략패턴에서의 컨텍스트
> ```
> 콜백 :
> ```
> 실행되는 것을 목적으로 다른 객체의 메서드에 전달되는 객체
> 파라미터로 전달되지만 값을 참조하기 위함이 아니라 특정 로직을 담은 메서드를 실행시키기 위함
> functional object 라고도 함
> 전략패턴에서의 전략
> ``` 
1. 템플릿/콜백의 동작 원리
    - 템플릿/콜백의 특징
        - 콜백은 보통 단일 메서드 인터페이스를 사용한다. (여러 개의 메서드를 갖는 일반적인 인터페이스를 사용할 수 있는 전략 패턴의 전략과 다름)
        - 특정 기능을 위해 한 번 호출되는 경우가 일반적이기 때문
        - 일반적인 흐름
            1. 클라이언트에서 콜백 생성
            2. 템플릿 호출하면서 콜백 전달
            3. 템플릿에서 workflow 시작
            4. 템플릿에서 참조정보 생성
            5. 템플릿에서 콜백 호출하면서 참조정보 전달
            6. 콜백에서 Client final 변수 참조
            7. 콜백에서 작업 수행
            8. 콜백 작업 결과 템플릿으로 전달
            9. 템플릿에서 workflow 진행 및 마무리
            10. 템플릿에서 클라이언트로 작업 결과 전달
    - JdbcContext에 적용된 템플릿/콜백
        - 클라이언트 : UserDao.add()
        - 템플릿 : JdbcContext.workWithStatementStrategy()
        - 콜백 : 익명 StatementStrategy 객체
2. 편리한 콜백의 재활용
    - 콜백의 분리와 재활용
        - 복잡한 익명 내부 클래스의 사용을 최소화
        - ```
          // 익명 내부 클래스를 사용한 클라이언트 코드
          public void deleteAll() throws SQLException {
            jdbcContextWithStatementStrategy(new StatementStrategy() {
              public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
                return c.prepareStatement("delete from users");
              }
            });
          }
          // 변하지 않는 부분을 분리시킨 코드
          public void deleteAll() throws SQLException {
            executeSql("delete from users");
          }
          public void executeSql(final String query) throws SQLException {
            jdbcContextWithStatementStrategy(new StatementStrategy() {
              public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
                return c.prepareStatement(query);
              }
            });
          }
          ```
    - 콜백과 템플릿의 결합
        - executeSql()메서드는 재사용성이 높기 때문에 템플릿으로 옮겨도된다.
        - ```
          // JdbcContext로 옮긴 executeSql 메서드 
          public class JdbcContext {
            ...
            public void executeSql(final String query) throws SQLException {
              workWithStatementStrategy(new StatementStrategy() {
                public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
                  return c.prepareStatement(query);
                }
              });
            }
            public void workWithStatementStrategy(StatementStrategy strategy) throws SQLException {
              ...
            }
          }
          ```
        - ```
          // JdbcContext로 옮긴 executeSql 메서드를 사용하는 deleteAll 메서드
          public class UserDao {
            public void deleteAll() throws SQLException {
              this.jdbcContext.executeSql("delete from users");
            }
          }
          ```
3. 템플릿/콜백의 응용
    - 어떻게 써야될까?
        - 고정된 작업 흐름을 갖고 있으면서 여기저기서 자주 반복되는 코드가 있따면, 중복코드를 분리할 방법을 생각하는 습관을 기르자.
        - 중복된 코드는 메서드로 분리해보고
        - 필요에 따라 바꿔야 한다면 인터페이스를 사이에 두고 분리해서 전략 패턴을 적용하고 DI로 의존관계를 관리하도록 만든다.
        - 거기에 바뀌는 부분이 한 애플리케이션 안에서 동시에 여러종류가 만들어 질 수 있다면 템플릿/콜백 패턴을 적용해보자
    - 테스트와 try/catch/finally
        - 간단한 템플릿/콜백 예제
        - ```
          public class CalcSumTest { //250p
            @Test
            public void sumOfNumbers() throws IOException {
              String filePath = "src\\test\\resources\\numbers.txt";
              Integer result = Calculator.calcSum(filePath);
              assertThat(result.equals(10));
            }
          }
          public class Calculator {
            public static Integer calcSum(String filePath) throws IOException {
              BufferedReader br = null;
              try {
                br = new BufferedReader(new FileReader(filePath));
                Integer sum = 0;
                String line = null;
                while((line = br.readLine()) != null) {
                  sum += Integer.valueOf(line);
                }
                return sum;
              } catch (IOException e) {
                throw e;
              } finally {
                if (br != null) { try { br.close(); } catch (Exception e) { throw e; } }
              }    
            }
          }
          ```
    - 중복의 제거와 템플릿/콜백 설계
        - 반복되는 작업 흐름은 템플릿으로 독립
        - 템플릿과 콜백의 경계를 정하고 템플릿이 콜백에게, 콜백이 템플릿에게 각각 전달하는 내용이 무엇인지 파악하는 것이 중요하다.
        - 구조
            - fileReadTemplate(String filePath, BufferedReaderCallback) : 콜백 객체를 받아서 적절한 시점에 실행
            - BufferedReaderCallback : 각 라인을 읽어서 처리한 후에 최종결과를 템플릿에 전달
        - ```
          // 콜백
          public interface BufferedReaderCallback {
            Integer doSomethingWithReader(BufferedReader br) throws IOException;
          }          
          public class Calculator {
            // BufferedReaderCallback을 사용하는 템플릿 메서드
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
            // 템플릿/콜백을 적용한 calcSum 메서드
            public Integer calcSum(String filePath) throws IOException {
              BufferedReaderCallback callback = new BufferedReaderCallback() {
                @Override
                public Integer doSomethingWithReader(BufferedReader br) throws IOException {
                  Integer sum = 0;
                  String line = null;
                  while((line = br.readLine()) != null) {
                    sum += Integer.valueOf(line);
                  }
                  return sum;
                }
              };
              return fileReadTemplate(filePath, callback);
            }            
          }                    
          ```
    - 템플릿/콜백 재설계
        - 남아있는 중복되는 부분을 정리한다.
        - ```
          // 라인 별 작업을 정리한 콜백 인터페이스
          public interface LineCallback {
            Integer doSomethingWithLine(String line, Integer value);
          }
          public class Calculator {
            // LineCallback을 사용하는 템플릿 메서드
            public Integer lineReadTemplate(String filePath, LineCallback lineCallback, int initValue) throws IOException {
              BufferedReader br = null;
              try {
                br = new BufferedReader(new FileReader(filePath));
                Integer result = initValue;
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
            // lineReadTemplate을 사용하도록 수정한 합, 곱 계산 메서드
            public Integer calcMultiply(String filePath) throws IOException {
              LineCallback sumCallback = new LineCallback() {
                public Integer doSomethingWithLine(String line, Integer value) {
                  return value * Integer.valueOf(line);
                }
              };
              return lineReadTemplate(filePath, sumCallback, 1);
            }
            public Integer calcSum(String filePath) throws IOException {
              LineCallback sumCallback = new LineCallback() {
                public Integer doSomethingWithLine(String line, Integer value) {
                  return value + Integer.valueOf(line);
                }
              };
              return lineReadTemplate(filePath, sumCallback, 0);
            }
          }
          ```
    - 제네릭스를 이용한 콜백 인터페이스
        - 파일을 라인 단위로 처리한 결과의 타입을 다양하게 가져가고 싶으면 제너릭 활용
        - ```
          // 제너릭을 적용한 라인 별 작업을 정리한 콜백 인터페이스
          public interface LineCallback<T> {
            T doSomethingWithLine(String line, T value);
          }
          public class Calculator {
            // 제너릭을 적용한 템플릿 메서드
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
          }
          ```
### 6. 정리
- 
```
1. 
2. 
  > 
3. 
4. 
``` 
- 

콜백 : 다시 불려지는 기능 이라는 의미

### 중첩 클래스의 종류
- 스태틱 클래스(static class)
    - 독립적으로 오브젝트로 만들어질 수 있는 클래스
- 내부 클래스(inner class)
    - 자신이 정의된 클래스와 오브젝트 안에서만 만들어 질 수 있는 클래스
    1. 멤버 내부 클래스 : 멤버 필드처럼 오브젝트 레벨에 정의됨
    2. 로컬 클래스 : 메서드 레벨에 정의됨
    3. 익명 내부 클래스 : 이름을 갖지 않으며 선언된 위치에 따라 범위가 다름