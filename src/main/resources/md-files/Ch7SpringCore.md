#7장. 스프링 핵심 기술의 응용
---

## 요약 및 결론
> 
    
## 책 내용
> 스프링의 모든 기술은 결국 객체지향 언어의 장점을 적극적으로 활용해서 코드를 작성하도록 도와주는 것이다.
>
> 7장에서는 IoC/DI, 서비스 추상화, AOP를 애플리케이션 개발에 활용해서 새로운 기능을 만들어보고 스프링의 가치와 사용자에게 요구하는 것에 대해 살펴본다.
>
> 디폴트 의존관계 : 외부에서 DI 받지 않는 경우 기본적으로 자동 적용되는 의존관계(593p)


### 1. SQL과 DAO의 분리
```
UserDao로 돌아가보면...
충분한 개선이 이뤄졌었지만
테이블이 바뀔 때 마다 SQL문장을 담고있는 DAO에 수정이 발생할것이다.
SQL을 DAO에서 분리하면 더 좋겠다.
```
1. XML 설정을 이용한 분리
    - 가장 손쉽게 생각해볼 수 있는 방법
    - 개별 SQL 프로퍼티 방식... 영 별로다
        - <details markdown="1">
          <summary>코드 접기/펼치기</summary>
          <pre>
          @Bean
          public UserDao userDao() {
            UserDaoJdbc userDaoJdbc = new UserDaoJdbc(dataSource());
            userDaoJdbc.setSqlAdd("insert into users(id, name, password, level, login, recommend, email) values(?, ?, ?, ?, ?, ?, ?)");
            return userDaoJdbc;
          }
          @Setter
          public class UserDaoJdbc implements UserDao {
            private String sqlAdd;
            public void add(final User user) {
              this.jdbcTemplate.update(this.sqlAdd
                      , user.getId(), user.getName(), user.getPassword(), user.getLevel().intValue(), user.getLogin()
                      , user.getRecommend(), user.getEmail());
            }
          }
          </pre>
          </details>
    - SQL 맵 프로퍼티 방식
        - SQL을 하나의 컬렉션으로 담아주는 방법... 
        - 못난방법이 뭐가있을까 생각하다 억지로 만들어본 느낌
        - 진짜 쓸거면 쿼리키를 상수로 등록해주든지 했겠지 오타가 날 수도 있으니 안좋다 라고 대충넘기지 않고
2. SQL 제공 서비스
    - 위처럼 SQL과 DI 설정정보가 섞여있으면 보기에도 지저분하고 관리하기에도 좋지 않다. 독립적인 SQL 제공 서비스를 만들어보자.
    - SQL 서비스 인터페이스
        - <details markdown="1">
          <summary>코드 접기/펼치기</summary>
          <pre>
          public interface SqlService {
            String getSql(String key) throws SqlRetrievalFailureException;
          }
          </pre>
          </details>
    - SQL 조회 실패 시 예외
        - <details markdown="1">
          <summary>코드 접기/펼치기</summary>
          <pre>
          public class SqlRetrievalFailureException extends RuntimeException {
            public SqlRetrievalFailureException(String message) {
              super(message);
            }
            public SqlRetrievalFailureException(String message, Throwable cause) {
              super(message, cause);
            }
          }
          </pre>
          </details>
    - 스프링 설정을 사용하는 단순 SQL 서비스
        - ```
          @Bean
          public SqlService sqlService() {
            SimpleSqlService sqlService = new SimpleSqlService();
            Map<String, String> sqlMap = new HashMap();
            sqlMap.put(SqlService.USER_ADD, "insert into users(id, name, password, level, login, recommend, email) values(?, ?, ?, ?, ?, ?, ?)");
            sqlMap.put(SqlService.USER_GET, "select * from users where id = ?");
            sqlMap.put(SqlService.USER_GET_ALL, "select * from users order by id");
            sqlMap.put(SqlService.USER_DELETE_ALL, "delete from users");
            sqlMap.put(SqlService.USER_GET_COUNT, "select count(*) from users");
            sqlMap.put(SqlService.USER_UPDATE, "update users set name = ?, password = ?, level = ?, login = ?, recommend = ?, email = ? where id = ?");
            sqlService.setSqlMap(sqlMap);
            return sqlService;
          }
          ```
### 2. 인터페이스의 분리와 자기참조 빈
1. XML 파일 매핑
    - 스프링 XML 설정파일에 SQL정보를 넣어두는건 좋은 방법이 아니다.
    - JAXB
        - XML 문서정보를 거의 동일한 구조의 객체로 직접 매핑해준다.
    - SQL 맵을 위한 스키마 작성과 컴파일
        - ```
          xjc -p toby.service.sql sql-scheme.xsd -d c:\
          xjc -p 패키지 파일명 -d 대상디렉토리
          ```
    - 언마샬링
        - 생성된 매핑 클래스를 적용하기 전 학습 테스트
        - <details markdown="1">
          <summary>코드 접기/펼치기</summary>
          <pre>
          public class SqlMapLearningTest {          
            @Test
            public void readSqlMap() throws JAXBException {
              String contextPath = Sqlmap.class.getPackage().getName();
              JAXBContext context = JAXBContext.newInstance(contextPath);
              Unmarshaller unmarshaller = context.createUnmarshaller();
              // 경로 : resources/sql/
              InputStream resourceAsStream = getClass().getResourceAsStream("/sql/sql-map-test.xml");
              Sqlmap sqlMap = (Sqlmap) unmarshaller.unmarshal(resourceAsStream);
              List<SqlType> sqlList = sqlMap.getSql();
              assertThat(sqlList.size()).isEqualTo(3);
              assertThat(sqlList.get(0).getKey()).isEqualTo("add");
              assertThat(sqlList.get(0).getValue()).isEqualTo("insert");
            }
          }
          </pre>
          </details>
2. XML 파일을 이용하는 SQL 서비스
    - SQL 맵 XML 파일
        - sql-map.xml
    - XML SQL 서비스
        - 생성자 초기화 방법을 사용하는 XmlSqlService 클래스
        - ```
          public class XmlSqlService implements SqlService {
            
            private Map<String, String> sqlMap = new HashMap();
            
            public XmlSqlService() {
              String contextPath = Sqlmap.class.getPackage().getName();
              try {
                JAXBContext context = JAXBContext.newInstance(contextPath);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                InputStream resourceAsStream = getClass().getResourceAsStream("/sql/sql-map.xml");
                Sqlmap unmarshal = (Sqlmap) unmarshaller.unmarshal(resourceAsStream);
                unmarshal.getSql().forEach(map -> this.sqlMap.put(map.getKey(), map.getValue()));
              } catch (JAXBException e) {
                throw new RuntimeException(e);
              }
            }
            
            @Override
            public String getSql(String key) throws SqlRetrievalFailureException {
              String sql = sqlMap.get(key);
              if (sql == null) {
                throw new SqlRetrievalFailureException(key + "에 대한 SQL을 찾을 수 없습니다.");
              }
              return sql;
            }
            
          }
          ```
3. 빈의 초기화 작업
    - XmlSqlService 개선필요
        - 생성자에 예외처리블럭 안좋음. 초기 상태를 가진 객체를 만들어 두고 별도의 초기화 메서드를 사용해야함
        - 읽어들일 파일의 위치와 이름이 하드코딩되어있음. DI로 설정할 수 있도록 만들어주어야 함
        - ```
          public class XmlSqlService implements SqlService {
            ...
            private String sqlmapFile;
            
            public XmlSqlService(String sqlmapFile) {
              this.sqlmapFile = sqlmapFile;
            }
          
            @PostConstruct  // 빈 객체를 생성하고 DI작업을 마친 뒤에 @PostConstruct 메서드를 실행함
            public void loadSql() {
              ...
            }
          }
          ```
4. 변화를 위한 준비 : 인터페이스 분리
    - XmlSqlService는 아직 확장할 영역이 많다.
        - XML대신 다른 포맷에서 쿼리를 읽어오게 하려면? XmlSqlService코드를 직접 수정해야한다.
    - 책임에 따른 인터페이스 정의
        1. SQL정보를 외부의 리소스로부터 읽어오는 것
        2. 읽어온 SQL을 보관해두고 있다가 필요할 때 제공해주는 것
        3. 한번 가져온 SQL을 필요에 따라 수정할 수 있게 하는 것
    - ```
      // SqlRegistry
      public interface SqlRegistry {
        void registerSql(String key, String sql);
        String findSql(String key) throws SqlNotFoundException;
      }
      // SqlReader
      public interface SqlReader {
        void read(SqlRegistry sqlRegistry);
      }
      ```
5. 자기참조 빈으로 시작하기
    - 다중 인터페이스 구현과 간접 참조
        - XmlSqlService 클래스 하나가 SqlReader, SqlRegistry, SqlService 세 개의 인터페이스를 구현하도록 만든다.
    - 인터페이스를 이용한 분리
        - ```
          public class XmlSqlService implements SqlService, SqlRegistry, SqlReader {          
            private Map<String, String> sqlMap = new HashMap();          
            private SqlRegistry sqlRegistry;          
            private SqlReader sqlReader;          
            private String sqlmapFile;          
            public XmlSqlService(SqlRegistry sqlRegistry, SqlReader sqlReader, String sqlmapFile) {
              this.sqlRegistry = sqlRegistry;
              this.sqlReader = sqlReader;
              this.sqlmapFile = sqlmapFile;
            }          
            @PostConstruct  // 빈 객체를 생성하고 DI작업을 마친 뒤에 @PostConstruct 메서드를 실행함
            public void loadSql() {
              this.sqlReader.read(this.sqlRegistry);
            }          
            @Override
            public String getSql(String key) throws SqlRetrievalFailureException {
              try {
                return this.sqlRegistry.findSql(key);
              } catch (SqlNotFoundException e) {
                throw new SqlRetrievalFailureException(e.getMessage(), e.getCause());
              }
            }          
            @Override
            public void registerSql(String key, String sql) {
              sqlMap.put(key, sql);
            }          
            @Override
            public String findSql(String key) throws SqlNotFoundException {
              String sql = sqlMap.get(key);
              if (sql == null) {
                throw new SqlRetrievalFailureException(key + "에 대한 SQL을 찾을 수 없습니다.");
              }
              return sql;
            }          
            @Override
            public void read(SqlRegistry sqlRegistry) {
              String contextPath = Sqlmap.class.getPackage().getName();
              try {
                JAXBContext context = JAXBContext.newInstance(contextPath);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                InputStream resourceAsStream = getClass().getResourceAsStream(sqlmapFile);
                Sqlmap unmarshal = (Sqlmap) unmarshaller.unmarshal(resourceAsStream);
                unmarshal.getSql().forEach(map -> sqlRegistry.registerSql(map.getKey(), map.getValue()));
              } catch (JAXBException e) {
                throw new RuntimeException(e);
              }
            }
          }
          ```
    - 자기참조 빈 설정
        - 빈 등록은 어떻게 할건가?
        - 자기참조 뭔가 이상함
        - <details markdown="1">
          <summary>코드 접기/펼치기</summary>
          <pre>
          @Bean
          public SqlService sqlService() {
            HashMapSqlRegistry sqlRegistry = new HashMapSqlRegistry();
            JaxbXmlSqlReader sqlReader = new JaxbXmlSqlReader("/sql/sql-map.xml");
            XmlSqlService sqlService = new XmlSqlService(sqlRegistry, sqlReader);
            return sqlService;
          }
          </pre>
          </details>
6. 디폴트 의존관계
    - 확장 가능한 기반 클래스
        - SqlRegistry와 SqlReader를 이용하는 가장 간단한 SqlService 구현 클래스를 만들어본다.
        - <details markdown="1">
          <summary>코드 접기/펼치기</summary>
          <pre>
          @Bean
          public SqlService sqlService() {
            BaseSqlService sqlService = new BaseSqlService();
            sqlService.setSqlRegistry(sqlRegistry());
            sqlService.setSqlReader(sqlReader());
            return sqlService;
          }
          
          @Bean
          public SqlRegistry sqlRegistry() {
            return new HashMapSqlRegistry();
          }
          
          @Bean
          public SqlReader sqlReader() {
            return new JaxbXmlSqlReader("/sql/sql-map.xml");
          }
          </pre>
          </details>
    - 디폴트 의존관계를 갖는 빈 만들기
        - BaseSqlService는 sqlReader와 sqlRegistry 프로퍼티의 DI를 통해 의존관계를 자유롭게 변경해가면서 기능을 확장할 수 있다.
        - 3개의 빈을 등록해줘야 한다는 점이 귀찮게 느껴진다.
        - 디폴트 의존관계 : 외부에서 DI 받지 않는 경우 기본적으로 자동 적용되는 의존관계
        - sqlmapFile 역시 디폴트값을 저장해두고 교체할 수 있도록 setter를 추가해준다.
        - DefaultSqlService는 BaseSqlService를 상속받았기 때문에 빈 등록 시점에 필요한 프로퍼티로 교체해 줄 수 있다.
        - <details markdown="1">
          <summary>코드 접기/펼치기</summary>
          <pre>
          @Bean
          public SqlService sqlService() {
            DefaultSqlService sqlService = new DefaultSqlService();
            return sqlService;
          }
          public class DefaultSqlService extends BaseSqlService {
            public DefaultSqlService() {
              setSqlRegistry(new HashMapSqlRegistry());
              setSqlReader(new JaxbXmlSqlReader());
            }
          }
          </pre>
          </details>

### 3. 서비스 추상화 적용
### 4. 인터페이스 상속을 통한 안전한 기능확장
### 5. DI를 이용해 다양한 구현 방법 적용하기
### 6. 스프링 3.1의 DI
### 7. 정리
- 