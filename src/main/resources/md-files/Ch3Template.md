#3장. 템플릿
---

## 요약 및 결론
> 템플릿/콜백 : 콜백이라는 이름의 의미처럼 다시 불려지는 기능을 만들어서 보낸다.
>
> 배운 내용 정리
> ```
> - 이 책이 나온 당시에는 JdbcTemplate이 많이 사용됐었나 보다.
> - 예외처리, 자원관리 등 변하지 않는 부분을 메서드(jdbcContextWithStatementStrategy)로 빼고
> - 메서드로 추출한 내용을 다른 클래스에서도 쓸 수 있도록 별도 클래스(JdbcContext)로 분리하는 과정을 보면서
> - 중복된 내용들을 어떻게 제거해서 깔끔하고 재사용하기 좋은 코드를 만든 결과를 확인하고
> - 그 결과를 스프링이 어떻게 제공하는지 봤다.
> - 비록 JdbcTemplate은 사용할 일이 없을 것 같지만, 스프링에 녹아있는 아이디어를 알게됐다.
> ```
    
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

### 6. 스프링의 JdbcTemplate
> 스프링은 다양한 템플릿/콜백 기술을 제공한다.
> 
> 그 중 JdbdTemplate을 사용해본다.
> 
> ```
> public class UserDao {
>   private JdbcTemplate jdbcTemplate;
>   public void setDataSource(DataSource dataSource) {
>     this.jdbcTemplate = new JdbcTemplate(dataSource);
>     this.dataSource = dataSource;
>   }
> ```
1. update()
    - deleteAll()에 적용
    - ```
      public void deleteAll() throws SQLException {
        // PreparedStatementCreator를 활용한 deleteAll()
        this.jdbcTemplate.update(connection -> connection.prepareStatement("delete from users"));
        // this.jdbcTemplate.update(
        //     new PreparedStatementCreator() {
        //       @Override
        //       public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        //         return connection.prepareStatement("delete from users");
        //       }
        //     }
        // );
        // 내장 콜백을 사용하는 update()로 변경한 deleteAll()
        this.jdbcTemplate.update("delete from users");
      }
      ```
    - add()에 적용
    - ```
      public void add(User user) throws ClassNotFoundException, SQLException {
        this.jdbcTemplate.update("insert into users(id, name, password) values(?, ?, ?)"
            , user.getId(), user.getName(), user.getPassword());
      }
      ```
2. queryForInt() - 3.2.2 deprecated
    - 콜백이 2개인 .query() 메서드 활용
        - 첫 번째 콜백(PreparedStatementCreator) : statement 생성
        - 두 번째 콜백(ResultSetExtractor) : ResultSet으로 부터 값 추출(제너릭)
    - ```
      public int getCount() {
        return this.jdbcTemplate.query(new PreparedStatementCreator() {
          @Override
          public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
            return connection.prepareStatement("select count(*) from users");
          }
        }, new ResultSetExtractor<Integer>() {
          @Override
          public Integer extractData(ResultSet resultSet) throws SQLException, DataAccessException {
            resultSet.next();
            return resultSet.getInt(1);
          }
        });
      }
      ```
    - queryForInt() 활용
    - ```
      // queryForInt는 spring 3.2.2에서 deprecated. 현재는 queryForObject 뿐
      public int getCount() {
        return jdbcTemplate.queryForObject("select count(*) from users", Integer.class);
      }
      ```
3. queryForObject()
    - RowMapper : ResultSet의 로우 하나를 매핑하기 위해 사용
    - 한 개의 결과만 얻을 것으로 기대한다.
        - single일까 unique일까?
        - unique!
        - 2개 이상이면? IncorrectResultSizeDataAccessException 발생 하면서 실패
        - 0개면? EmptyResultDataAccessException 발생 하면서 실패
    - ```
      public User get(String id) throws SQLException {
        return jdbcTemplate.queryForObject("select * from users where id = ?",
          // SQL에 바인딩 할 파라미터 값. 가변인자 대신 배열 사용
          new Object[] { id },
          // ResultSet 한 로우의 결과를 오브젝트에 매핑해주는 RowMapper콜백
          new RowMapper<User>() {
            public User mapRow(ResultSet rs, int rowNum) throws SQLException {
              User user = new User();
              user.setId(rs.getString("id"));
              user.setName(rs.getString("name"));
              user.setPassword(rs.getString("password"));
              return user;
            }
          });
      }
      ```
4. query()
    - public \<T> List\<T> query(String sql, RowMapper<T> rowMapper)
    - 기능 정의와 테스트 작성
        - 모든 사용자 정보를 가져오기 위한 getAll은 어떻게?
        - id순으로 정렬해서 가져오도록.
    - 테스트코드
    - ```
      @Test
      public void getAll() throws Exception {
        userDao.deleteAll();
        User user1 = new User("001", "name1", "password1");
        User user2 = new User("002", "name2", "password2");
        User user3 = new User("003", "name3", "password3");
        List<User> originUserList = Arrays.asList(user1, user2, user3);
        for(User user : originUserList) { userDao.add(user); }
             List<User> userList = userDao.getAll();
             assertThat(userList.size()).isEqualTo(originUserList.size());
        for(int i = 0; i < userList.size(); i++) {
          checkSameUser(originUserList.get(i), userList.get(i));
        }
      }
      private void checkSameUser(User originUser, User resultUser) {
        assertThat(originUser.getId()).isEqualTo(resultUser.getId());
        assertThat(originUser.getName()).isEqualTo(resultUser.getName());
        assertThat(originUser.getPassword()).isEqualTo(resultUser.getPassword());
      }
      ```
    - UserDao 코드
    - ```
      public List<User> getAll() {
        return jdbcTemplate.query("select * from users order by id",
          (rs, rowNum) -> {
            User user = new User();
            user.setId(rs.getString("id"));
            user.setName(rs.getString("name"));
            user.setPassword(rs.getString("password"));
            return user;
          });
      }
      ```
    - 테스트 보완
        - 네거티브 테스트 필요
        - 아래처럼 빈 테이블의 결과는 size가 0인 List이다 라고 알 수 있는 테스트가 있으면, getAll메서드가 내부적으로 어떻게 작동하는지 알 필요도 없어지는 장점이 있다.
        - ```
          @Test
          public void getAllFromEmptyTable() throws Exception {
            userDao.deleteAll();
            List<User> userList = userDao.getAll();
            assertThat(userList.size()).isEqualTo(0);
          }
          ```
5. 재사용 가능한 콜백의 분리
    - DI를 위한 코드 정리
        - ```
          // 불필요한 DataSource, JdbcContext 제거
          public class UserDao {        
            private JdbcTemplate jdbcTemplate;        
            public UserDao(DataSource dataSource) {
              this.jdbcTemplate = new JdbcTemplate(dataSource);
          }
          ```
    - 중복 제거
        - get(), getAll()에 RowMapper의 내용이 똑같음
        - 단 두개의 중복이라 해도 언제 어떻게 확장이 필요해질지 모르니 제거하는게 좋다.
        - ```
          // 중복 제거한 rowMapper와 get()메서드
          public User get(String id) {
            return jdbcTemplate.queryForObject("select * from users where id = ?",
                    new Object[] { id },
                    getUserMapper());
          }          
          private RowMapper<User> getUserMapper() {
            return (resultSet, i) -> {
              User user = new User();
              user.setId(resultSet.getString("id"));
              user.setName(resultSet.getString("name"));
              user.setPassword(resultSet.getString("password"));
              return user;
            };
          }
          ```
    - 템플릿/콜백 패턴과 UserDao
        - 템플릿/콜백 패턴과 DI를 이용해 깔끔해진 UserDao 클래스
        - 응집도 높다 : 테이블과 필드정보가 바뀌면 UserDao의 거의 모든 코드가 함께 바뀐다
        - 결합도 낮다 : JDBC API 활용방식, 예외처리, 리소스 반납, DB 연결 등에 대한 책임과 관심은 JdbcTemplate에 있기 때문에 변경이 일어난다 해도 UserDao의 코드에는 영향을 주지 않는다.
        - 추가 개선 사항
            - userMapper를 독립된 빈으로 만들어서 분리한다.
            - SQL문장을 외부 리소스에 담고 읽어와서 사용한다. => 쿼리를 최적화하는 경우에 UserDao 코드에 손 댈 필요 없다. 
        - ```
          public class UserDao {          
            private JdbcTemplate jdbcTemplate;          
            public UserDao(DataSource dataSource) {
              this.jdbcTemplate = new JdbcTemplate(dataSource);
            }          
            public void add(final User user) {
              this.jdbcTemplate.update("insert into users(id, name, password) values(?, ?, ?)"
                  , user.getId(), user.getName(), user.getPassword());
            }          
            public User get(String id) {
              return jdbcTemplate.queryForObject("select * from users where id = ?"
                      ,new Object[] { id }, getUserMapper());
            }
            private RowMapper<User> getUserMapper() {
              return (resultSet, i) -> {
                User user = new User();
                user.setId(resultSet.getString("id"));
                user.setName(resultSet.getString("name"));
                user.setPassword(resultSet.getString("password"));
                return user;
              };
            }
            public User getUserByName(String name) {
              return jdbcTemplate.queryForObject("select * from users where name = ?"
                       , new Object[] { name }, getUserMapper());
            }          
            public void deleteAll() {
              this.jdbcTemplate.update("delete from users");
            }          
            public int getCount() {
              return jdbcTemplate.queryForObject("select count(*) from users", Integer.class);
            }          
            public List<User> getAll() {
              return jdbcTemplate.query("select * from users order by id", getUserMapper());
            }          
          }
          ```


### 7. 정리
- 예외처리와 안전한 리소스 반환을 보장해주는 DAO 코드를 만들었다.
- 객체지향 설계 원리, 디자인 패턴, DI 등을 적ㅇ요해서 깔끔하고 유연하며 단순한 코드로 만들었다.
- JDBC 처럼 예외발생 가능성이 있으며 공유 리소스 반환이 필요한 코드는 반드시 try-catch-finally 블록으로 관리해야 한다.
- 전략 패턴
    - 로직에 반복이 있으면서 그 중 일부(전략)만 바뀌고 일부(컨텍스트)는 바뀌지 않는다면 전략 패턴을 적용한다.
    ```
    1. 한 애플리케이션 안에서 여러 전략을 동적으로 구성하고 사용해야 한다면 컨텍스트를 이용하는 클라이언트 메서드에서 직접 전략을 정의하고 제공하도록 만든다.
    2. 익명 내부 클래스를 사용해서 전략 오브젝트를 구현하면 편리하다.
    3. 컨텍스트가 하나 이상의 클라이언트 객체에서 사용된다면 클래스를 분리해서 공유하도록 만든다.
    4. 컨텍스트는 별도 빈으로 등록해서 DI 받거나 클라이언트 클래스에서 직접 생성해 사용한다.
    ``` 
- 템플릿 콜백 패턴
    - 단일 전략 메서드를 갖는 전략 패턴이면서, 익명 내부 클래스를 사용해서 매번 전략을 새로 만들어 사용하고, 컨텍스트 호출과 동시에 전략 DI를 수행하는 방식
    - 콜백 : 다시 불려지는 기능 이라는 의미
    ```
    1. 콜백 코드에도 일정한 패턴이 반복된다면 콜백을 템플릿에 넣고 재활용 하는 것이 편리하다.
    2. 템플릿과 콜백의 타입이 다양하게 바뀔 수 있다면 제너릭을 이용한다.
    3. 스프링은 JDBC 코드 작성을 위해 JdbcTemplate을 기반으로 하는 다양한 템플릿과 콜백을 제공한다.
    4. 템플릿은 한 번에 하나 이상의 콜백을 사용할 수도 있고, 하나의 콜백을 여러번 호출할 수 있다.
    5. 템플릿/콜백을 설계할 때에는 템플릿과 콜백 사이에 주고받는 정보에 관심을 두어야 한다.
    ```
- 템플릿/콜백은 스프링이 객체지향 설계와 프로그래밍에 얼마나 가치를 두고 있는지를 잘 보여주는 예다.
    - 얼마나 유연하고 변경이 용이하게 만들고자 하는지를 잘 보여주는 예다.
    - 이 챕터에서는 추상화(템플릿), 캡슐화(DI)를 통해서
    - DI에 녹아있는 캡슐화? -> DataSource가 갖는 세세한 정보를 DataSource를 직접 사용하는 UserDao는 알 필요가 없기 때문

### 중첩 클래스의 종류
- 스태틱 클래스(static class)
    - 독립적으로 오브젝트로 만들어질 수 있는 클래스
- 내부 클래스(inner class)
    - 자신이 정의된 클래스와 오브젝트 안에서만 만들어 질 수 있는 클래스
    1. 멤버 내부 클래스 : 멤버 필드처럼 오브젝트 레벨에 정의됨
    2. 로컬 클래스 : 메서드 레벨에 정의됨
    3. 익명 내부 클래스 : 이름을 갖지 않으며 선언된 위치에 따라 범위가 다름