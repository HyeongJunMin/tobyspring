#3장. 템플릿
---

## 요약 및 결론
> 

## 책 내용
> 템플릿 기법 : 변경이 거의 없는 부분을 독립시켜서 활용성을 높이는 방법
>
> 전략 패턴을 활용한 템플릿 기법



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


### 중첩 클래스의 종류
- 스태틱 클래스(static class)
    - 독립적으로 오브젝트로 만들어질 수 있는 클래스
- 내부 클래스(inner class)
    - 자신이 정의된 클래스와 오브젝트 안에서만 만들어 질 수 있는 클래스
    1. 멤버 내부 클래스 : 멤버 필드처럼 오브젝트 레벨에 정의됨
    2. 로컬 클래스 : 메서드 레벨에 정의됨
    3. 익명 내부 클래스 : 이름을 갖지 않으며 선언된 위치에 따라 범위가 다름