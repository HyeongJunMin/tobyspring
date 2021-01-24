#4장. 예외
---

## 요약 및 결론
> 템플릿/콜
    
## 책 내용
> 템
>
> 전
> 



### 1. 사라진 SQLException
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
    - add() 메서드의 예외처리
    - 애플리케이션 예외
5. SQLException은 어떻게 됐나?
### 2. 예외 전환
1. JDBC의 한계
    - 비표준 SQL
    - 호환성 없는 SQLException의 DB 에러정보
2. DB 에러 코드 매핑을 통한 전환
3. DAO 인터페이스와 DataAccessException 계층 구조
    - DAO 인터페이스와 구현의 분리
    - 데이터 엑세스 예외 추상화와 DataAccessException 계층 구조
4. 기술에 독립적인 UserDao 만들기
    - 인터페이스 적용
    - 테스트 보완
    - DataAccessException 활용 시 주의사항
### 3. 정리
- 바람직한 예외처리 방법이 무엇인지 살펴본 챕터
- ```
  1. JDBC 예외의 단점이 무엇인지?
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