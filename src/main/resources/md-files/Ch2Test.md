#2장. 테스트
---

## 요약 및 결론
> 테스트는 코드가 개발자의 의도대로 동작하는지에 대한 검증을 자동화시켜서 신경써야될 부분을 줄여주고 성능의 신뢰도를 높인다.
>
> 지금 사용하던 JUnit, AssertJ와 사용방법이 다르다. 앞으로도 계속 바뀔 가능성이 높으니 계속 잘 배우자
>
> TDD : 테스트를 먼저 만들어 두면 개발이 끝나자마자 검증을 수행할 수 있어서 개발한 코드의 오류를 아주 빠르게 발견할 수 있다. 더 늦게 발견할 수록 해결하기 더 어렵다.

## 책 내용
> 스프링이 개발자에게 제공하는 가장 중요한 가치는 객체지향과 테스트 라고한다.
> 
> 테스트의 장점 : 코드에 대한 확신, 변화에 유연하게 대처할 수 있는 자신감
>
> 내용 : 테스트란, 테스트의 가치/장점/활용전략, 스프링과의 관계

### 1. UserDaoTest 다시 보기
1. 테스트의 유용성
    - 내가 예상하고 의도했던 대로 코드가 정확히 동작하는지를 확인할 수 있게 해준다.
2. UserDaoTest의 특징
    - 코드와 특징
      ```
      // main() 메서드 사용
      // UserDao의 오브젝트를 가져와 메서드를 호출
      // 테스트에 사용할 입력 값(User 오브젝트)을 직접 코드에서 만들어 넣어줌
      // 테스트의 결과를 콘솔에 출력해줌
      // 각 단계의 작업이 에러 없이 끝나면 콘솔에 성공 메시지로 출력해줌
      public class UserDaoTest (
        public static void main(String[] args) throws SQLException (
          ApplicationContext context =new GenericXmlApplicationContext("applicationContext.xml");          
          UserDao dao =context.getBean("userDao", UserDao.class);
          User user =new User(); 
          user.setld("user"); 
          user.setName("백기선"); 
          user.setPassword("married");          
          dao .add(user);          
          System.out.println(user.getId() + " 등록 성공“);
          User user2 = dao.get(user.getld()); 
          System.out.println(user2.getName()); 
          System.out.println(user2.getPassword());
          System.out.println(user2.getld() + " 조회 성공");
        }
      }
      ```
    - 웹을 통한 DAO 테스트 방법의 문제점
        - DAO만 테스트해야 되는데 서비스 클래스, 컨트롤러, 뷰를 다 만든 다음에야 테스트 할 수 있다.
            - 어디서 문제가 났는지 찾아다녀야 함
            - 하나의 기능을 테스트 하기 위해 불필요한 다른 코드들이 너무 많음
    - 작은 단위의 테스트(단위 테스트; unit test)
        - 관심의 분리를 적용해야 오류가 발생했을 때 원인 파악이 쉬워진다.
        - 코드가 의도대로 동작하는지 빨리 확인하기 위해서라도 단위가 작을 수록 좋다.
    - 자동수행 테스트 코드
        - 웹을 통한 테스트와 다르게 테스트 주체가 값을 잘못입력하거나, 테스트에 필요한 다른 프로그램을 직접 제어할 일이 없다.
        - 번거로운 점이 없기 때문에 자주 반복하기 좋다.
    - 지속적인 개선과 점진적인 개발을 위한 테스트
        - 1장에서 DAO를 개선하는동안 테스트코드 덕분에 잠재적인 오류를 안정적이고 점진적으로 해결해 나갈 수 있었다.
        - UserDao의 기능을 추가하는 경우에도 사이드이펙트에 대한 우려를 덜어준다.
3. UserDaoTest의 문제점
    - 문제점들
        1. 수동 확인 작업의 번거로움 -> 사람의 눈으로 직접 확인해야(print) 해서 양이 많아지면 불가능
        2. 실행 작업의 번거로움 -> main 메서드를 수백 개 실행해야 하는 상황이 생길 수 있음
        
### 2. UserDaoTest 개선
1. 테스트 검증의 자동화
    - 결과를 직접 확인해야 됐던 상황에서
    - 성공과 실패로 결과를 나눔
2. 테스트의 효율적인 수행과 결과 관리
    1. JUnit 테스트로 전환(JUnit Framework)
        - 이 프레임워크 역시 제어의역전(IoC)이 적용돼 있다.
        - main()메서드도 필요 없고 오브젝트를 만들어 실행시키는 코드도 필요 없다.
    2. 테스트 메서드 전환
        - JUnit 프레임워크가 요구하는 조건 : public일 것, @Test 애노테이션이 있을 것
    3. 검증 코드 전환
        - assertThat() 활용
        - 테스트성공 print가 필요 없다.
        - ```
          import org.junit.Test;
          import static org.hamcrest.CoreMatchers.is;
          import static org.junit.Assert.assertThat;
          public class UserDaoTest {
            @Test
            public void addAndGet() {
              ApplicationContext context = new GenericXmlApplicationContext("applicationContext.xml");		
              UserDao dao = context.getBean("userDao", UserDao.class);
              User user = new User("id1", "name1", "password1");
              dao.add(user);
              User user2 = dao.get(user.getId());
              assertThat(user2.getName(), is(user.getName());
              assertThat(user2.getPassword(), is(user.getPassword));              
            }
          }
          ```
    4. JUnit 테스트 실행
        - 어디선가 한 번은 JUnit 프레임워크를 시작시켜 줘야 한다.
        - ```
          public static void main(String[] args) {
            JUnitCore.main("springbook.user.dao.UserDaoTest");
          }
          ```

### 3. 개발자를 위한 테스팅 프레임워크 JUnit
> JUnit은 사실상 자바의 표준 테스팅 프레임워크라고 할 수 있다.
> 
> 스프링 테스트 모듈도 JUnit을 이용한다.
1. JUnit 테스트 실행 방법
    - IDE
        - 이클립스 Run As 항목에 JUnit Test를 선택
        - 수행시간, 실행한 테스트 수, 테스트 에러의 수, 테스트 실패의 수 등을 알려준다.
        - 실패한 경우 assertThat()의 위치를 알려준다.
    - 빌드 툴(ANT, Maven 등)
        - 테스트 실행 결과는 옵션에 따라 HTML이나 텍스트 파일의 형태로 보기좋게 만들어진다. (surefire report?)
2. 테스트 결과의 일관성
    - 지금 까지 테스트의 문제는 테스트 결과에 대해 rollback이 없는 것.
    - 테스트를 마치면 모든 데이터를 삭제하는 기능을 만들어 본다.
        - ```
          public void deleteAll() throws SQLException {
            try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement("delete from users");) {
              ps.executeUpdate();       
            } catch (Exception e) { }
          }
          public int getCount() {
            int count = 0;
            try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement("select * from user");) {
              ResultSet rs = ps.executeQuery();
              count = rs.getInt();
              rs.close();              
            } catch (Exception e) { }
          }
          ```
    - deleteAll(), getCount() 테스트
        - 기존에 있던 addAndGet()을 확장 : 이미 값이 있는 상태인 경우에만 유의미한 메서드이기 때문
        - ```
          public class UserDaoTest {
            @Test
            public void addAndGet() {
              ApplicationContext context = new GenericXmlApplicationContext("applicationContext.xml");		
              UserDao dao = context.getBean("userDao", UserDao.class);
              dao.deleteAll();
              assertThat(dao.getCount(), is(0));
              User user = new User("id1", "name1", "password1");
              dao.add(user);
              User user2 = dao.get(user.getId());
              assertThat(user2.getName(), is(user.getName());
              assertThat(user2.getPassword(), is(user.getPassword));              
            }
          }
          ```
    - 동일한 결과를 보장하는 테스트가 됐다

3. 포괄적인 테스트
    - 테스트를 안 만드는 것도 위험하지만 성의 없는 테스트는 더 위험하다.
    - 여러 경우를 검증해야 한다.
        - getCount() 테스트 보완점 : User를 등록할 때 마다 count가 늘어나는지 검증
        - addAndGet() 테스트 보완점 : add후에 count, User 두 종류
        - get() 
            - 예외조건(없는 경우) : 예외리턴
            - ```
              @Test(expected = EmptyResultDataAccessException.class)
              ```
            - 테스트를 성공시키기 위한 코드 수정
            - ```
              public User get(String id) throws SQLException {
                ...
                User user = null;
                if (rs.next()) {
                  // 쿼리의 결과가 있을 때만 수행
                  user = new User(rs.getString("id"), rs.getString("name"), rs.getString("password")); 
                }
                if (user == null) throw new EmptyResultDataAccessException(1);
                ...
              }
              ```
    - 포괄적인 테스트
        - 간단한 기능이라도 방심하면 원인을 찾기 힘든 상황에 빠질 수 있다. 왠만하면 테스트 케이스 만들자.
        - 성공하는 케이스만 만들지 말고 예외적인 상황도 생각하는게 맞다. 맞지 ㅎㅎ
        - 로드 존슨(스프링 창시자) : 항상 네거티브를 먼저 만들라
        
4. 테스트가 이끄는 개발(TDD)
    - 위에서 작업한 순서 : 새 기능을 만들기 위해 테스트를 먼저 만들고 UserDao 코드를 수정(expected 설정 -> get메서드 수정)
    - 기능설계를 위한 테스트
        - 추가하고 싶은 기능을 일반 언어가 아니라 테스트 코드로 표현을 해서 코드로 된 설계문서처럼 만듬
        - 바로 이 테스트를 실행해서 설계한 대로 코드가 동작하는지를 빠르게 검증할 수 있다.
    - 테스트 주도 개발
        - 테스트 코드를 먼저 만들고, 테스트를 성공하게 해주는 코드를 작성하는 방식의 개발 방법
        - 기본 원칙 : 실패한 테스트를 성공시키기 위한 목적이 아닌 코드는 만들지 않는다. (=실패한 테스트를 성공시키기 위한 목적의 코드만 만든다.?)
        - 개발한 코드의 오류를 아주 빠르게 발견할 수 있다. 더 늦게 발견할 수록 해결하기 더 어렵다.
        - 테스트를 먼저 만들어 두면 개발이 끝나자마자 검증을 수행할 수 있어서 좋다.

5. 테스트 코드 개선
    - @Before : JUnit이 제공하는 애노테이션.
        - @Test 메서드가 실행되기 전에 먼저 실행돼야 하는 메서드를 정의한다.
        - ```
          import org.junit.Before;
          public class UserDaoTest {
            private UserDao dao;
            @Before
            public void setUp() {
              //중복되던 코드 한방에 해결
              ApplicationContext context =new GenericXmlApplicationContext("applicationContext.xml");          
              dao =context.getBean("userDao", UserDao.class);
            }
          }
          ```
    - JUnit이 테스트를 수행하는 방식
        1. 하나의 테스트 클래스를 가져온다.
        2. 클래스 안에 @Test가 붙고 public void이며 파라미터가 없는 테스트 메서드를 모두 찾아온다.
        3. 각 @Test 메서드마다
            - 테스트 클래스의 객체 생성
            - @Before가 붙은 메서드가 있으면 실행한다.
            - @Test 메서드 호출
            - 테스트 결과 저장
            - @After가 붙은 메서드가 있으면 실행
        4. 모든 테스트의 결과를 종합해서 돌려준다.
    - 왜 JUnit은 매 @Test 마다 새 인스턴스를 만들까?
        - 각 테스트가 서로 영향을 주지 않고 독립적으로 실행됨을 보장하기 위해
        - 그러니까 @Before든 private으로 뺀 메서드든 별 차이 없다.
    - 픽스처(fixture)
        - 테스트를 수행하는 데 필요한 정보나 오브젝트
        - 예제코드에서는 dao

### 4. 스프링 테스트 적용
> 지금까지 예제에서 문제는 애플리케이션 컨텍스트가 테스트 메서드 개수만큼 만들어지는 것
> 
> 애플리케이션 컨텍스트가 생성될 때는 모든 빈 객체를 초기화하기 때문에 오래걸린다.

1. 테스트를 위한 애플리케이션 컨텍스트 관리
    - 스프링에서는
        - JUnit을 이용하는 테스트 컨텍스트 프레임워크를 제공한다.
        - 간단한 애노테이션 설정으로 애플리케이션 컨텍스트를 모든 테스트가 공유하도록 만들 수 있다.
    - 스프링 테스트 컨텍스트 프레임워크 적용
        - ApplicationContext를 @Autowired로 주입받음
        - SpringJUnit4ClassRunner : JUnit용 테스트 컨텍스트 프레임워크 확장 클래스
            - 테스트가 사용할 애플리케이션 컨텍스트를 만들고 관리
        ```
        // 스프링의 테스트 컨텍스트 프레임워크의 JUnit확장기능 지정
        @RunWith(SpringJUnit4ClassRunner.class) 
        // 테스트 컨텍스트가 자동으로만들어줄 애플리케이션 컨텍스트의 위치 지정
        @ContextConfiguration(locations="/applicationContext.xml") 
        public class UserDaoTest {
          // ApplicationContext는 초기화할 때 자기 자신도 빈으로 등록한다.
          @Autowired
          private ApplicationContext context;            
        
          @Before
          public void setUp() {			
            // ApplicationContext context = new GenericXmlApplicationContext("applicationContext.xml");
            this.dao = context.getBean("userDao", UserDao.class);
            // 테스트가 실행될 때 마다 동일한 context를 사용하는지 확인
            System.out.println(this.context);
          }
        }
        ```
    - 테스트 클래스의 컨텍스트 공유
        - 서로 다른 클래스에 있는 테스트여도 하나의 설정파일을 보고있다면 애플리케이션 컨텍스트를 공유한다.
        - 일부 테스트에서만 다른 설정파일을 사용하도록 설정할 수도 있다.

2. DI와 테스트
    - 테스트 코드에 의한 DI
        - 테스트코드에서 운영용 DB 정보를 갖는 DataSource를 사용하면 큰일이니까
            - (방법1) 테스트클래스에 전용 DataSource를 만들어 사용한다.
            - ```
              // 테스트 메서드가 애플리케이션 컨텍스트의 구성이나 상태를 변경한다는 것을
              // 테스트 컨텍스트 프레임워크에 알려주는 애노테이션
              @DirtiesContext
              public class UserDaoTest {
                @Autowired
                UserDao dao;
                @Before
                public void setUp() throws SQLException {
                    DataSource dataSource = new SingleConnectionDataSource(
                      "jdbc:mysql://localhost/testdb", "srping", "book", true);
                    dao.setDataSource(dataSource); //코드에 의한 수동 DI
                }
              }
              ```
            - ApplicationContext를 공유하는 다음 테스트코드에 문제가 생길 수 있음
                - 그래서 @DirtiesContext 애노테이션을 붙이면 ApplicationContext를 공유하지 못하게 막음
                - 그 테스트 클래스 만을 위한 ApplicationContext를 새로이 생성
    - 테스트를 위한 별도의 DI 설정
        - (방법2) 테스트에서만 사용될 DataSource가 빈으로 정의된 테스트 전용 설정파일을 따로 만들어두고 사용
        - ```
          <!-- test-applicationContext.xml -->
          <bean id="dataSource" class="o.s.j.d.SimpleDriverDataSource">
            <property name="driverClass" value="com.mysql.cj.jdbc.Driver" />
            <property name="url" value="jdbc:mysql://localhost/testdb" />
            <property name="username" value="username" />
            <property name="password" value="password" />
          <bean/>
          ```
          ```
          @RunWith(SpringJUnit4ClassRunner.class) 
          // 테스트 컨텍스트 설정
          @ContextConfiguration(locations = "/ㅅㄷㄴㅅ-applicationContext.xml") 
          public class UserDaoTest {
            ...
          }
          ```
    - 컨테이너 없는 DI 테스트
        - (방법3)스프링 컨테이너를 사용하지 않고 테스트를 만드는것.
        - ```
          public class UserDaoTest {
            UserDao dao;
            @Before
            public void setUp() throws SQLException {
                // 객체 생성, 관계설정을 직접
                dao = new UserDao();
                DataSource dataSource = new SingleConnectionDataSource(
                  "jdbc:mysql://localhost/testdb", "srping", "book", true);
                dao.setDataSource(dataSource);
            }
          }
          ```
        - 애플리케이션 컨텍스트가 만들어지는 시간을 절약했다고함... 
            - 이게 DI인 이유는? 테스트코드(@Test)에 setUp()을 통해서 UserDao를 주입해주었기 때문?
    - DI를 이용한 테스트 방법 선택
        - 스프링 컨테이너 없이 테스트 할 수 있는 방법을 우선적으로 고려
            - 수행 속도가 빠르고 테스트 자체가 간결하기 때문

### 5. 학습 테스트로 배우는 스프링
> 학습 테스트 : 개발자 자신이 만들지 않은 프레임워크나 다른 개발팀에서 제공한 라이브러리 등에 대해 수행하는 테스트
>
> 목적 : 사용방법 익히기

1. 학습 테스트의 장점
    - 다양한 조건에 따른 기능을 손쉽게 확인해볼 수 있다.
        - 자동화된 테스트의 모든 장점이 학습 테스트에도 그대로 적용된다.
        - 조건을 다르게 해서 반복적으로 테스트하기 쉽다.
    - 학습 테스트 코드를 개발 중에 참고할 수 있다.
        - 테스트코드는 기록으로 남기 때문
    - 프레임워크나 제품을 업그레이드할 때 호환성 검증을 돕는다.
        - 업그레이드를 마친 뒤에 학습 테스트코드를 수행하면 검증에 도움이 된다. 
    - 테스트 작성에 대한 좋은 훈련이 된다.
    - 새로운 기술을 공부하는 과정이 즐거워진다.
2. 학습 테스트 예제
    - 참고 : https://github.com/spring-projects/spring-framework
    - 예제
        - 3개의 테스트메서드가 각각 수행될 때 마다 테스트클래스 인스턴스가 다름을 증명
        - 서로 다른 테스트 메서드 마다 ApplicationContext를 공유하는지 증명하는 테스트
        - ```           
          @RunWith(SpringJUnit4ClassRunner.class)
          @ContextConfiguration(locations="/applicationContext.xml")
          public class UserDaoTest {
            @Autowired
            private ApplicationContext context;
            static Set<JUnitTest> testObjects = new HashSet<JUnitTest>();
            @Test
            public void test1() {
              assertThat(testObjects, not(hashItme(this)));
              testObjects.add(this);
              assertThat(contextObject == null || contextObject == this.context, is(true));
              contextObject = this.context;
            }
            @Test
            public void test2() {
              assertThat(testObjects, not(hashItme(this)));
              testObjects.add(this);
              assertTrue(contextObject == null || contextObject == this.context);
              contextObject = this.context;
            }
            @Test
            public void test3() {
              assertThat(testObjects, not(hashItme(this)));
              testObjects.add(this);
              assertThat(contextObject, either(is(nullValue())).or(is(this.context)));
              contextObject = this.context;
            }
          }
          ```
3. 버그 테스트
    - 버그가 원인이 되어서 테스트가 실패하는 테스트코드
    - 장점
        - 테스트의 완성도를 높여준다.
            - 버그를 발견하기 전에 있던 테스트가 버그에 대한 부분을 커버하지 못하고 있었기 때문에 버그에 대한 테스트코드를 추가하면 테스트의 완성도가 높아진다. 
        - 버그의 내용을 명확하게 분석해준다.
            - 테스트코드로 작성하기 위한 과정에서 명확하게 문제를 분석할 수 있게 된다.
            - 분석하는 도중 또 다른 사이드이펙트를 확인할 가능성도 있다.
        - 기술적인 문제를 해결하는 데 도움이 된다.
            - 이유를 알수 없는 문제에 대한 답이 될 수 있다.
            
### 6. 정리
- 좋은 테스트의 기준
```
1. 자동화돼야 하고, 빠르게 실행할 수 있어야 한다.
2. 일관성이 있어야 한다
    > 코드의 변경 없이 환경이나, 테스트 실행 순서에 따라서 결과가 달라지면 안된다.
3. 포괄적으로 작성해야 한다.
    > 충분한 검증을 하지 않는 테스트는 없는 것보다 나쁠 수 있다.
4. 코드 작성과 테스트 수행의 간격이 짧을수록 효과적이다. 
``` 

- main()테스트 대신 JUnit 프레임워크를 이용한 테스트가 편리하다.
- 테스트를 먼저 만들고 테스트를 성공시키는 코드를 만들어가는 테스트 주도 개발 방법도 유용하다.
- 테스트코드 역시 애플리케이션 코드와 마찬가지로 적절한 리팩토링이 필요하다.
- 동일한 설정파일을 사용하는 테스트는 하나의 애플리케이션 컨텍스트를 공유한다.
- 스프링을 사용하는 개발자라면 자신이 만든 코드를 테스트로 검증하는 방법을 알고 있어야 하며, 테스트를 개발에 적극적으로 활용할 수 있어야 한다.