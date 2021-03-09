#6장. AOP
---

## 요약 및 결론
> 
    
## 책 내용
> AOP는 스프링 3대 기반기술 중 하나이다.(IoC/DI, 서비스 추상화)
>
> OOP를 대체하려는 것 처럼 보이는 AOP의 등장배경과 스프링이 이것을 도입한 이유, 장점을 이해해야 한다.
> ```
> 1. 등장배경 : 
> 2. 이유 : 
> 3. 장점 : 
> ```
> 


### 1. 트랜잭션 코드의 분리
- 코드를 정리해본다 :
    1. 한 메서드가 하나의 책임만 갖도록 : UserService.upgradeLevelsInternal()
    2. 한 클래스가 하나의 책임만 갖도록 : UserService에서 트랜잭션 경계설정 코드
1. 메서드 분리
    - upgradeLevels()에서 트랜잭션 경계설정 코드와 비즈니스 로직 코드를 분리한다.
    - <details markdown="1">
      <summary>코드 접기/펼치기</summary>
      <pre>
      // 트랜잭션 경계설정 코드만 남은 메서드
      public void upgradeLevels() {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
          upgradeLevelsInternal();
          transactionManager.commit(status);
        } catch (Exception e) {
          transactionManager.rollback(status);
          throw e;
        }
      }      
      // upgradeLevels에서 비즈니스로직만 분리
      private void upgradeLevelsInternal() {
        List<User> userList = userDao.getAll();
        userList.forEach(user -> {
          if (canUpgradeLevel(user)) {
            upgradeLevel(user);
          }
        });
      }
      </pre>
      </details>
2. DI를 이용한 클래스의 분리
    - 트랜잭션을 담당하는 코드는 UserService에 있을 필요가 없으니 클래스 밖으로 뽑아낸다.
    - UserService에 인터페이스를 도입하고 구현클래스를 2개 둔다(UserServiceImpl, UserServiceTx)
    - UserServiceTx는 트랜잭션 경계설정에 대한 책임만 갖게된다.
    - UserServiceTx는 비즈니스 로직을 전혀 갖지 않고 다른 UserService 구현 오브젝트에 위임만 한다.
    - 트랜잭션이 적용된 UserServiceTx
    - <details markdown="1">
      <summary>코드 접기/펼치기</summary>
      <pre>
      @RequiredArgsConstructor
      public class UserServiceTx implements UserService {      
        private final UserService userService;
        private final PlatformTransactionManager transactionManager;  
        ...
        public void upgradeLevels() {
          TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
          try {
            userService.upgradeLevels();
            transactionManager.commit(status);
          } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
          }
        }
        ...
      }
      </pre>
      </details>
    - 트랜잭션 경계설정 코드 분리의 장점
        1. UserServiceImpl은 트랜잭션 내용을 신경쓰지 않아도 된다.
        2. 비즈니스 로직에 대한 테스트를 손쉽게 만들어낼 수 있다.(다음절에서 확인)
    - @Transactional이 더 편리해 보인다.

### 2. 고립된 단위 테스트
- 가장 편하고 좋은 테스트 방법은 가능한 한 작은 단위로 쪼개서 테스트하는 것이다.
    - 테스트가 실패했을 때 원인을 찾기 쉽다.
1. 복잡한 의존관계 속의 테스트
    - UserService는 꽤 복잡하다
    - UserService 뒤에 존재하는 훨씬 더 많은 객체, 환경, 서비스, 서버, 네트워크까지 테스트 하는 셈이다.
2. 테스트 대상 객체 고립시키기
    - 그래서
        - 외부 요소들에 영향을 받지 않도록 고립시킬 필요가 있다.
    - 장점
        1. 다른 요소를 배제한 UserService의 기능만을 테스트할 수 있다.
        2. 다른 요소들에 필요한 자원들을 소모하지 않으므로 수행 성능이 좋다.(속도 향상)
    - 테스트를 위한 UserServiceImpl 고립
        - MockUserDao, MockMailSender 사용하면 됨
    - 고립된 단위테스트 활용
        - upgradeLevels()에 적용
        - UserDao 목 객체
            - UserServiceTest전용이므로 스태틱 내부 클래스로 선언
        - <details markdown="1">
          <summary>코드 접기/펼치기</summary>
          <pre>
          @Test
          public void upgradeLevels() throws Exception {
            // 고립된 테스트이므로 대상 객체를 직접 생성한다.
            UserServiceImpl userService = new UserServiceImpl();
            // 목 객체를 set해준다.
            MockUserDao mockUserDao = new MockUserDao(this.userList);
            userService.setUserDao(mockUserDao);
            MockMailSender mockMailSender = new MockMailSender();
            userService.setMailSender(mockMailSender);
            // 목 객체를 가진 UserService의 메서드를 수행한다.
            userService.upgradeLevels();
            // UserService 결과가 담긴 목 객체의 값들을 확인한다.
            List<User> updated = mockUserDao.getUpdated();
            assertThat(updated.size()).isEqualTo(3);
            checkUserAndLevel(updated.get(0), userList.get(1).getId(), Level.SILVER);
            checkUserAndLevel(updated.get(1), userList.get(3).getId(), Level.GOLD);
            checkUserAndLevel(updated.get(2), userList.get(4).getId(), Level.GOLD);
            List<String> requests = mockMailSender.getRequests();
            assertThat(requests.size()).isEqualTo(3);
            assertThat(requests.get(0)).isEqualTo(userList.get(1).getEmail());
            assertThat(requests.get(1)).isEqualTo(userList.get(3).getEmail());
            assertThat(requests.get(2)).isEqualTo(userList.get(4).getEmail());
          }
          </pre>
          </details>
3. 단위 테스트와 통합 테스트
    - 이 책에서 단위 테스트 : 의존 오브젝트나 외부 리소스를 사용하지 않도록 고립시킨 테스트
    - 이 책에서 통합 테스트 : 외부의 DB, 파일 등의 리소스가 참여하는 테스트
    - 선택 가이드라인
        1. 항상 단위테스트 먼저 고려
        2. 외부리소스를 사용해야만 하는 경우에는 통합 테스트
        3. 단위 테스트를 만들기가 너무 복잡하면 처음부터 통합테스트를 고려해본다.
4. 목 프레임워크
    - Mockito 프레임워크
        - 특징 : 목 클래스를 준비할 필요 없음
        - 오... 좋은데
        - 생성자 의존성 주입으로 바꾸면 더 좋겠다.
        - 좋은데 앞으로 안쓴단다. 왜안쓰지;;
        - <details markdown="1">
          <summary>코드 접기/펼치기</summary>
          <pre>
          @Test
          public void upgradeLevelsWithMockito() {
            UserServiceImpl userService = new UserServiceImpl();
            UserDao mockUserDao = mock(UserDao.class);
            when(mockUserDao.getAll()).thenReturn(this.userList);
            userService.setUserDao(mockUserDao);
            MailSender mockMailSender = mock(MailSender.class);
            userService.setMailSender(mockMailSender);
            userService.upgradeLevels();
            // 목 객체가 제공하는 검증 기능 활용
            // 어떤 메서드가 몇 번 호출됐는지, 파라미터는 무엇인지 확인할 수 있다.
            verify(mockUserDao, times(3)).update(any(User.class));
            verify(mockUserDao).update(userList.get(1));
            assertThat(userList.get(1).getLevel()).isEqualTo(Level.SILVER);
            verify(mockUserDao).update(userList.get(3));
            assertThat(userList.get(3).getLevel()).isEqualTo(Level.GOLD);
            verify(mockUserDao).update(userList.get(4));
            assertThat(userList.get(4).getLevel()).isEqualTo(Level.GOLD);
            // 파라미터를 정밀하게 검사하기 위해 캡처
            ArgumentCaptor<SimpleMailMessage> mailMessageArgs = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mockMailSender, times(3)).send(mailMessageArgs.capture());
            List<SimpleMailMessage> mailMessages = mailMessageArgs.getAllValues();
            assertThat(mailMessages.get(0).getTo()[0]).isEqualTo(userList.get(1).getEmail());
            assertThat(mailMessages.get(1).getTo()[0]).isEqualTo(userList.get(3).getEmail());
            assertThat(mailMessages.get(2).getTo()[0]).isEqualTo(userList.get(4).getEmail());
          }
          </pre>
          </details>

### 3. 다이내믹 프록시와 팩토리 빈
1. 프록시와 프록시 패턴, 데코레이터 패턴
    - 프록시
        - 클라이언트는 인터페이스만 보고 사용하기 때문에 핵심기능을 갖는 클래스(타깃)를 사용할 것으로 기대하지만 실제적으로는 부가기능(프록시)을 통해 핵심기능을 사용하는 구성
        - 클라이언트 -> 프록시 -> 타깃 
        - 프록시 : 마치 자신이 실제 대상인 것처럼 위장해서 클라이언트의 요청을 대리인처럼 대신 받아줌
        - 타깃 또는 실체 : 프록시를 통해 최종적으로 요청을 위임받아 처리하는 실제 객체
        - 프록시의 목적
            1. 클라이언트가 타깃에 접근하는 방법을 제어하기 위함
            2. 타깃에 부가적인 기능을 부여해주기 위함
    - 데코레이터 패턴
        - 타깃에 부가적인 기능을 런타임 시점에 동적으로 부여해주기 위해 프록시를 사용하는 패턴
        - 동적인 이유 : 컴파일 시점에는 프록시와 타깃의 관계가 정해져있지 않기 때문
        - 데코레이터인 이유 : 여러 겹의 포장지와 장식처럼 실제 내용물에는 변함 없지만 부가적인 효과를 부여하기 때문
        - 대표적인 예 : InputStream(내용물) - FileInputStream(부가기능 + 내용물)
        - 예제에서 UserService와 UserServiceTx도 데코레이터 패턴
    - 프록시 패턴
        - 일반적으로 사용하는 프록시와 디자인 패턴 프록시 패턴에서의 프록시는 구분이 필요하다.
            - 전자 : 클라이언트와 사용 대상 사이에 대리 역할을 맡은 객체를 두는 방법을 총칭
            - 후자 : 프록시를 사용하는 방법 중 타깃에 대한 접근 방법을 제어하려는 목적을 가진 경우
        - 프록시 패턴에서 프록시는
            - 타깃의 기능을 확장하거나 추가하지 않는다.
            - 클라이언트가 타깃에 접근하는 방식을 변경해준다.
            - ```
              타깃 객체를 생성하기가 복잡하거나 당장 필요하지 않은 경우에는 꼭 필요한 시점까지 객체를 생성하지 않는 편이 좋다.
              그런데 타깃 객체에 대한 레퍼런스가 미리 필요할 수 있다.
              이럴 때 프록시 패턴을 적용한다.
              실제 타깃 객체를 만드는 대신 프록시를 넘겨준다.
              프록시의 메서드를 통해 타깃을 사용하려고 시도하면?
              그때 프록시가 타깃 객체를 생성하고 요청을 위임하는 식이다.
              ```
    - 앞으로는...
        - 타깃과 동일한 인터페이스를 구현하고
        - 클라이언트와 타깃 사이에 존재하면서
        - 기능의 부가 또는 접근 제어를 담당하는 객체
        - 를 모두 프록시라고 부르겠다.
2. 다이내믹 프록시
    - 프록시는 기존 코드에 영향을 주지 않으면서 기능을 확장하거나 접근 방법을 제어하는 유용한 방법이다.
        - 그러나 번거롭기 때문에 손이 안간다.
        - 프록시도 목 프레임워크 처럼 편리하게 사용할 수 있는 방법이 있을까?
    - 프록시의 구성과 프록시 작성의 문제점
        - 프록시의 구성
            1. 타깃과 같은 메서드를 구현하고 있다가 메서드가 호출되면 타깃 객체로 위임한다.
            2. 지정된 요청에 대해서는 부가기능을 수행할 수 있다.
        - UserServiceTx에서 프록시의 구성
        - <details markdown="1">
          <summary>코드 접기/펼치기</summary>
          <pre>   
          public class UserServiceTx implements UserService {          
            @Autowired private PlatformTransactionManager transactionManager;          
            @Autowired private UserServiceImpl userService;          
            // 타깃 객체로 위임
            public void add(User user) { userService.add(user); }          
            public void upgradeLevels() {
              // 부가기능 수행 ~
              TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
              try {                
                userService.upgradeLevels(); // 타깃 객체로 위임
                transactionManager.commit(status);
              } catch (Exception e) {
                transactionManager.rollback(status);
                throw e;
              }
              // ~ 부가기능 수행
            }
          }
          </pre>
          </details>
        - 프록시 작성의 문제점
            1. 타깃의 인터페이스를 구현하고 위임하는 코드를 작성하기 번거롭다. 메서드가 추가되거나 변경될 때 마다 해줘야된다.
            2. 부가기능 코드 중복이 몹시 심해진다.
    - 리플렉션
        - JDK의 다이내믹 프록시는 리플렉션 기능을 이용해서 프록시를 만들어 준다.
        - <details markdown="1">
          <summary>코드 접기/펼치기</summary>
          <pre>
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
          </pre>
          </details>
    - 프록시 클래스
        - 다이내믹 프록시를 이용한 프록시를 만들어본다.
        - HelloProxyTest.simpleProxy()는 프록시 적용의 일반적인 문제점 두가지를 모두 갖는다.
    - 다이내믹 프록시 적용(UppercaseHandler, HelloProxyTest.dynamicProxy())
        - HelloUppercaseProxy를 다이내믹 프록시를 이용해 만들어본다.
        - Hello 인터페이스에 메서드가 아무리 많아도 InvocationHandler에서 invoke메서드 하나로 처리한다.
        - 충분히 재사용할 수 있다.
        - 메서드를 선별해서 사용할 수도 있다.
3. 다이내믹 프록시를 이용한 트랜잭션 부가기능
    - UserServiceTx를 다이내믹 프록시 방식으로 변경
    - 변경하는 이유?
        1. 서비스 인터페이스의 메서드를 모두 구현해야 함
        2. 트랜잭션이 필요한 메서드 마다 트랜잭션 처리코드가 중복
    - 트랜잭션 InvocationHandler
        - 트랜잭션 부가기능을 갖는 핸들러
        - <details markdown="1">
          <summary>코드 접기/펼치기</summary>
          <pre>
          public class TransactionHandler implements InvocationHandler {
            // 부가기능을 제공할 타깃 객체. 어떤 타입의 객체든 적용할 수 있다.
            @Setter private Object target;
            // 트랜잭션을 제공하기 위한 트랜잭션 매니저
            @Autowired private PlatformTransactionManager transactionManager;
            // 트랜잭션을 적용할 메서드 이름 패턴
            @Setter private String pattern;
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
              if (method.getName().startsWith(pattern)) {
                return invokeInTransaction(method, args);
              } else {
                return method.invoke(target, args);
              }
            }
            private Object invokeInTransaction(Method method, Object[] args) throws Throwable {
              TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());
              try {
                Object ret = method.invoke(target, args);
                this.transactionManager.commit(status);
                return ret;
              } catch (InvocationTargetException e) {
                this.transactionManager.rollback(status);
                throw e.getTargetException();
              }
            }
          }
          </pre>          
          </details>
    - TransactionHandler와 다이내믹 프록시를 이용하는 테스트
        - UserServiceTest에 적용
        - <details markdown="1">
          <summary>코드 접기/펼치기</summary>
          <pre>
          @Test
          public void upgradeAllOrNothing() {
            TestUserService testUserService = new TestUserService(userList.get(3).getId());
            testUserService.setUserDao(this.userDao);
            testUserService.setMailSender(mailSender);
            TransactionHandler txHandler = new TransactionHandler();
            txHandler.setTarget(testUserService);
            txHandler.setTransactionManager(transactionManager);
            txHandler.setPattern("upgradeLevels");
            UserService txUserService = (UserService)Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class[] { UserService.class },
                    txHandler
            );
            userDao.deleteAll();
            userList.forEach(user -> userDao.add(user));
            try {
              txUserService.upgradeLevels();
              fail("TestUserServiceException expected");
            } catch (TestUserServiceException e) {
            }
            checkLevelUpgraded(userList.get(1), false);
          }
          </pre>
          </details>
4. 다이내믹 프록시를 위한 팩토리 빈
    - 이제 TransactionHandler와 다이내믹 프록시를 스프링의 DI를 통해 사용할 수 있도록 만들 차례다.
    - 그런데 일반적인 빈으로 등록할 수 없는게 문제
        - 스프링 빈은 클래스 이름과 프로퍼티로 정의되는데?
        - 스프링은 지정된 클래스 이름을 가지고 리플렉션을 이용해서 해달 클래스의 객체를 만들어요
        - 다이내믹 프록시 객체는 이런 식으로 프록시 객체가 생성되지 않는다는 점이다.
        - 다이내믹 프록시는 Proxy클래스의 newProxyInstance()라는 스태틱 메서드를 통해서만 만들 수 있다.
    - 팩토리 빈
        - 스프링을 대신해서 객체의 생성로직을 담당하도록 만들어진 특별한 빈
        - 팩토리 빈을 등록하는 가장 간단한 방법은 스프링의 FactoryBean인터페이스를 구현하는 방법
        - 생성자를 제공하지 않는 Message클래스를 팩토리 빈으로 등록하는 예제
        - <details markdown="1">
          <summary>코드 접기/펼치기</summary>
          <pre>
          // 참고 : https://www.baeldung.com/spring-factorybean
          @RunWith(SpringJUnit4ClassRunner.class)
          @ContextConfiguration(classes = FactoryBeanConfig.class)
          public class MessageFactoryBeanTest {          
            @Autowired
            private ApplicationContext context;          
            @Qualifier("message")
            @Autowired
            private Message message;          
            @Resource(name = "&message")
            private MessageFactoryBean messageFactoryBean;          
            @Test
            public void getMessageFromFactoryBean() throws Exception {
              Object messageFromContext = context.getBean("message");
              assertThat(messageFromContext).isInstanceOf(Message.class);
              assertThat(((Message) messageFromContext).getText()).isEqualTo("Factory Bean");
              assertThat((message).getText()).isEqualTo("Factory Bean");
              Message messageFromFactoryBean = messageFactoryBean.getObject();
              assertThat(messageFromFactoryBean.getText()).isEqualTo("Factory Bean");
            }
          }
          </pre>
          </details>
    - 다이내믹 프록시를 만들어주는 팩토리 빈
        - 팩토리 빈을 사용하면 스프링의 빈으로 만들어 줄 수 있군(팩토리 빈의 getObject 메서드)
        - <details markdown="1">
          <summary>FactoryBean코드 접기/펼치기</summary>
          <pre>
          @Setter
          public class TxProxyFactoryBean implements FactoryBean&lt;Object&gt; {
            private Object target;
            private PlatformTransactionManager transactionManager;
            private String pattern;
            private Class&lt;?&gt; serviceInterface;          
            // FactoryBean 구현 메서드
            @Override
            public Object getObject() throws Exception {
              TransactionHandler txHandler = new TransactionHandler();
              txHandler.setTarget(target);
              txHandler.setTransactionManager(transactionManager);
              txHandler.setPattern(pattern);
              return Proxy.newProxyInstance(getClass().getClassLoader(),
                      new Class[]{serviceInterface},
                      txHandler);
            }
            public Class&lt;?&gt; getObjectType() {
              return serviceInterface;
            }
              public boolean isSingleton () {
                // 싱글톤 빈이 아니라는 뜻이 아님
                // getObject가 매 번 같은 객체를 리턴하지 않는다는 의미
                return false;
            }
          }
          </pre>
          </details>          
        - <details markdown="1">
          <summary>Config 코드 접기/펼치기</summary>
          <pre>
          @Configuration
          public class FactoryBeanConfig {          
            @Autowired private UserServiceImpl userServiceImpl;
            @Autowired private PlatformTransactionManager transactionManager;
            @Bean(name = "txProxy")
            public TxProxyFactoryBean txProxyFactoryBean() {
              TxProxyFactoryBean factoryBean = new TxProxyFactoryBean();
              factoryBean.setTarget(userServiceImpl);
              factoryBean.setTransactionManager(transactionManager);
              factoryBean.setPattern("upgradeLevels");
              factoryBean.setServiceInterface(UserService.class);
              return factoryBean;
            }          
          }
          </pre>
        - </details>
          <details markdown="1">
          <summary>테스트 코드 접기/펼치기</summary>
          <pre>
          @RunWith(SpringRunner.class)
          @SpringBootTest
          @Slf4j
          class UserServiceTest {
            @Autowired private TxProxyFactoryBean txProxy;
            @Test
            public void upgradeAllOrNothing() throws Exception {
              TestUserService testUserService = new TestUserService(userList.get(3).getId());
              testUserService.setUserDao(this.userDao);
              testUserService.setMailSender(mailSender);
              txProxy.setTarget(testUserService);
              UserService txProxyUserService = (UserService)txProxy.getObject();
          
              userDao.deleteAll();
              userList.forEach(user -> userDao.add(user));
              try {
                txProxyUserService.upgradeLevels();
                fail("TestUserServiceException expected");
              } catch (TestUserServiceException e) {
              }
              checkLevelUpgraded(userList.get(1), false);
            }
            static class TestUserService extends UserServiceImpl {
              private String id;
              public TestUserService(String id) {
                this.id = id;
              }
              protected void upgradeLevel(User user) {
                if (user.getId().equals(this.id)) {
                  throw new TestUserServiceException();
                }
                super.upgradeLevel(user);
              }
            }
          }
          </pre>
          </details>
5. 프록시 팩토리 빈 방식의 장점과 한계
    - 프록시 팩토리 빈의 재사용
        - TransactionHandler를 이용하는 TxProxyFactoryBean은 코드 수정 없이 다양한 클래스에 적용할 수 있다.
    - 프록시 팩토리 빈 방식의 장점
        - 데코레이터 패턴이 적용된 프록시를 사용할 때 발생하는 문제 두 가지를 해결할 수 있다.
            1. 프록시 대상 클래스를 일일이 만들어야 하는 번거로움
            2. 부가기능 코드 중복
    - 프록시 팩토리 빈의 한계
        1. 여러 클래스에 공통적인 부가기능을 제공하려면 중복이 발생한다.
            - 한 클래스 안에 있는 여러 메서드에 부가기능을 제공하는건 가능하다.
            - 비슷한 설정이 계속 반복돼서 나타나게 될 것
        2. TransactionHandler 객체가 프록시 팩토리 빈 개수만큼 만들어질 것이다.
            - 타깃 객체를 프로퍼티로 갖고있다(UserServiceImpl)
            - 그래서 타깃 객체가 여러개라면 그 개수만큼 빈을 선언하고 그만큼 객체가 생성 된다.
            - 싱글톤 빈으로 만드는게 좋겠지?
- 여기까지 요약
```
AOP 냄새가 나기 시작했다.
1 트랜잭션 코드의 분리 : AOP들어가기위한 준비
2 고립된 단위테스트
 - 1에서 분리한 내용의 장점을 테스트코드에 적용
 - 장점 : 코드 분리를 통해 비즈니스 로직에 대한 테스트를 더 쉽게 만들 수 있음(비즈니스 로직 UserService 코드와 부가기능 트랜잭션 경계설정 코드 분리)  
 - mockito는 내용 설명하다 생각나서 끼워넣은거 같은데 참 좋아보였다. 안쓴다니 아쉽다.
3. 다이내믹 프록시와 프록시 패턴
 - 프록시 패턴을 적용해서 부가기능을 추가했다.
  - 기존 코드에 영향 없이 필요한 기능을 추가했다.
 - 프록시를 팩토리 빈으로 등록했다.
  - 필요한 기능이 중복되는 경우 중복을 어느정도 막을 수 있다.
  - 하지만 타깃 객체가 여러개가 되는 경우 설정 중복을 피할 수 없다.
  - AOP 냄새가 난다.
AOP? 중복되는 기능을 코드 중복 없이 동작하게 하는 방법?
```