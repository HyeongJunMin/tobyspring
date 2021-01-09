#1장. 오브젝트와 의존관계
---

## 요약 및 결론
- IoC가 생겨나게 된 원인부터 차근차근 성장 과정을 지켜본 느낌
- 막연히 'IoC는 스프링의것'이라고 생각이 한계에 닿아 있었는데 뚫림
- 스프링을 공부한다. = DI를 어떻게 활용해야 할지를 공부한다.

> 제어의 역전
- IoC의 요약 : 객체를 생성하고 객체 간 의존관계를 맺는 작업을 개발자가 직접하는 대신 프레임워크가 하는 것
- IoC의 장점 
    1. 프로그램의 수행과 구체적인 구현을 분리시킬 수 있다.(구현이 바뀌는 일이 생겨도 그 객체를 사용하는 프로그램 수행부에서는 변경이 필요 없다.)
    2. 여러가지 구현체들 사이의 변경이 용이하다.
    3. 모듈간 의존성이 낮아진다.
- 제어의 역전 보다는 제어의 전이가 정확하지 않나? 왜 역전이라고 하지?
- 개발자-시스템 관계로 봤을 때는 제어의 역전이라는게 말이 된다.
- 제어 방향의 역전이라는 의미인건가
    - UserDao의 예를 들면, ConnectionMaker의 인스턴스를 능동적으로 결정하던 UserDao가 있었는데,
    - ConnectionMaker 인스턴스 결정을 수동적으로 바꾸게 되면서
    - 내가(UserDao) 인스턴스 종류를 결정(제어)하게 지시했는데, 지시를 받도록 뒤집혔다(역전).
```
inversion 어원
inverse는 'in(inside)'과 'verse(turn)'가 결합된 것이다.
'안을 밖으로 돌리다(turn inside out)'라는 그림에서 '(위치나 관계가)반대의, 도치된, 역함수의, 반대'라는 뜻 
inversion = '전도, 역, 정반대, 도치(법)'

https://dictionary.cambridge.org/ko/%EC%82%AC%EC%A0%84/%EC%98%81%EC%96%B4/inversion
a situation in which something is changed so that it is the opposite of what it was before, or in which something is turned upside down
풀이 : 이전과 반대가 되도록 무언가가 변경되거나 무언가가 거꾸로 뒤집히는 상황
```
참고 : https://starkying.tistory.com/entry/IoC-DI

> 의존관계 주입
- DI 컨테이너에 의해 런타임 시 의존 오브젝트를 사용할 수 있도록 그 레퍼런스를 전달받는 과정이 마치 메서드(생성자)를 통해 DI 컨테이너가 UserDao에게 주입해 주는 것과 같다고 해서 이를 의존관계 주입이라고 부른다.

## 책 내용
> 스프링은 자바를 기반으로 한 기술이다. 
> 
> 객체지향 프로그래밍이 제공하는 폭넓은 혜택을 누릴 수 있도록 기본으로 돌아가자는 것이 스프링의 핵심 철학이다.
>
> 스프링은 객체를 어떻게 효과적으로 설계하고, 구현하고, 개선하는가에 대한 명쾌한 기준을 프레임워크 형태로 제시한다.
>

### 1. 초난감 DAO
> UserDAO 클래스의 문제점을 살펴보고 다양한 방법, 패턴, 원칙 등을 적용해서 개선해 나가는 과정 소개.
> 
> 아래 DAO 코드는 문제가 많은 코드이다.
1. User
    - 사용자 정보를 저장하는 User class
    ```java
    @Getter
    @Setter
    public class User {
      String id;
      String name;
      String password;
    }
    ```
2. UserDAO
    - 사용자 정보를 DB에 넣고 관리할 수 있는 DAO 클래스
    ```java
    public class UserDao {
     public void add(User user) throws ClassNotFoundException, SQLException {
         Class.forName("org.h2.Driver");
         Connection c = DriverManager.getConnection("jdbc:h2:tcp://localhost/~/test", "sa", "");   
         PreparedStatement ps = c.prepareStatement("insert into users(id, name, password) values (?, ?, ?)");
         ps.setString(1, user.getId());
         ps.setString(2, user.getName());
         ps.setString(3, user.getPassword());   
         ps.executeUpdate();   
         ps.close();
         c.close();
       }
       public User get(String id) throws ClassNotFoundException, SQLException {
         Class.forName("org.h2.Driver");
         Connection c = DriverManager.getConnection("jdbc:h2:tcp://localhost/~/test", "sa", "");
   
         PreparedStatement ps = c.prepareStatement("select * from users where id = ?");
         ps.setString(1, id);   
         ResultSet rs = ps.executeQuery();
         rs.next();
   
         User user = new User();
         user.setId(rs.getString("id"));
         user.setName(rs.getString("name"));
         user.setPassword(rs.getString("password"));   
         rs.close();
         ps.close();
         c.close();   
         return user;
       }
    }
    ```
3. main()을 이용한 DAO 테스트 코드
    - 위 DAO가 제대로 동작하는지 확인하기 위한 테스트 코드
    - main 메서드에서 실행하는 방법
    ```
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
      UserDao dao = new UserDao();
      User user = new User();
      user.setId("whiteship");
      user.setName("백기선");
      user.setPassword("married");
      dao.add(user);
      System.out.println(user.getId() + " 등록 성공");
      User user2 = dao.get(user.getId());
      System.out.println(user2.getName());
      System.out.println(user2.getPassword());
      System.out.println(user2.getId() + " 조회 성공");
    }
    ```


### 2. DAO의 분리
> 개선 방법 첫 번째.
1. 관심사의 분리
    - 객체지향의 세계에서는 모든 것이 변한다.
        - 변수나 오브젝트 말고 오브젝트에 대한 설계와 이를 구현한 코드가 변한다는 것.
        - 소프트웨어 개발에서의 끝은 애플리케이션이 더이상 사용되지 않아 폐기처분 될 때이다.
    - 변경이 일어날 때 필요한 작업을 최소화 하는 방법
        - 분리
        - 관심이 같은 것 끼리는 모으고, 다른 것 끼리는 서로 영향을 주지 않도록 분리해야 한다.
2. 커넥션 만들기의 추출
    - 위 UserDao에 관심사항 정의
        1. DB와 연결을 위한 커넥션을 가져오는 방법
        2. SQL문장을 만들고 실행하는 방법
        3. 작업 종료 후 리소스 반환(Statement, Connection)
    - 중복 코드의 메서드 추출
        - 나중에 메서드가 2000개 정도 됏을 때 Connection쪽 수정이 있다면 아래 방법이 훨씬 낫다.
        ```java
        public class UserDao {
          public void add(User user) throws ClassNotFoundException, SQLException {
            Connection c = getConnection();   
            ...
          }
          public User get(String id) throws ClassNotFoundException, SQLException {
            Connection c = getConnection();
            ...
          }
          private Connection getConnection() throws ClassNotFoundException, SQLException {
            Class.forName("org.h2.Driver");
            Connection c = DriverManager.getConnection("jdbc:h2:tcp://localhost/~/test", "sa", "");
            return c;
          }
        }
        ```
3. DB 커넥션 만들기의 독립
    - 한 코드를 DB종류가 다른 두 프로젝트에서 사용하려면???
    - 상속을 통한 확장
        - UserDao에서 getConnection() 메서드 구현 코드를 제거하고 추상 메서드로 제공
        - 아래 예제의 UserDao는 손쉽게 확장된다고 말할 수 있다.
        - 한계점
            - 자바는 다중 상속을 허용하지 않으므로 이미 UserDao가 다른 목적으로 상속을 사용하고 있다면 아래 방법 못씀
            - 관심사가 다른데 긴밀한 결합(상속)을 허용했음 
        ```java
        public abstract class UserDao {
          public void add(User user) throws ClassNotFoundException, SQLException {
            Connection c = getConnection();   
            ...
          }
          public User get(String id) throws ClassNotFoundException, SQLException {
            Connection c = getConnection();
            ...
          }
          private abstract Connection getConnection() throws ClassNotFoundException, SQLException;
        }
        ```
        ```java
        public class NUserDao extends UserDao{
          private Connection getConnection() throws ClassNotFoundException, SQLException {
            // N 회사 DB Connection 생성 코드
          } 
        }
        public class KUserDao extends UserDao{
          private Connection getConnection() throws ClassNotFoundException, SQLException {
            // K 회사 DB Connection 생성 코드
          } 
        }
        ```
    - 위와 같은 방식을 템플릿 메서드 패턴이라고 한다.
        - 슈퍼클래스에 기본적인 로직의 흐름을 만든다.
        - 기능의 일부를 추상 메서드나 오버라이딩 가능한 protected메서드 등으로 만든다.
        - 서브 클래스에서 이런 메서드를 필요에 맞게 구현한다.
    - 팩토리 메서드 패턴 : 서브클래스에서 구체적인 오프젝트 생성 방법을 결정하게 하는 것.


### 3. DAO의 확장
> 추상 클래스를 만들고 서브클래스에서 변화가 필요한 부분을 바꿀 수 있도록 만든 이유는 변화의 성격이 다른 것을 분리해서 서로 영향을 주지 않도록 독립적으로 변경할 수 있기 위함이다.
>
> 성격이 다른 변화들 : DB 연결 방법, 데이터 접근 로직
1. 클래스의 분리
    - 두 관심사를 본격적으로 독립 시키면서 쉽게 확장할 수 있는 방법
    - DB커넥션 생성 기능만을 담당하는 SimpleConnectionMaker 클래스 정의
    - 커넥션이 필요한 곳에서 SimpleConnectionMaker 객체를 생성해서 사용
    - 아래 방식의 문제점
        1. 이미 사용하고 있던 Connection 가져오는 메서드가 많이 있을 때 작업의 양이 너무 커진다.
        2. DB 커넥션을 제공하는 클래스가 어떤 것인지 UserDao가 구체적으로 알고 있어야 한다.
    - 문제의 원인 : 변경 가능성이 있는 정보(DB Connection class)에 대해 너무 많이 알고 있기 때문 = 종속성 높음
    ```java
    public class SimpleConnectionMaker {
      public Connection makeNewConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection("jdbc:h2:tcp://localhost/~/test", "sa", "");
      }
    }
    public class UserDao {
      private SimpleConnectionMaker simpleConnectionMaker;
      public UserDao() {
        simpleConnectionMaker = new SimpleConnectionMaker();
      }
      public void add(User user) throws ClassNotFoundException, SQLException {
        Connection c = simpleConnectionMaker.makeNewConnection();   
        ...
      }
      public User get(String id) throws ClassNotFoundException, SQLException {
        Connection c = simpleConnectionMaker.makeNewConnection();
        ...
      }
    }
    ```
2. 인터페이스의 도입
    - 클래스를 분리하면서도 위 방법의 문제점을 해결할 수 있을까
    - 인터페이스를 활용해서 공통적인 성격을 뽑아내어 이를 따로 분리하는 추상화 작업
    - 개선된 내용
        - DB 접속 방법에 변경이 있어 클래스를 다시 만든다 해도 UserDao의 코드를 고칠 일 없다.
    - 문제점
        - 생성자에 있는 코드는 그대로 남아있다.
        ```
        connectionMaker = new NConnectionMaker();
        ```
    ```java
    public interface ConnectionMaker {
      Connection makeConnection() throws ClassNotFoundException, SQLException;
    }
    public class NConnectionMaker implements ConnectionMaker {
      public Connection makeConnection() throws ClassNotFoundException, SQLException {
         //N 회사의 방식으로 Connection 생성
      }
    }
    // ConnectionMaker를 사용하도록 개선한 UserDao
    public class UserDao {
      private ConnectionMaker connectionMaker;
      public UserDao() {
        connectionMaker = new NConnectionMaker();
      }
      public void add(User user) throws ClassNotFoundException, SQLException {
        Connection c = connectionMaker.makeConnection();   
        ...
      }
      public User get(String id) throws ClassNotFoundException, SQLException {
        Connection c = connectionMaker.makeConnection();
        ...
      }
    }
    ```
3. 관계설정 책임의 분리
    - 두 개의 관심을 분리했는데도 왜 UserDao는 인터페이스 뿐 만 아니라 구체적인 클래스까지 알아야 하나??
        - 이유 : UserDao안에 분리되지 않은 또 다른 관심사항이 존재하기 때문이다.
        - 아직 남은 관심 : UserDao가 사용할 ConnectionMaker의 특정 구현 클래스가 무엇인가? 에 대한 관심
        - 관심을 분리해야 한다.
    - ConnectionMaker를 매개변수로 받는 UserDao 생성자를 만들어 본다.
        - 왜? DB connection 결정을 UserDao를 사용하는 곳(UesrDao의 클라이언트)이 결정하도록
        - 왜? UserDao에 있으면 안되는 관심사항이니까
    - ***뭐가 좋아졌나? DB 커넥션을 가져오는 방법을 어떻게 변경하든 UserDao 코드는 아무런 영향을 받지 않는다.***
    ```java
    public class UserDao {
      ...
      public UserDao(ConnetionMaker connectionMaker) {
        this.connectionMaker = connectionMaker;
      }
      ...
    }
    public class UserDaoTest {
      UserDao userDao = new UserDao(new NConnectionMaker());
      ...
    }
    ```
4. 원칙과 패턴
    - 객체지향 설계와 프로그래밍의 이론을 통해서 관계설정 책임의 분리를 더 체계적으로 살펴본다.
    - 개방 폐쇄 원칙(OCP, Open-Closed Principle)
        - 깔끔한 설계를 위해 적용 가능한 객체지향 설계 원칙 중 하나이다.
        - '클래스나 모듈은 확장에는 열려 있어야 하고 변경에는 닫혀 있어야 한다.'는 내용
        - UserDao는?
            - 확장 : DB 연결 방법이라는 기능을 확장하기 쉽다.
            - 변경 : UserDao 핵심 로직 코드는 위 변화에 영향을 받지 않는다.
    - 높은 응집도와 낮은 결합도(high coherence and low coupling)
        - 소프트웨어 개발의 고전적인 원리
        - 높은 응집도
            - 응집도가 높다? 변화가 일어날 때 모듈의 많은 부분이 함께 바뀐다.
            - UserDao의 경우 : 사용자의 데이터를 처리하는 기능이 여기저기 흩어져 있지 않고 DAO 안에 모여있으므로 응집도가 높다.
        - 낮은 결합도
            - 높은 응집도 보다 더 민감한 원칙
            - 책임과 관심사가 다른 객체는 느슨한 연결을 유지하는 것이 바람직 하다.
            - 느슨한 연결 : 관계를 유지하는 데 꼭 필요한 방법만 간접적으로 제공하고, 나머지는 알 필요도 없게 만드는 것
            - 결합도 : 하나의 객체에 변경이 일어날 때 관계를 맺고 있는 다른 객체에게 변화를 요구하는 정도
    - 전략 패턴(Strategy Pattern)
        - 자신의 기능 맥락에서 필요에 따라 변경이 필요한 알고리즘을 인터페이스를 통해 통째로 외부로 분리 시키고, 이를 구현한 구체적인 알고리즘 클래스를 필요에 따라 바꿔서 사용할 수 있게 하는 디자인 패턴
        - 독립된 책임으로 분리된 클래스를 필요에 따라 바꿔서 사용하는 패턴
        - 추상화?


### 4. 제어의 역전(IoC)
1. 오브젝트 팩토리
    - UserDaoTest가 ConnectionMaker의 종류를 결정하고 있다.
    - UserDaoTest가 할 일이 아니니까 분리해야 맞다.
    - 그래서 제어의 역전이 어떻게 적용된건가?
        - 클라이언트(UserDaoTest)가 어떤 ConnectionMaker 객체를 생성할 것인지 선택(제어)하고 있었다.
        - DaoFactory를 만들면서 더이상 클라이언트는 어떤 ConnectionMaker를 사용할 것인지 선택하지 않는다.
        - ConnectionMaker 결정을 제어하는 주체는 DaoFactory가 됐다.
        - 지금은 스프링이 
    ```java
    public class DaoFactory {
      public UserDao userDao() {
        ConnectionMaker connectionMaker = new DConnectionMaker();
        UserDao userDao = new UserDao(connectionMaker);
        return userDao;
      }
    }
    public class UserDaoTest {
      // 이전 코드 : UserDao userDao = new UserDao(new NConnectionMaker());
      UserDao userDao = new DaoFactory().userDao();
      ...
    }
    ```
2. 오브젝트 팩토리의 활용
    - DaoFactory에 UserDao 말고 다른DAO 생성 기능을 넣으면 어떻게 되죠?? -> 메서드 마다 ConnectionMaker객체 만들어야 됨
    - 중복 문제가 있으니까 분리해내면 되겠지?
    ```java
    public class DaoFactory {
      public UserDao userDao() {
        return new UserDao(connectionMaker());
      }
      public AccountDao accountDao() {
        return new AccountDao(connectionMaker());
      }
      public ConnectionMaker connectionMaker() {
        return new DConnectionMaker();
      }
    }
    ```
3. 제어권의 이전을 통한 제어관계 역전
    - 제어의 역전? : 프로그램의 제어 흐름 구조가 뒤바뀌는 것
        - 일반적 흐름 : 시작 지점에서 다음 사용할 객체를 결정하고, 객체를 생성하고, 객체의 메서드를 호출하는 식
        - 역전 : 시작 지점(UserDao)에서 사용할 객체를 받아들임 
            - UserDao가 DConnection, NConnection등을 결정해야 하던 상태에서, 생성자로 받아 들이도록 결정 권한이 역전됨
            - 예시 : 서블릿. 서블릿은 직접 실행시키는게 아니고 서블릿 컨테이너가 서블릿을 제어
    - 라이브러리와 프레임워크의 차이
        - 라이브러리 : 라이브러리는 애플리케이션 흐름을 직접 제어
        - 프레임워크 : 애플리케이션 코드가 프레임워크에 의해 사용된다. 프레임워크가 흐름을 주도하는 중에 개발자가 만든 애플리케이션 코드를 사용하도록 만드는 방식이다.


### 5. 스프링의 IoC
> 스프링의 핵심을 담당하는 건 ApplicationContext(Bean Factory)이다.
1. 오브젝트 팩토리를 이용한 IoC
    - 애플리케이션 컨텍스트와 설정정보
        - Bean : 스프링이 제어권을 가지고, 직접 만들고, 관계를 부여하는 오브젝트
            - 자바빈과 비슷한 오브젝트 단위의 애플리케이션 컴포넌트
        - Bean Factory : 빈의 생성과 관계설정 같은 제어를 담당하는 IoC 오브젝트
        - Application Context : 빈 팩토리를 IoC방식을 따라 더 확장한 IoC 엔진
        - Application Context는 별도로 설정정보를 담고 있는 무언가를 가져와 활용하는 IoC 엔진이다.
        - 설정정보를 만드는 방법은 아주 여러가지가 있다.
    - DaoFactory를 사용하는 애플리케이션 컨텍스트
        - DaoFactory를 빈 팩토리가 사용할 수 있는 본격적인 설정정보로 만들어 본다.
        - 그 다음 그 설정정보를 사용하는 애플리케이션 컨텍스트를 선언해본다.
        ```java
        import org.springframework.context.ApplicationContext;import org.springframework.context.annotation.AnnotationConfigApplicationContext;import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;
        // 스프링이 빈 팩토리를 위한 오브젝트 설정을 담당하는 클래스라고 인식할 수 있게 해주는 어노테이션
        @Configuration 
        public class DaoFactory {
          // 오브젝트 생성을 담당하는 IoC용 메서드라는 표시
          // 메서드 이름은 Bean의 이름이 된다.
          @Bean
          public UserDao userDao() {
            return new UserDao(connectionMaker());
          }
          @Bean
          public ConnectionMaker connectionMaker() {
            return new DConnectionMaker();
          }
        }
        public class UserDaoTest {
          @Test
          public UserDaoTest() {
            ApplicationContext context
              = new AnnotationConfigApplicationContext(DaoFactory.class);
            UserDao userDao = context.getBean("userDao", UserDao.class);
          }
        }
        ```
2. 애플리케이션 컨텍스트의 동작방식
    - 애플리케이션 컨텍스트가 뭔가요
    ```
      1. 오브젝트 팩토리에 대응되는 것이 스프링의 애플리케이션 컨텍스트이다.
        - 애플리케이션 컨텍스트는 ApplicationContext 인터페이스를 구현하는데(? 뭔솔)
        - ApplicationContext는 BeanFactory는 상속받앗음(ListableBeanFactory)
        - 그러니까 애플리케이션 컨텍스트는 빈 팩토리인 셈
      2. 애플리케이션에서 IoC를 적용해서 관리할 모든 오브젝트에 대한 생성과 관계설정을 담당한다.
      3. 직접 객체 생성하는게 아니고 생성정보와 연관관계 정보를 별도의 설정정보를 통해 얻는다.
      4. 때로는 외부 오브젝트 팩토리에 그 작업을 위임하고 그 결과를 가져다가 사용하기도 한다.
    ```
    - 애플리케이션 컨텍스트를 사용했을 때의 장점 (DaoFactory를 오브젝트 팩토리로 직접 사용했을 때와 비교해서)
    ```
      1. 클라이언트는 구체적인 팩토리 클래스를 알 필요가 없다.
        - 애플리케이션이 커지면 IoC를 적용한 오브젝트도 많아지겠지
        - 오브젝트가 많아지면서 알아야 될 팩토리 클래스의 수는 늘어나겠지
        - 그리고 필요할 때 마다 팩토리 클래스 생성해야겠지 = 귀찮
      2. 애플리케이션 컨텍스트는 종합 IoC 서비스를 제공해 준다.
        - 다양한 제어 기능 제공 : 오브젝트 생성 방식-시점-전략 각각 다르게 결정, 자동생성, 오브젝트에 대한 후처리, 정보의 조합, 설정 방식의 다변화 등
        - 컨테이너 차원에서 빈이 사용할 수 있는 기반기술 서비스나 외부 시스템과의 연동 지원
      3. 애플리케이션 컨텍스트는 빈을 검색하는 다양한 방법을 제공한다. 
        - 이름으로 검색, 타입으로 검색, 애노테이션으로 검색 등
    ```
3. 스프링 IoC의 용어 정리
    - 빈(bean) : 스프링이 IoC방식으로 생성과 제어를 담당하여 관리하는 오브젝트
    - 빈 팩토리(bean factory) : 스프링의 IoC를 담당하는 핵심 컨테이너.
    ```
      1. 빈을 등록하고, 생성하고, 조회하고, 돌려주고 그 외에 부가적인 빈을 관리하는 기능
      2. 보통은 빈 팩토리를 확장한 애플리케이션 컨텍스트를 이용하게 된다.
    ```    
    - 애플리케이션 컨텍스트(application context) : 빈 팩토리를 확장한 IoC 컨테이너
    ```
      1. 빈을 등록하고 관리하는 기본적인 기능은 빈 팩토리와 동일
      2. 스프링이 제공하는 각종 부가 서비스를 추가로 제공
    ```
    - 설정정보/설정 메타정보(configuration metadata) : 빈 팩토리가 IoC를 적용하기 위해 사용하는 메타정보
    ```
      IoC 컨테이너에 의해 관리되는 애플리케이션 오브젝트를 생성하고 구성하는데 사용됨
    ```
    - 컨테이너(container) 또는 IoC 컨테이너 : IoC 방식으로 빈을 관리한다는 의미로 애플리케이션 컨텍스트나 빈 팩토리를 지칭하는 말
    ```
      1. 컨테이너라는 말 자체가 IoC의 개념을 담고 있기 때문에 이름이 긴 애플리케이션 컨텍스트 대신 스프링 컨테이너 라고 이야기한다.
      2. '스프링에 빈을 등록하고'라는 식으로 말할 때 스프링이 컨테이너=애플리케이션컨텍스트
    ```
    - 스프링 프레임워크 : 스프링이 제공하는 모든 기능을 통틀어 말할 때

### 6. 싱글톤 레지스트리와 오브젝트 스코프
> 동일성(==)과 동등성(equals())
> 
> DaoFactory의 userDao()메서드를 호출했을 때 동일한 오브젝트가 돌아오는가?(동일성) -> 아뇨 -> 매번 new 하니까
>
> 스프링 컨텍스트에서 getBean("userDao", UserDao.class)메서드 호출하면? -> 동일한 오브젝트 리턴함 -> 왜그럴까
1. 싱글톤 레지스트리로서의 애플리케이션 컨텍스트
    - 애플리케이션 컨텍스트는 이전 예제에서 만들었던 오브젝트 팩토리와 비슷한 방식으로 동작하는 IoC 컨테이너다.
    - 동시에 싱글톤을 저장하고 관리하는 싱글톤 레지스트리이기도 하다.
    - 스프링은 기본적으로 별다른 설정을 하지 않으면 내부에서 생성하는 빈 오브젝트를 모두 싱글톤으로 만든다.
    - 서버 애플리케이션과 싱글톤
        - 왜 빈을 싱글톤으로 만들죠? -> 스프링이 주로 적용되는 대상이 자바 엔터프라이즈 기술을 사용하는 서버환경이기 때문
        - 요청이 올 때 마다 객체를 만들게되면..? ... 끔찍
    - 싱글톤 패턴의 한계
        ```java
        // 싱글톤 예제
        public class MySingleton {
          private static MySingleton mySingleton;
          private MySingleton() { }
          public static synchronized getInstance() {
            if (mySingleton == null) mySingleton = new MySingleton();
            return mySingleton;
          }
        }
        ```
        1. 생성자가 private이기 때문에 상속할 수 없다.
            - 서비스만 제공하면 상관없을 수 있지만 일반 오브젝트의 경우 객체지향적 설계의 장점을 적용하기 어렵다.
        2. 싱글톤은 테스트하기가 힘들다.
            - 만들어지는 방식이 제한적이기 때문에 mock객체 등으로 대체하기 힘들다.
        3. 서버환경에서는 싱글톤이 하나만 만들어지는 것을 보장하지 못한다.
            - 서버의 클래스 로더 구성에 따라 싱글톤 클래스임에도 둘 이상의 객체가 생성될 수 있다.
            - 여러 JVM에 분산된 경우에도 독립적으로 객체가 생기기 때문에 싱글톤으로서의 가치가 떨어진다.
        4. 싱글톤의 사용은 전역 상태를 만들 수 있기 때문에 바람직하지 못하다.
            - 싱글톤은 사용하는 클라이언트가 정해져있지 않다 -> 전역 상태로 사용되기 쉽다
            - 전역 상태를 갖는 것은 객체지향 프로그래밍에서는 권장되지 않는 프로그래밍 모델이다. -> 어디선가 객체의 상태를 바꿀 수 있기 때문에
    - 싱글톤 레지스트리
        - 자바의 기본적인 싱글톤 패턴 구현방식은 여러가지 단점이 있지요
        - 그래서 스프링은 직접 싱글톤 형태의 객체를 만들고 관리하는 기능인 싱글톤 레지스트리를 제공한다.
        - 장점 : 평범한 자바클래스를 싱클톤으로 활용하게끔 해준다.
            - 그래서 public 생성자를 가질 수 있는 싱글톤 객체가 나오지 -> 테스트 제약 없음
            - 객체지향 설계방식, 디자인패턴을 적용하는데 아무 제약이 없음

2. 싱글톤과 오브젝트의 상태
    - 싱글톤은 여러 스레드가 동시에 접근해서 사용할 수 있기 때문에 무상태 방식으로 만들어야 한다.
    - 인스턴스 변수를 가지면 값이 바뀌면서 문제가 발생할 수 있다. -> 읽기전용이라면 OK

3. 스프링 빈의 스코프
    - 싱글톤 스코프 : 스프링 빈의 기본 스코프
        - 컨테이너 내에 하나의 객체만 만들어 져서, 강제로 제거하지 않는 한 스프링 컨테이너가 존재하는 동안 계속 유지 됨
    - 프로토타입 스코프 : 컨테이너에 빈을 요청할 때 마다 매 번 새로운 객체를 생성해줌
    - 요청 스코프 : 새로운 HTTP 요청이 생길 때 마다 생성되는 스코프
    - 세션 스코프 : 웹의 세션과 스코프가 유사
    - 그 외에 다양하게 있음

### 7. 의존관계 주입(Dependency Injection, DI)
> 지금까지는 스프링을 IoC 컨테이너로 적용하는 방법과 싱글톤 저장소로서의 특징을 살펴봤다.
>
> 스프링 IoC에 대해 좀 더 깊이 알아본다.
1. 제어의 역전(IoC)과 의존관계 주입
    - IoC는 소프트웨어에서 자주 발견할 수 있는 일반적인 개념이라고 설명했다. -> 폭 넓은 의미
    - 의존관계 주입 : 스프링이 제공하는 IoC 방식의 핵심을 짚어주는 의도가 명확히 드러나는 용어
        - 스프링이 여타 프레임워크와 차별화돼서 제공해주는 기능임을 분명하게 드러냄
        - 핵심 : 오브젝트 레퍼런스를 외부로부터 제공(주입)받고 이를 통해 여타 오브젝트와 다이나믹하게 의존관계가 만들어지는 것
2. 런타임 의존관계 설정
    - 의존관계
        - 의존한다? : 의존당하는 있는 대상이 변하면 의존을 하는 객체에도 영향을 미친다.
        - 의존관계에는 방향성이 있다.
    - UserDao의 의존관계
        - 이전까지 만든 예제에서는 UserDao가 ConnectionMaker에 의존하고 있다.
        - 그러므로 ConnectionMaker 인터페이스가 변하면 그 영향을 UserDao가 직접적으로 받게 된다.
        - 하지만 ConnectionMaker 인터페이스를 구현한 클래스의 변경은 UserDao에 영향을 주지 않는다.
    - 의존관계 주입
        - 런타임 시에 구체적인 의존 오브젝트와 그것을 사용할 주체를 연결해 주는 작업
        - 아래 세 조건을 충족하는 작
        ```
          1. 클래스 모델이나 코드에는 런타임 시점의 의존관계가 드러나지 않는다.
          2. 런타임 시점의 의존관계는 컨테이너나 팩토리 같은 제3의 존재가 결정한다.
          3. 의존관계는 사용할 오브젝트에 대한 레퍼런스를 외부에서 제공(주입)해줌으로써 만들어진다.
        ```
        - 핵심은 설계 시점에는 알지 못했던 두 오브젝트의 관계를 맺도록 도와주는 제3의 존재가 있다는 것
    - UserDao의 의존관계 주입
        - 관계설정의 책임을 분리하기 전의 UserDao                        
            ```java
            public class UserDao {
              connectionMaker = new DConnectionMaker();
            }
            ```
            - 문제점 : 런타임 시의 의존관계가 코드 속에 미리 결정된 것(UserDao는 설계 시점에서 구체적인 클래스를 알고 있어야 한다.)
            - 개선방향 : 
                - IoC방식으로 UserDao로 부터 런타임 의존관계를 드러내는 코드 제거
                - 제3의 존재에게 런타임 의존관계 결정 권한을 위임
        - 의존관계 주입을 위한 코드
            - 두 오브젝트 간 런타임 의존관계가 만들어 진다
            - DI 컨테이너(예제에서는 DaoFactory)는 UserDao를 만드는 시점에서 DConnectionMaker를 주입해줌
            - 근데 이것도 똑같은거 아닌가; 싶었는데 설계시점에는 몰라도 되는거니까 ok
            ```java
            public class UserDao {
              private ConnectionMaker connectionMaker;
              public UserDao(ConnectionMaker connectionMaker) {
                this.connectionMaker = connectionMaker;
              }
            }
            @Configuration 
            public class DaoFactory {
              @Bean
              public UserDao userDao() {
                return new UserDao(connectionMaker());
              }
              @Bean
              public ConnectionMaker connectionMaker() {
                return new DConnectionMaker();
              }
            }
            ```
3. 의존관계 검색(dependency lookup, DL)과 주입
    - 의존관계를 맺는 방법이 외부로부터의 주입이 아니라 스스로 검색을 이용하기 때문에 의존관계 검색이라고 불린다.
    - 의존관계를 맺을오브젝트를 가져올 때는 스스로 컨테이너에게 요청하는 방법 사용
    - 의존관계를 맺을 오브젝트를 결정하는 것과 오브젝트의 생성작업은 외부 컨테이너에게 IoC로 맡김
    - 예시
        - DaoFactory를 이용하는 UserDao 생성자
            - 이렇게 생성자를 설정해도 UserDao는 여전히 어떤 ConnectionMaker를 사용할지 미리 알지 못한다.
            - 의존대상은 ConnectionMaker 인터페이스
            - 그러나 외부로부터 주입받는게 아니고 스스로 IoC 컨테이너인 DaoFactory에 요청하는 형식임
            ```java
            public class UserDao() {
              public UserDao() {
                this.connectionMaker = daoFactory.connectionMaker();
              }
            }
            ```
        - 의존관계 검색을 이용하는 UserDao 생성자
            - 의존관계 검색은 기존 의존관계 주입의 거의 모든 장점을 갖고 있으며, IoC원칙에도 잘 들어맞는다.
            ```java
            public class UserDao() {
              public UserDao() {
                ApplicationContext context
                  = new AnnotationConfigApplicationContext(DaoFactory.class);
                this.connectionMaker = context.getBean("connectionMaker", ConnectionMaker.class);
              }
            }
            ```
    - 의존관계 검색과 주입 중 어떤게 더 나은가?
        - 의존관계 주입 쪽이 더 단순하고 깔끔하다.
            - 의존관계 검색은 코드 안에 불필요한 부분(오브젝트 팩토리 클래스, 스프링 API)이 드러나기 때문에 성격이 다른 오브젝트에 의존하게 되는 것이므로 바람직하지 않다.
    - 의존관계 검색이 필요할 때는?
        - 기동메서드 main()에서 -> DI를 이용해 오브젝트를 주입받을 방법이 없기 때문
        - 서버에서 사용자의 요청을 받을 때 마다 main()과 유사한 역할을 하는 서블릿의 경우 -> 스프링이 제공해주니까 직접 구현할 필요없음
    - 의존관계 검색 방식에서 검색하는 오브젝트는 스프링의 빈일 필요가 없다.
        - 예제에서는 UserDao의 경우(ConnectionMaker만 빈이면 된다)
    - 의존관계 주입 방식에서는 모두 빈 오브젝트여야 한다.
        - 컨테이너가 UserDao에 ConnectionMaker를 주입해 주려면 UserDao에 대한 생성과 초기화 권한을 가져야한다.
        - 그러려면 UserDao는 IoC방식으로 컨테이너에서 생성되는 오브젝트인 스프링빈이어야 한다.
4. 의존관계 주입의 응용
    - DI : 런타임 시에 사용 의존관계를 맺을 오브젝트를 주입
        1. 코드에는 런타임 클래스에 대한 의존관계가 나타나지 않는다.
        2. 인터페이스를 통해 결합도가 낮은 코드를 만들기 때문에 의존관계에 있는 대상이 변경돼도 영향받지 않음
        3. 변경을 통한 다양한 확장 방법에 자유롭다.
    - 기능 구현의 교환(응용1)
        - DB예제 
            - DI방식 없이 로컬 DB와 운영 DB를 관리한다면? 매 번 커넥션 수정 필요
            - DI를 적용한다면???
        - 개발용 ConnectionMaker 생성 코드
            ```java
            @Configuration
            public class DBConfig {
              @Bean
              public ConnectionMaker connectionMaker() {
                return new LocalDBConnectionMaker();
              }
            }
            ```
        - 운영용 ConnectionMaker 생성 코드
            ```java
            @Configuration
            public class DBConfig {
              @Bean
              public ConnectionMaker connectionMaker() {
                return new ProductionDBConnectionMaker();
              }
            }
            ```
    - 부가기능 추가(응용2)
        - DAO가 DB를 얼마나 많이 연결해서 사용하는지 파악하고 싶은 경우
        - DAO와 DB커넥션을 만드는 오브젝트 사이에 연결횟수를카운팅하는 오브젝트를 추가하면 됨
        - 예제
            - 장점 : 예제에서 모든 DAO가 직접 의존해서 사용할 ConnectionMaker 타입 오브젝트는 connectionMaker()메서드로 정해지기 때문에 CountingConnectionMaker의 의존관계를 추가하려면 이 메서드만 수정하면 된다.
            - 의존관계 주입의 매력을 잘 드러내는 응용 방법이라고 할 수 있음
            ```java
            // 연결횟수 카운팅기능이 있는 클래스
            public class CountingConnectionMaker implements ConnectionMaker {
              int count = 0;
              private ConnectionMaker realConnectionMaker;
              public CountingConnectionMaker(ConnectionMaker realConnectionMaker) {
                this.realConnectionMaker = realConnectionMaker;
              }
              public Connection makeConnection() throws Exception {
                this.count++;
                return realConnectionMaker.makeConnection();
              }
              public int getCount() { return this.count; }
            }
            ```
            ```java
            // CountingConnectionMaker 의존관계가 추가된 DI 설정용 클래스
            // DConnectionMaker 객체를 사용하도록 설정
            @Configuration
            public class CountingDaoFactory {
              @Bean
              public UserDao userDao() {
                return new UserDao(connectinoMaker());
              }
              @Bean
              public ConnectionMaker connectionMaker() {
                return new CountingConnectionMaker(realConnectionMaker());
              }
              @Bean
              public ConnectionMaker realConnectionMaker() {
                return new DConnectionMaker();
              }
            }
            ```
            ```java
            // CountingConnectionMaker 테스트 클래스
            public class UserDaoConnectionCountingTest {
              public static void main(String[] args) throws ClassNotFoundException, SQLException {
                AnnotationConfigApplicationContext context 
                  = new AnnotationConfigApplicationContext(CountingDaoFactory.class);
                UserDao dao = context.getBean("userDao", UserDao.class);
                CountingConnectionMaker connectionMaker = context.getBean("connectionMaker", CountingConnectionMaker.class);
                System.out.println("Connection count :" + connectionMaker.getCount());
              }
            }
            ```
5. 메서드를 이용한 의존관계 주입
    - 지금까지 예제에서는 의존관계 주입을 위해 생성자를 사용했다.
    - 생성자가 아닌일반 메서드를 이용해 관계를 주입하는 방법
    1. 일반 메서드를 이용한 주입
    2. 수정자 메서드(setter)를 이용한 주입
        - 메서드는 항상 set으로 시작한다.
        - DI방식에서 활용하기에 적당함
        - 스프링이 전통적으로 많이 사용해온 방식
        ```java
        public class UserDao() {
          private ConnectionMaker connectionMaker;
          public setConnectionMaker(ConnectionMaker connectionMaker) {
            this.connectionMaker = connectionMaker;
          }
        }
        ```
        ```java
        // 수정자 메서드 DI를 사용하는 팩토리 메서드
        @Configuration
        public class DaoFactory {
          @Bean
          public UserDao userDao() {
            UserDao userDao = new UserDao();
            userDao.setConnectionMaker(connectionMaker());
            return userDao;
          }
        }
        ```

### 8. XML을 이용한 설정
> XML로 DI 의존관계 설정정보를 만들 수 있다.
>
> 오브젝트 사이의 의존정보를 일일이 자바 코드로 만들어주는 일은 번거롭다고 한다.
>
> 장점 :
> 1. 단순 텍스트파일이기 때문에 다루기 쉽다
> 2. 쉽게 이해할 수 있다.
> 3. 컴파일 등 별도의 빌드 작업이 없다.
> 4. 오브젝트의 관계가 바뀌는 경우에도 빠르게 변경사항을 반영할 수 있다.
> 5. 스키마나 DTD를 이용해서 정해진 포맷에 따라 작성됐는지 손쉽게 확인할 수도 있다.
>
> 나는 자바 설정이 편하던데 ㅎㅎ

1. XML 설정
    - 어노테이션에 대응하는 XML 태그
        - @Configuration -> <beans>
        - @Bean -> <bean>
    - connectionMaker 전환
    ```
    @Configuration
    public class DaoFactory {
      @Bean
      public ConnectionMaker connectionMaker() {
        return new CountingConnectionMaker(realConnectionMaker());
      }
    }
    // 위 자바설정 XML로 변환
    <beans>
      <bean id="connectionMaker" class="a.b.c.CountingConnectionMaker">
    </beans>
    ```
    - userDao() 전환
        - <property>태그 : 보통 ref와 name이 같은 경우가 많다고 함
            1. name : DI에 사용할 수정자 메서드의 프로퍼티 이름
            2. ref : 주입할 오브젝트를 정의한 빈의 ID            
    ```
    @Configuration
    public class DaoFactory {
      @Bean
      public UserDao userDao() {
        UserDao userDao = new UserDao();
        userDao.setConnectionMaker(connectionMaker());
        return userDao;
      }
    }
    // 위 자바설정 XML로 변환
    <beans>
      <bean id="userDao" class="a.b.c.UserDao">
        <property name="connectionMaker" ref="connectionMaker"/>
      </bean>
    </beans>
    ```
   - 만약 connectionMaker가 myConnectionMaker로 바뀌면?
    ```
    <beans>
      <bean id="myConnectionMaker" class="a.b.c.CountingConnectionMaker"/>
      <bean id="userDao" class="a.b.c.UserDao">
        <property name="connectionMaker" ref="myConnectionMaker"/>
      </bean>
    </beans>
    ```
   - 여러 개의 의존 오브젝트를 만들고 골라서 사용하는 경우
    ```
    <beans>
      <bean id="localConnectionMaker" class="a.b.c.CountingConnectionMaker"/>
      <bean id="devConnectionMaker" class="a.b.c.CountingConnectionMaker"/>
      <bean id="realConnectionMaker" class="a.b.c.CountingConnectionMaker"/>
      <bean id="userDao" class="a.b.c.UserDao">
        <property name="connectionMaker" ref="~ConnectionMaker"/>
      </bean>
    </beans>
    ```

2. XML을 이용하는 애플리케이션 컨텍스트
    - 애플리케이션 컨텍스트가 DaoFactory 대신 XML 설정정보를 활용하도록 만드는 방법
    ```xml
    <!-- applicationContext.xml -->
    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.springframework.org/schema/beans
                               http://www.springframework.org/schema/beans/spring-beans-3.0.xsd">
      <bean id="connectionMaker" class="package.MyConnectionMaker" />
      <bean id="userDao" class="springbook.user.dao.UserDao">
      	<property name="connectionMaker" ref="connectionMaker" />
      </bean>
    </beans>
    ```
    - ApplicationContext 설정
    ```java
    import org.springframework.context.support.GenericApplicationContext;// CountingConnectionMaker 테스트 클래스
    public class UserDaoConnectionCountingTest {
      public static void main(String[] args) throws ClassNotFoundException, SQLException {
        AnnotationConfigApplicationContext context 
   //       = new AnnotationConfigApplicationContext(CountingDaoFactory.class);
            = new GenericApplicationContext("/~path~/applicationContext.xml");
        UserDao dao = context.getBean("userDao", UserDao.class);
        CountingConnectionMaker connectionMaker = context.getBean("connectionMaker", CountingConnectionMaker.class);
        System.out.println("Connection count :" + connectionMaker.getCount());
      }
    }
    ```

3. DataSource 인터페이스로 변환
    - DataSource 인터페이스 적용
        - ConnectionMaker는 DB커넥션을 생성해주는 기능 하나만을 정의한 매우 단순한 인터페이스 : 설명용이라는 말
        - DataSource 인터페이스가 자바에 존재 : DB커넥션을 가져오는 오브젝트의 기능을 추상화
        - DataSource를 사용하는 UserDao
        ```
        // UserDao에 주입될 의존 오브젝트 타입을 ConnectionMaker에서 DataSource로 변경
        public class UserDao() {
          private DataSource dataSource;
          public void setDataSource(DataSource dataSource) { this.dataSource = dataSource; }
          public void add(User user) throws SQLException {
            Connection c = dataSource.getConnection();
          }
        }
        // DataSource 구현 클래스(많은 것 들 중에 간단한 SimpleConnectionMaker 사용)
        // java 설정
        @Bean
        public DataSource dataSource() {
          SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
          dataSource.setDriverClass(com.mysql.cj.jdbc.Driver.class);
          dataSource.setUrl("jdbc:mysql://localhost/spring?serverTimezone=UTC");
          dataSource.setUsername("root");
          dataSource.setPassword("root");
          return dataSource;
        }
        @Bean
        public Dao dao() {
          Dao dao = new Dao();
          dao.setDataSource(dataSource());
          return dao
        }
        // XML 설정
        // spring이 프로퍼티의 값을 수정자 메서드의 파라미터 타입을 참고로 적절하게 변환해준다
        // 예시 : jdbc.Driver를 jdbc.Driver.class로 변환
        <beans>
          <bean id="dataSource" class="a.b.c.CountingConnectionMaker">
            <property name="driverClass" value="com.mysql.cj.jdbc.Driver" />
            <property name="url" value="jdbc:mysql://localhost/spring?serverTimezone=UTC" />
            <property name="username" value="user name" />
            <property name="password" value="password" />
          <bean/>
          <bean id="userDao" class="a.b.c.UserDao">
            <property name="connectionMaker" ref="dataSource"/>
          </bean>
        </beans>
        ```

### 9. 정리
> 간단한 DAO 코드로 시작해서 IoC/DI 프레임워크 적용
1. 과정
    - 관심사의 분리, 리팩토링
    - 전략 패턴 적용 : 독립된 책임으로 분리된 클래스를 필요에 따라 바꿔서 사용하는 패턴(구현방법이 달라져도 그 기능을 사용하는 클래스의 코드는 수정할 필요가 없도록)
    - 개방 폐쇄 원칙 적용 : 불필요한 변경을 방지하고 확장성 up
    - 낮은 결합도 : 한쪽의 기능 변화가 다른 쪽의 변경을 요구하지 않아도 됨
    - 높은 응집도 : 자신의 책임과 관심사에만 순수하게 집중하는 깔끔한 코드 탄생
    - IoC : 객체가 생성되고 여타 객체와 관계를 맺는 작업의 제어권을 IoC컨테이너로 넘겼다.
    - 싱글톤 레지스트리 : 싱글톤 패턴의 단점을 극복하도록 설계된 컨테이너 활용
    - DI : 설계시점에는 클래스-인터페이스 간 느슨한 의존관계를 만들어 두고, 런타임 시에 실제 사용할 구체적인 의존 객체를 DI컨테이너의 도움으로 주입받아서 동적 의존관계를 갖게 해주는 IoC의 특별한 케이스 적용
    - 생성자 주입, 수정자 주입
    - XML 설정
2. 스프링이란?
    - 어떻게 객체가 설계되고, 만들어지고, 관계를 맺고, 사용되는지에 관심을 갖는 프레임워크
    - 스프링의 관심은 객체와 그 관계이다.
    - 개발자의 역할과 책임은 객체 설계, 분리, 개선, 의존관계결정

### 기타
> 객체지향 설계 원칙(SOLID)
> 1. SRP(Single Responsibility Principle) : 단일 책임 원칙
> 2. OCP(Open Closed Principle) : 개방 폐쇄 원칙
> 3. LSP(Liskov Substitution Principle) : 리스코프 치환 원칙
> 4. ISP(Interface Segregation Principle) : 인터페이스 분리 원칙
> 5. DIP(Dependency Inversioin Principle) : 의존관계 역전 원칙