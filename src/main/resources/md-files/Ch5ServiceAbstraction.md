#5장. 서비스 추상화
---

## 요약 및 결론
> 템
    
## 책 내용
> 스프링이 어떻게 성격이 비슷한 여러 종류의 기술을 추상화 하고, 이를 일관된 방법으로 사용할 수 있도록 지원하는지 살펴본다.
>
> 전
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
        - 그러면 재시작 해야되잖아? 그냥 property로 갖고 준비해놨다가 필요할 때 켜면 되지요
## 7. 정리
- 전략 패턴
    - 로직에
    ```
    1. 
    ``` 