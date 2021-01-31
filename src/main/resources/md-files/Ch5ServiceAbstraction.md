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
          ```
4. UserService.add()
    - 처음 가입하는 사용자 BASIC레벨 설정은 어디가 좋을까?
## 7. 정리
- 전략 패턴
    - 로직에
    ```
    1. 
    ``` 