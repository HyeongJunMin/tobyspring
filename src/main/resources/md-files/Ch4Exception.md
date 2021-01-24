#4장. 예외
---

## 요약 및 결론
> 서버개발환경에서는 하나의 요청에 대한 작업에서 예외가 발생하면 그 작업만 끝내고 요청한 클라이언트에게 알려주면 된다.
>
> 예외처리는 이렇게?
> ```
> 1. 예외가 발생했을 때 코드레벨에서 복구할 수 있으면 그렇게 한다. (예외 복구)
> 2. 복구 가능성이 없으면 런타임 예외 방식으로 처리한다.
>   - 적절한 의미를 갖는 예외로 변경해서 알리기(예외 전환)
> ```

## 책 내용
> JdbcTemplate을 적용하고 SQLException이 없어졌다. 왜일까?
>
> 자바에서 예외의 종류와 특징은?
>
> 예외처리 전략은 어떻게 가져갈까?
>
> 스프링은 어떻게 예외를 처리할까? 



### 1. 사라진 SQLException
- 예외는 어떤 종류가 있고 어떻게 다루면 좋을까? 에 대한 내용
- ```
  // JdbcTEmplate 적용 전
  public void deleteAll() throws SQLException {
    this.jdbcContext.executeSql("delete from users");
  }
  // JdbcTEmplate 적용 후
  public void deleteAll() {
    this.jdbcTemplate.update("delete from users");
  }
  ```
1. 초난감 예외처리
    - 예외 블랙홀
        - catch로 잡아내고 그냥 넘어간다면? 비정상 동작, 리소스 소진 등 예상치 못한 다른 문제 발생 가능
        - 모든 예외는 적절하게 복구되거나 운영자 또는 개발자에게 분명하게 통보돼야 한다.
    - 무의미하고 무책임한 throws
        - 어떤 예외가 발생하는지 알 수 없기 때문에 어떻게 대처해야 할 지 알 수 없고, 적절한 대처를 할 수 없다.

2. 예외의 종류와 특징
    - ```
      unchecked exception : 컴파일에 문제 없고 코드를 잘못 만들어서 생기는 예외(NPE)
      checked exception : 컴파일러가 check하는 예외(ClassNotFoundException)
      ```
    - 자바에서 throw를 통해 발생시킬 수 있는 예외
        1. Error
            - 시스템에 뭔가 비정상적인 상황이 발생했을 경우
            - OutOfMemoryError, ThreadDeath등 catch블록으로 잡아봤자 대응 방법이 없음
        2. Exception과 체크 예외
            - 일반적으로 예외라고 하면 Exception 클래스의 서브클래스 중에서 RuntimeException을 상속하지 않은 체크 예외를 지칭
            - 체크 예외 해결방안을 적절히 마련하지 않으면 컴파일 에러 발생
        3. RuntimeException과 언체크/런타임 예외
            - 명시적인 예외처리를 강제하지 않는다.
            - 프로그램에 오류가 있을 때 발생하도록 의도된 것

3. 예외처리 방법
    - 복구할 수 있는 예외는 복구하고, 복구 가능성이 없으면 자세한 내용은 로그로 남기고 관리자에게 통보하는 방식의 처리가 좋다.
    - 예외 복구
        - 예외상황을 파악하고 문제를 해결해서 정상 상태로 돌려놓는 방법
        - 예외를 복구할 가능성이 있는 경우에 사용(ex_ 네트워크 문제로 SQLException -> DB 접속 재시도)
    - 예외처리 회피
        - 호출한 쪽으로 예외를 던지는 방법
        - 긴밀한 관계에 있는 다른 객체에게 예외처리 책임을 분명히 전가해야 한다.
        - 분명한 의도가 없으면 무책임한 책임회피가 될 수 있다.
    - 예외 전환
        - 예외를 메서드 밖으로 던지긴 하되, 적절한 예외로 전환해서 던지는 방법.
        - 예외상황에 대한 적절한 의미를 갖는 예외로 변경해서 던지기(ex SQLException -> DuplicateUserIdException)
        - 쉽고 단순한 예외처리를 위해 포장해서 던지기(checked exception을 unchecked exception으로 바꾸는 경우에 사용)
4. 예외처리 전략
    - 예외처리 전략을 정리해보면...
    - 런타임 예외의 보편화
        - 독립형 애플리케이션(메모장 등)과 달리 서버에서는 사용자와 바로 소통하며 예외상황을 복구할 수 있는 방법이 없다.
        - 서버로 들어온 하나의 요청을 처리하는 중 예외가 발생하면? 해당 요청에 대한 작업만 중단시키면 그만이다.
    - add() 메서드의 예외처리
        - ID중복이 발생하면 상황에 대한 의미를 갖는 예외를 던지도록 add()메서드 수정
        - ```
          // 런타임 예외이기 때문에 throws 없어도 됨
          public void add(final User user) throws DuplicateUserIdException {
            try {
              this.jdbcTemplate.update("insert into users(id, name, password) values(?, ?, ?)"
                      , user.getId(), user.getName(), user.getPassword());
              throw new SQLException("test", "test", DuplicateUserIdException.ERROR_DUPLICATED_ENTRY);
            } catch (SQLException e) {
              // 예외 전환(SQLException -> DuplicateUserIdException)
              if (e.getErrorCode() == DuplicateUserIdException.ERROR_DUPLICATED_ENTRY) {
                throw new DuplicateUserIdException();
              } else {
              // 예외 포장
                throw new RuntimeException(e);
              }
            }
          }
          ```
    - 애플리케이션 예외
        - 애플리케이션 자체의 로직에 의해 의도적으로 발생시키고 반드시 catch 해서 무언가 조치를 취하도록 강제
        - 비즈니스 로직 상 꼭 필요한 제약이 있는 경우
        - 첫 번째 방법 : 상황에 맞는 리턴코드를 정해두고 조건 분기
        - 두 번째 방법 : 상황에 맞는 checked exception을 던지도록 설계
        - ```
          try {
            BigDecimal balance = account.withdraw(amount);
            ...
          // checked exception
          } catch (InsufficientBalanceException e) {
            // InsufficientBalanceException에 담긴 인출 가능한 잔고금액 정보를 가져옴
            Bigdecimal availableFunds = e.getAvailableFunds();
            // 잔고 부족 안내 메시지를 준비하고 이를 출력하도록 진행
          }
          ```

5. SQLException은 어떻게 됐나?
    - 지금까지 다룬 예외처리에 대한 내용은 JdbcTemplate을 적용하는 중에 throws SQLException 선언이 왜 사라졌는가를 설명하는 데 필요한 내용들
    - SQLException은 어떻게됐나요?
        - SQLException은 복구가 가능한가?
            - 대부분의 SQLException은 코드레벨에서 복구할 방법이 없다.
            - 그러므로 개발자에게 빠르게 예외를 전달하도록 대처해야 한다.
    - 그래서 JdbcTemplate은?
        - 템플릿/콜백 안에서 발생하는 모든 SQLException을 DataAccessException으로 포장해서 던져준다.
        - DataAccessException : RuntimeException
        - 꼭 필요한 경우에만 잡아서 쓰면 된다.
### 2. 예외 전환
```
예외 전환의 목적
1. 로우레벨의 예외를 더 의미있고 추상화된 예외로 바꿔서 던져주는 것.
2. 무쓸모한 catch-throw를 줄여주도록 포장
JdbcTemplate의 DataAccessException
1. SQLException보다 더 의미있는 이름
2. 런타임 예외로 SQLException을 포장
```
1. JDBC의 한계
    - JDBC 특
        - JDBC 표준을 따른 DB 별 드라이버를 제공 
        - 하지만 DB를 자유롭게 변경해서 사용할 수 있는 유연한 코드를 보장해주지는 못한다.
        - 아래 2개는 DB를 자유롭게 변경하지 못하는 이유(비표준 SQL, DB 에러정보)
    - 비표준 SQL
        - DB 별 SQL이 다른 부분
        - DAO에 비표준 쿼리가 들어가면 DB 종속적 코드가 된다.
        - DAO를 DB별로 만들거나... SQL을 외부로 독립시켜서 사용하거나... 
    - 호환성 없는 SQLException의 DB 에러정보
        - DB마다 에러의 종류가 원인이 다르다. 
        - 그래서 JDBC에서는 SQLException 하나로 처리한다.
        - 그래도 getErrorCode하면 DB마다 다르다.
        - SQLException만으로 DB에 독립적인 유연한 코드를 작성하는 것은 불가능에 가깝다.
2. DB 에러 코드 매핑을 통한 전환
    - 스프링은 DB별 에러 코드를 분류해서 스프링이 정의한 예외 클래스와 매핑했다.(에러 코드 매핑 파일 301p)
    - JDK 1.6부터 문법오류, 제약조건 위반을 세분화
3. DAO 인터페이스와 DataAccessException 계층구조
    - 왜 스프링은 DataAccessException 계층구조를 이용해 기술에 독립적인 예외를 정의하고 사용하게 할까?
    - 다양한 상황을 하나로 통합하기위해?
    - DAO 인터페이스와 구현의 분리
        - DAO를 굳이 따로 만드는 이유? : 데이터 엑세스 로직을 담은 코드를 성격이 다른 코드들과 분리해 놓기 위해!
        - 인터페이스 선언으로는 불충분하다. 왜? : 데이터 엑세스 기술이 달라지면 같은 상황이라도 다른 종류의 예외가 던져진다. -> 클라이언트가 DAO 기술에 의존적이 될 수 밖에 없다.
    - 데이터 액세스 예외 추상화와 DataAccessException 계층구조
        - 그래서 스프링은 다양한 데이터 엑세스 기술을 사용할 떄 발생하는 예외들을 추상화해서 DataAccessException 계층구조 안에 정리했다.
        - DataAccessException은 JPA, Hibernate, MyBatis 등 자바의 주요 데이터 엑세스 기술에서 발생할 수 있는 대부분의 예외를 추상화하고 있다.
4. 기술에 독립적인 UserDao 만들기
    - 인터페이스 적용
    - 테스트 보완
    - DataAccessException 활용 시 주의사항

### 3. 정리
- 바람직한 예외처리 방법이 무엇인지 살펴본 챕터
- ```
  1. JDBC 예외의 단점이 무엇인지? -> 복구 가능성이 없는데 예외를 던진다. 무의미하다.
  2. 스프링이 제공하는 효과적인 데이터 액세스 기술의 예외처리 전략과 기능은 어땠는지?
  ```
- 예외를 잡아놓고 아무런 조치를 취하지 않거나 의미없는 throws 선언은 위험하다.
    - 복구하거나 예외처리 객체로 의도를 갖고 전달하거나 적절한 예외로 전환해야 한다.
    - 의미가 명확한 예외로 변경하거나 불필요한 catch/throws를 피하기 위해 런타임 예외로 포장하는 방법이 있다.
- 복구할 수 없는 예외는 가능한 한 빨리 런타임 예외로 전환하는 것이 바람직하다.
- 애플리케이션의 로직을 담기 위한 예외는 체크 예외로 만든다.
- JDBC의 SQLException은 대부분 복구할 수 없는 예외이므로 런타임 예외로 포장하는게 좋다.
- SQLException의 에러 코드는 DB에 종속되기 때문에 DB에 독립적인 예외로 전환될 필요가 있다.
- 스프링은 DataAccessException을 통해 DB에 독립적으로 적용 가능한 추상화된 런타임 예외 계층을 제공한다.
- DAO를 데이터 액세스 기술에서 독립시키려면?
- ```
  인터페이스 도입
  런타임 예외 전환
  기술에 독립적인 추상화된 예외로 전환
  ```
- 런타임 예외 중심의 전략
    1. 낙관적인 예외처리 기법이라고 할 수 있다.
    2. 복구할 수 있는 예외는 없다고 가정한다.
    3. 예외가 생겨도 시스템 레벨에서 처리해줄 것으로 기대한다.
    4. 꼭 필요한 경우는 코드를 통해 대응할 수 있다.
- 애플리케이션 예외 중심의 전략
    1. 애플리케이션 자체의 로직에 의해 의도적으로 발생시키고, 반드시 catch 해서 무엇인가 조치를 취하도록 강제하는 예외
