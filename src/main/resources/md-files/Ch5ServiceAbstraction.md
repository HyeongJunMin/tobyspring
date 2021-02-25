#5장. 서비스 추상화
---

## 요약 및 결론
> 서비스 추상화 잘 쓰세요
>
> @Transactional 쓰면 ㅇㅋ?
>
> 목 객체 : 테스트 객체가 정상적으로 실행되도록 도와주면서, 테스트 객체와 자신(mock)의 사이에서 일어나는 커뮤니케이션을 저장했다가 테스트 결과를 검증하는데 활용할 수 있도록 만들어진 객체
    
## 책 내용
> 기존 프로젝트에 스프링의 원칙들을 지키면서 신규 기능(등급관리, 메일발송)을 추가하는 과정
>
> 테스트를 목적으로 추상화 (UserService -> TestUserService, MailSender -> DummyMailSender)
> 



### 1. 사용자 레벨 관리 기능 추가
- 지금까지의 DAO는 단순 CRUD
- 레벨 서비스 추가
- ```
  1. 사용자의 레벨은 BASIC, SILVER, GOLD
  2. 처음 가입하면 BASIC
  3. 50회 이상 로그인하면 BASIC -> SILVER
  4. SILVER이면서 30번 이상 추천을 받으면 SILVER -> GOLD
  5. 레벨 변경 작업은 일정 주기를 가지고 일괄적으로 진행된다.
  ```

1. 필드 추가
    - 필드 몇 개 추가하는데 바꿀게 너무 많다;; 지저분하구만
    - Level Enum
    - ```
      public enum Level {
        BASIC(1), SILVER(2), GOLD(3);
        private final int value;
        // DB에 저장할 값을 넣어줄 생성자 선언
        Level(int value) {
          this.value = value;
        }
        public int intValue() {
          return value;
        }
        public static Level valueOf(int value) {
          switch (value) {
            case 1: return BASIC;
            case 2: return SILVER;
            case 3: return GOLD;
            default:
              throw new AssertionError("Unknown value : " + value);
          }
        }
      }
      ```
    - User 필드 추가
    - ```
      public class User {
        ...
        private Level level;
        private int login;
        private int recommend;
      }
      ```
    - UserDaoTest 테스트 수정
2. 사용자 수정 기능 추가
    - 여기까지도 거의 복습느낌
    - 수정 기능 테스트 추가
    - ``` 
      @Test
      public void update() {
        // when
        userDao.deleteAll();
        User user = new User("001", "name1", "password1", Level.BASIC, 1, 0);
        User finalUser = new User("002", "name1", "password1", Level.BASIC, 1, 0);
        userDao.add(user);
        userDao.add(finalUser);
        user.setName("히히힣");
        user.setPassword("dkaghghkehlsqlalfqjsgh");
        user.setLevel(Level.SILVER);
        user.setLogin(1000);
        user.setRecommend(999);
        userDao.update(user);
        // then
        User updatedUser = userDao.get(user.getId());
        checkSameUser(user, updatedUser);
        User updatedFinalUser = userDao.get(finalUser.getId());
        checkSameUser(finalUser, updatedFinalUser);
      }
      ```
    - 수정 기능
    - ```
      public void update(User user) {
        this.jdbcTemplate.update("update users set name = ?, password = ?, level = ?, login = ?, recommend = ? where id = ?"
            , user.getName(), user.getPassword(), user.getLevel().intValue(), user.getLogin(), user.getRecommend(), user.getId());
      }
      ```
3. UserService.upgradeLevels()
    - 사용자 관리 로직은 어디에 둬야 할까요?
        - UserDaoJdbc는 적당하지 않다. 데이터를 어떻게 가져오고 조작할지를 다루는 곳이기 때문
        - 비즈니스 로직 서비스를 제공해줄 클래스를 하나 추가해주자
    - UserService 추가
        - 나는 xml설정 안하기 때문에 @Service 사용
        - ```
          @Service
          public class UserService {          
            public static int LOGIN_COUNT_FOR_SILVER = 50;
            public static int RECOMMEND_COUNT_FOR_GOLD = 30;          
            @Autowired
            private UserDao userDao;          
            public void upgradeLevels() {
              List<User> userList = userDao.getAll();
              userList.forEach(user -> {
                if (Level.GOLD == user.getLevel()) {
                  return;
                }
                if (Level.BASIC == user.getLevel() && user.getLogin() >= LOGIN_COUNT_FOR_SILVER) {
                  user.setLevel(Level.SILVER);
                  userDao.update(user);
                  return;
                }
                if (Level.SILVER == user.getLevel() && user.getRecommend() >= RECOMMEND_COUNT_FOR_GOLD) {
                  user.setLevel(Level.GOLD);
                  userDao.update(user);
                  return;
                }
              });
            }          
            public void add(User user) {
              if (user.getLevel() == null) {
                user.setLevel(Level.BASIC);
              }
              userDao.add(user);
            }          
          }
          ```
4. UserService.add()
    - 처음 가입하는 사용자 BASIC레벨 설정은 -> 비즈니스 로직을 담고 있는 UserService가 적당
    - ```
      @Test
      public void addDefaultLevel() {
        User userWithLevel = userList.get(4);
        userService.add(userWithLevel);
        User userWithLevelRead = userDao.get(userWithLevel.getId());
        assertThat(userWithLevel.getLevel()).isEqualTo(userWithLevelRead.getLevel());
      }      
      @Test
      public void addGivenLevel() {
        User userWithoutLevel = userList.get(0);
        userWithoutLevel.setLevel(null);
        userService.add(userWithoutLevel);
        User userWithoutLevelRead = userDao.get(userWithoutLevel.getId());
        assertThat(userWithoutLevel.getLevel()).isEqualTo(userWithoutLevelRead.getLevel());
      }
      ```
5. 코드 개선
    - 무엇을 개선할까?
        1. 코드에 중복 없나요
        2. 가독성 괜찮나요
        3. 각 코드는 적당한 제자리에 있나요
        4. 변경에 대해 유연하게 대처할 수 있는 코드인가요
    - upgradeLevels()
        - 내가 스스로 바꿨던 코드
        - ```
          public void upgradeLevels() {
            List<User> userList = userDao.getAll();
            userList.forEach(user -> {
              if (Level.GOLD == user.getLevel()) {
                return;
              }
              if (Level.BASIC == user.getLevel() && user.getLogin() >= LOGIN_COUNT_FOR_SILVER) {
                user.setLevel(Level.SILVER);
                userDao.update(user);
                return;
              }
              if (Level.SILVER == user.getLevel() && user.getRecommend() >= RECOMMEND_COUNT_FOR_GOLD) {
                user.setLevel(Level.GOLD);
                userDao.update(user);
                return;
              }
            });
          }
          ```
        - 토비 아저씨가 개선한 코드
        - ```
          public void upgradeLevels() {
            List<User> userList = userDao.getAll();
            userList.forEach(user -> {
              if (canUpgradeLevel(user)) {
                upgradeLevel(user);
              }
            });
          }          
          private boolean canUpgradeLevel(User user) {
            Level currentLevel = user.getLevel();
            switch (currentLevel) {
              case GOLD: return false;
              case BASIC: return (user.getLogin() >= LOGIN_COUNT_FOR_SILVER);
              case SILVER: return (user.getRecommend() >= RECOMMEND_COUNT_FOR_GOLD);
              default: throw new IllegalArgumentException("Unknown level : " + currentLevel);
            }
          }          
          public void upgradeLevel(User user) {
            user.upgradeLevel();
            userDao.update(user);
          }
          ```
        - ```
          public class User {
            ...
            public void upgradeLevel() {
              Level nextLevel = this.level.getNextLevel();
              if (nextLevel == null) {
                throw new IllegalStateException(this.level + "은 업그레이드가 불가합니다.");
              }
              this.level = nextLevel;
            }
          }
          ```
    - User 테스트
        - User 도메인에도 메서드를 추가했으니 그에 맞는 테스트를 작성해보자.
        - 레벨 업그레이드가 잘 되는지, 안되는 업그레이드 시키면 기대에 맞는 예외가 발생 하는지
    - UserService 테스트
        - private check메서드로 중복제거
        - 하드코딩된 정수를 상수로 선언
        - 시작할 떄 부터 비슷하게 만들어서 패스
    - UserService 변화
        - 특정 이벤트 때문에 업그레이드 정책(조건)이 바뀔 수 있다면?
        - UserService를 인터페이스로 선언하고, 정책에 맞는 Service 구현체를 사용하도록 변경
        - 그러면 재시작 해야되잖아? 그냥 등급업 조건들을 property로 갖고 준비했다가 필요할 때 켜면 되지요

### 2. 트랜잭션 서비스 추상화
- 작업 중 장애발생하면 처리중이였던 작업은 모두 롤백하기로 결정
1. 모 아니면 도
    - 테스트 시나리오 중 DB서버를 다운 시키거나 네트워크를 끊을 수는 없다.
    - 장애가 발생했을 때 일어나는 예외가 던져지는 상황을 의도적으로 만들어본다.
    - 테스트용 UserService 대역
        - 테스트용으로 특별히 만든 UserService의 대역을 사용한다.
        - 테스트 클래스 안에 내부클래스로 UserService를 상속해서 만들도록 한다.
2. 트랜잭션 경계 설정
    - 모든 트랜잭션은 시작점과 끝점이 있다.
    - 끝점은 롤백과 커밋으로 나뉘지
    - JDBC 트랜잭션의 트랜잭션 경계 설정
        - 하나의 Connection을 사용하다가 닫는 사이에서 일어난다.
        - setAutoCommit(false)로 트랜잭션의 시작을 선언하고 commit() 또는 rollback()으로 트랜잭션을 종료하는 작업을 트랜잭션의 경계설정 이라고 한다.
    - UserService와 UserDao의 트랜잭션 문제
        - 코드 어디에도 트랜잭션 경계설정 코드가 존재하지 않기 때문에 트랜잭션이 적용되지 않았다.
        - 여러 쿼리들이 하나의 트랜잭션으로 묶이려면? DB커넥션 하나에서 돌아야된다고? 정말? 더읽어보자
    - 비즈니스 로직 내 트랜잭션 경계설정
        - 트랜잭션 경계를 upgradeLevels()메서드 안에 두려면 DB커넥션도 이 메서드 안에서 만들고 종료시킬 필요가 있다.
        - 으 뭔가 끔찍한 코드를 만들고 있어 금방 바꿔주겠지
        - ```
          // 단점들
          // 리소스의 깔끔한 처리를 도와줬던 jdbcTemplate 사용 못함
          // 비즈니스 로직을 담는 UserService가 몰라도 됐던 Connection을 알아야함
          // UserDao인터페이스에 Connection파라미터가 추가되면서 더이상 데이터 액세스 기술에 독립적일 수 업삳.
          public void upgradeLevels() throws Exception {
            1. DB Connection 생성
            2. 트랜잭션 시작
            try {
              3. DAO 메서드 호출
              4. 트랜잭션 커밋
            } catch (Exception e) {
              5. 트랜잭션 롤백
            } finally {
              6. DB 커넥션 종료
            }
          }
          ```
3. 트랜잭션 동기화
    - 둘다 아님 : UserService 메서드 안에서 경계를 설정해 관리하려면 지금까지의 깔끔한 코드는 포기해야할까? 트랜잭션 기능을 포기?
    - Connection 파라미터 제거
        - 트랜잭션 동기화 : 트랜잭션을 시작하기 위해 만든 Connection 객체를 특별한 저장소에 보관해 두고, 이후에 호출되는 DAO의 메서드에서는 저장된 Connection을 활용
    - 트랜잭션 동기화 적용
        - ```
          public void upgradeLevels() throws Exception {
            // 동기화작업 초기화
            TransactionSynchronizationManager.initSynchronization();
            // DB커넥션 생성, 트랜잭션 시작
            Connection c = DataSourceUtils.getConnection(dataSource);
            c.setAutoCommit(false);
            try {
              List<User> userList = userDao.getAll();
              userList.forEach(user -> {
                if (canUpgradeLevel(user)) {
                  upgradeLevel(user);
                }
              });
              c.commit();
            } catch (Exception e) {
              c.rollback();
              throw e;
            } finally {
              DataSourceUtils.releaseConnection(c, dataSource);
              TransactionSynchronizationManager.unbindResource(this.dataSource);
              TransactionSynchronizationManager.clearSynchronization();
            }
          }
          ```
    - JdbcTemplate과 트랜잭션 동기화
        - 특별히 시킨게 없으면 알아서 함
4. 트랜잭션 서비스 추상화
    - 기술과 환경에 종속되는 트랜잭션 경계설정 코드
        - 여러개의 DB를 사용하는 경우 DB Connection에 종속된 로컨 트랜잭션으로는 해결 불가
        - 글로벌 트랜잭션 방식이 필요함
        - 자바는 글로번 트랜잭션을 지원하는 트랜잭션 매니저를 지원하기 위해 JTA(Java Transaction Api) 제공
        - JNDI(Java Naming and Directory Interface) : 디렉터리 서비스에서 제공하는 데이터 및 객체를 발견(discover)하고 참고(lookup)하기 위한 자바 API
        - ```
          // JTA를 이용한 트랜잭션 코드 구조
          public void jtaTransaction() {
            InitialContext ctx = new InitialContext();
            UserTransaction tx = (UserTransaction)ctx.lookup(USER_TX_JNDI_NAME);
            tx.begin();
            Connection c = dataSource.getConnection();  // JNDI로 가져온 dataSource
            try {
              // 데이터 액세스 코드
              tx.commit();
            } catch (Exception e) {
              tx.rollback();
              throw e;
            } finally {
              c.close();
            }
          }
          ```
    - 하이버네이트 쓰는데는 또 다르게 설정해줘야 함
    - 트랜잭션 API의 의존관계 문제와 해결책
        - 원래 UserService는 UserDao 인터페이스에만 의존했다. -> DAO 클래스 구현 기술이 바껴도 영향없었음
        - 트랜잭션 때문에 다 망함
    - 스프링의 트랜잭션 서비스 추상화
        - 스프링은 트랜잭션 기술의 공통점을 담은 트랜잭션 추상화 기술을 제공한다.
        - ```
          public void upgradeLevels() throws Exception {
            // JDBC 트랜잭션 추상 객체 생성
            DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
            // 트랜잭션 시작
            TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
            try {
              List<User> userList = userDao.getAll();
              userList.forEach(user -> {
                if (canUpgradeLevel(user)) {
                  upgradeLevel(user);
                }
              });
              transactionManager.commit(status);
            } catch (Exception e) {
              transactionManager.rollback(status);
              throw e;
            }
          }
          ```
    - 트랜잭션 기술 설정의 분리
        - JTA를 이용하는 글로벌 트랜잭션으로 변경하려면?
            - DataSourceTransactionManager를 JTATransactionManager로 바꿔주기만 하면 된다.
            - 주요 자바 서버에서 제공하는 JTA 정보를 JNDI를 통해 자동으로 인식하는 기능을 갖고 있다.
            - 하이버네이트는 HibernateTransactionManager, JPA는 JPATransactionManager를 사용하면 된다.
        - UserService가 어떤 트랜잭션 매니저를 사용할지 결정하는건 DI 원칙에 위배된다.
        - transactionManager를 빈으로 등록해서 사용
        - ```
          // DataSource를 설정했던 DaoFactory에 transactionManager 빈 등록
          @Bean
          public DataSourceTransactionManager transactionManager() {
            return new DataSourceTransactionManager(dataSource());
          }
          ```
5. 왜 @Transactional은 안썼나?
    - @Transactional 클래스에 since 1.2로 돼있는데?
    - 써보니까 잘 작동한다.
    - ```
      @Transactional
      public void addAll(List<User> userList) {
        userList.forEach(user -> add(user));
      }
      ```
      ```
      @Test
      public void addAllIsTransactional() {
        userDao.deleteAll();
        User user1 = new User("001", "name1", "password1", Level.BASIC, 1, 0);
        User user2 = new User("002", "name2", "password2", Level.BASIC, 1, 0);
        User duplicatedUser = new User("002", "name3", "password3", Level.BASIC, 1, 0);
        List<User> originUserList = Arrays.asList(user1, user2, duplicatedUser);
        try {
          userDao.addAll(originUserList);
        } catch (Exception e) {
          // ignore
        }
        List<User> userList = userDao.getAll();
        assertThat(userList.size()).isEqualTo(0);
      }
      ```

### 3. 서비스 추상화와 단일 책임 원칙
1. 수직, 수평 계층구조와 의존관계
    - 같은 계층에서 수평적 분리(UserDao-UserService) : 같은 로직을 담았지만 내용에 따라 분리
    - 트랜잭션 추상화 : 애플리케이션 비즈니스 로직과는 다른 계층의 트랜잭션 기술(로우레벨) 분리
    - 결합도 낮추며 분리를 잘 했다는 내용
2. 단일 책임 원칙
    - 하나의 모듈이 바뀌는 이유는 한 가지여야 한다.
    - 하나의 모듈은 한 가지 책임을 가져야 한다.
3. 단일 책임 원칙의 장점
    - 변화에 유연하게 대처할 수 있다.
    - 변경이 필요할 때 수정 대상이 명확하다.
    - 스프링 DI가 없었다면 추상화를 했더라도 적지않은 코드간 결합이 남아있게 된다.
    - DI 짱이다 라는 내용
### 4. 메일 서비스 추상화
- 레벨이 업그레이드 되는 사용자에게는 안내 메일을 보내세요
1. JavaMail을 이용한 메일 발송 기능
    - User에 email필드 추가
    - JavaMail 메일 발송
        - ```
          private void sendUpgradeEMail(User user) {
            Properties properties = new Properties();
            properties.put("mail.smtp.host", "mail.naver.com");
            Session session = Session.getInstance(properties);
            MimeMessage message = new MimeMessage(session);
            try {
              message.setFrom(new InternetAddress("useradmin@naver.com"));
              message.addRecipient(Message.RecipientType.TO, new InternetAddress(user.getEmail()));
              message.setSubject("등업알림");
              String text = String.format("%s%s%s", "등급 ", user.getLevel().name(), " 로 올랐다.");
              message.setText(text);
              Transport.send(message);
            } catch (AddressException e) {
              throw new RuntimeException(e);
            } catch (MessagingException e) {
              throw new RuntimeException(e);
            }
          }
          ```
2. JavaMail이 포함된 코드의 테스트
    - 메일서버가 없으니까 발송못함
    - 테스트시점에 메일가는게 맞나? 아니지 생략하는게 좋겠죠
3. 테스트를 위한 서비스 추상화
    - 테스트에서는 JavaMail과 같은 인터페이스를 갖는 객체를 만들어서 사용하면 될 것
    - JavaMail을 이용한 테스트의 문제점
        - JavaMail의 핵심 API에는 DataSource처럼 인터페이스로 만들어져서 구현을 바꿀 수 있는게 없다.
        - 스프링이 JavaMail에 대한 추상화 기능을 제공한다.
    - 메일 발송 기능 추상화
    - DI 적용
        - ```
          // MailConfig에서 mailSender 빈 등록
          // spring-boot-starter-mail
          @Autowired
          private MailSender mailSender;
          private void sendUpgradeEMail(User user) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setFrom("useradmin@admin.com");
            message.setSubject("등업이다");
            message.setSubject("너 이제 " + user.getLevel().name());
            mailSender.send(message);
          }
          ```
    - 테스트와 서비스 추상화
        - 메일발송 기능도 트랜잭션으로 묶여있어야 하지만 예제니까 넘어간답니다.
4. 테스트 대역
    - 의존 오브젝트의 변경을 통한 테스트 방법
        - UserDaoTest는 UserDao의 기능에만 관심이 있지만 DB없이는 제 기능을 수행할 수 없기 때문에 가벼운 DB를 사용했다.
        - 비슷한 목적으로 UserService에서도 DummyMailSender를 사용했다.
        - 결론 : DI를 적용해야 세상 편하다.
    - 테스트 대역의 종류와 특징
        - 테스트 환경을 만들어 주기 위한 객체를 통틀어 테스트 대역이라고 부른다.
            - 테스트 대상이 되는 객체의 기능에 충실하면서 빠르고 자주 테스트를 실행하기 위해 사용
            - 테스트 스텁(test stub) : 대표적인 테스트 대역
                - DummyMailSender는 가장 단순하고 심플한 테스트 스텁의 예
        - 목 객체(mock object)
            - 테스트 객체가 정상적으로 실행되도록 도와주면서, 테스트 객체와 자신의 사이에서 일어나는 커뮤니케이션을 저장했다가 테스트 결과를 검증하는데 활용할 수 있게 해준다.
            - 의존/협력 객체
    - 목 객체를 이용한 테스트
        - 간접적인 출력 값까지 확인할 수 있다.
        - ```
          // UserServiceTest.java
          @Getter
          static class MockMailSender implements MailSender {
            private List<String> requests = new ArrayList();
            @Override
            public void send(SimpleMailMessage simpleMailMessage) throws MailException {
              // 전송요청이 들어온 메일 주소를 List에 저장한다.
              requests.add(simpleMailMessage.getTo()[0]);
            }
            @Override
            public void send(SimpleMailMessage... simpleMailMessages) throws MailException {
              // ignore
            }
          }          
          @Test
          @DirtiesContext // 컨텐스트의 DI 설정을 변경하는 테스트라는 것을 표현
          public void upgradeLevelsWithMockMailSender() throws Exception {
            userDao.deleteAll();
            userList.forEach(user -> userDao.add(user));
            MockMailSender mockMailSender = new MockMailSender();
            userService.setMailSender(mockMailSender);
            userService.upgradeLevels();
            checkLevelUpgraded(userList.get(0), false);
            checkLevelUpgraded(userList.get(1), true);
            checkLevelUpgraded(userList.get(2), false);
            checkLevelUpgraded(userList.get(3), true);
            checkLevelUpgraded(userList.get(4), true);
            List<String> requests = mockMailSender.getRequests();
            assertThat(requests.size()).isEqualTo(3);
            assertThat(requests.containsAll(Arrays.asList(userList.get(1).getEmail(), userList.get(3).getEmail(), userList.get(4).getEmail())));
          }
          ```
## 5. 정리
- 비즈니스 로직을 담은 UserService 클래스를 만들고 트랜잭션을 적용하면서 스프링의 서비스 추상화에 대해 살펴보았다.
- 비즈니스 로직을 담은 코드는 데이터 액세스 로직을 담은 코드와 깔끔하게 분리되는 것이 바람직하다.
- ```
  1. 비즈니스 로직 코드 또한 내부적으로 책임과 역할에 따라 깔끔하게 메서드로 정리돼야 한다.
  2. DAO 기술 변화에 서비스 계층의 코드가 영향을 받지 않도록 인터페이스와 DI를 잘 활용해서 결합도를 낮춰야 한다.
  ```
- DAO를 사용하는 비즈니스 로직에는 단위 작업을 보장해주는 트랜잭션이 필요하다.
- ```
  1. 트랜잭션 경계 설정 : 트랜잭션의 시작과 종료를 지정하는 일. (비즈니스 로직 안에서 일어나는 경우가 많음)
  2. 스프링이 제공하는 트랜잭션 동기화 기법을 활용하는 것이 편리하다.
  3. 스프링의 트랜잭션 서비스 추상화를 이용하는 이유 : 트랜잭션 경계 설정 코드가 비즈니스 로직 코드에 영향을 주지 않게 하기 위함 
  ```
- 서비스 추상화
- ```
  1. 로우레벨 트랜잭션 기술과 API 변화에 상관없이 일관된 API를 가진 추상화 계층을 도입한다.
  2. 테스트를 편리하게 작성하도록 도와주는 것만으로도 서비스 추상화는 가치가 있다.
  ```
- 테스트 대역
- ```
  1. 테스트 대역 : 테스트 대상이 사용하는 의존 객체를 대체할 수 있도록 만든 객체
  2. 대상 객체가 원활하게 동작할 수 있도록 도우면서 테스트를 위해 간접적인 정보를 제공해주기도 한다.
  3. 목 객체 : 테스트 대역 중에서 테스트 대상으로부터 전달받은 정보를 검증할 수 있도록 설계된 것
  ```