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
          window에서
          xjc -p toby.service.sql sql-scheme.xsd -d c:\
          xjc -p 패키지 파일명 -d 대상디렉토리
          mac에서
          xjc 스키마파일디렉토리 -d 대상디렉토리
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
1. OXM 서비스 추상화
    - OXM?
        - OXM(Object-XML Mapping) : XML과 자바 객체를 매핑해서 상호 변환해주는 기술
        - OXM 기술 : Castor XML, JiBX, XmlBeans, Xstream
        - 기능이 같은 여러 기술이 존재한다? -> 서비스 추상화
    - OXM 서비스 인터페이스
        - 스프링이 제공하는 OXM 추상화 서비스 인터페이스 : Marshaller, Unmarshaller
    - JAXB 구현 테스트
        - <details markdown="1">
          <summary>코드 접기/펼치기</summary>
          <pre>
          // 빈 등록
          @Bean
          public Unmarshaller unmarshaller() {
            Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
            marshaller.setContextPath("toby.service.sql");
            return marshaller;
          }
          // 테스트
          @RunWith(SpringRunner.class)
          @SpringBootTest
          public class OxmTest {          
            @Autowired
            private Unmarshaller unmarshaller;          
            @Test
            public void unmarshallSqlMap() throws IOException {
              StreamSource source = new StreamSource(getClass().getResourceAsStream("/sql/sql-map.xml"));
              Sqlmap sqlMap = (Sqlmap) unmarshaller.unmarshal(source);
              List sqlList = sqlMap.getSql();
              assertThat(sqlList.size()).isEqualTo(6);
              assertThat(sqlList.get(0).getKey()).isEqualTo("userAdd");
              assertThat(sqlList.get(0).getValue()).isEqualTo("insert into users(id, name, password, level, login, recommend, email) values(?, ?, ?, ?, ?, ?, ?)");
            }          
          }
          </pre>
    - Castor 구현 테스트
        - 한참 찾아보다가 4.3.13에서 deprecated됐다는 내용 찾음
        - ```
          Deprecated. 
          as of Spring Framework 4.3.13, due to the lack of activity on the Castor project
          ```
2. OXM 서비스 추상화 적용
    - 멤버 클래스를 참조하는 통합 클래스
        - <details markdown="1">
          <summary>OxmSqlService 코드 접기/펼치기</summary>
          public class OxmSqlService implements SqlService {          
            private SqlRegistry sqlRegistry = new HashMapSqlRegistry();          
            public void setSqlRegistry(SqlRegistry sqlRegistry) {
              this.sqlRegistry = sqlRegistry;
            }          
            public void setUnmarshaller(Unmarshaller unmarshaller) {
              this.oxmSqlReader.setUnmarshaller(unmarshaller);
            }          
            public void sqlmapFile(String sqlmapFile) {
              this.oxmSqlReader.setSqlmapFile(sqlmapFile);
            }          
            @PostConstruct
            public void loadSql() {
              this.oxmSqlReader.read(this.sqlRegistry);
            }          
            public String getSql(String key) throws SqlRetrievalFailureException {
              try {
                return this.sqlRegistry.findSql(key);
              } catch (SqlNotFoundException e) {
                throw new SqlRetrievalFailureException(e.getMessage(), e.getCause());
              }
            }          
            private final OxmSqlReader oxmSqlReader = new OxmSqlReader();          
            @Setter
            private class OxmSqlReader implements SqlReader {
              private Unmarshaller unmarshaller;
              private static final String DEFAULT_SQLMAP_FILE = "/sql/sql-map.xml";
              private String sqlmapFile = DEFAULT_SQLMAP_FILE;          
              @Override
              public void read(SqlRegistry sqlRegistry) {
                try {
                  StreamSource source = new StreamSource(getClass().getResourceAsStream(sqlmapFile));
                  Sqlmap sqlMap = (Sqlmap) unmarshaller.unmarshal(source);
                  sqlMap.getSql().forEach((sql) -> sqlRegistry.registerSql(sql.getKey(), sql.getValue()));
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              }
            }          
          }
          <pre>
          </pre>
          </details>
        - <details markdown="1">
          <summary>SqlService bean 코드 접기/펼치기</summary>
          @Bean
          public SqlService sqlService() {
            OxmSqlService sqlService = new OxmSqlService();
            sqlService.setUnmarshaller(unmarshaller());
            return sqlService;
          }
          @Bean
          public Unmarshaller unmarshaller() {
            Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
            marshaller.setContextPath("toby.service.sql");
            return marshaller;
          }
          <pre>
          </pre>
          </details>
    - 위임을 이용한 BaseSqlService의 재사용
        - loadSql(), getSql() 메서드 중복이 있다(OxmSqlService, DefaultSqlService)
        - 구현 로직은 BaseSqlService에만 두는 방식으로 변경해본다.
        - 관련 로직이 변경되면 BaseSqlService만 수정하면 된다.
        - <details markdown="1">
          <summary>수정된 OxmSqlService 코드 접기/펼치기</summary>
          <pre>
          public class OxmSqlService implements SqlService {
            private final BaseSqlService baseSqlService = new BaseSqlService();
            ...
            @PostConstruct
            public void loadSql() {
              this.baseSqlService.setSqlReader(this.oxmSqlReader);
              this.baseSqlService.setSqlRegistry(this.sqlRegistry);
              this.baseSqlService.loadSql();
            }
            public String getSql(String key) throws SqlRetrievalFailureException {
              return this.baseSqlService.getSql(key);
            }
            ...
          }
          </pre>
          </details>
3. 리소스 추상화
    - 문제점
        - 클래스패스에 존재하는 파일만 사용할 수 있다.
        - 해결방법은?
    - 리소스
        - 스프링은 자바의 리소스 접근 API를 추상화해서 Resource라는 추상화 인터페이스를 정의했다.
        - 스프링의 거의 모든 API에서 외부 리소스 정보가 필요할 때 Resource 추상화를 이용한다.
        - 하지만 Resource는 빈이 아니라 값으로 취급된다.
    - 리소스 로더
        - 접두어를 이용해 Resource 객체를 선언하는 방법
        - 접두어 -> file:, classpath:, 없음, http:
    - Resource를 이용해 XML 파일 가져오기
        - <details markdown="1">
          <summary>코드 접기/펼치기</summary>
          <pre>
          public class OxmSqlService implements SqlService {          
            private Resource sqlmap = new ClassPathResource("/sql/sql-map.xml", UserDao.class);          
            public void setSqlmap(Resource sqlmap) {
              this.sqlmap = sqlmap;
            }
            ...
            @Setter
            private class OxmSqlReader implements SqlReader {
              ...
              public void read(SqlRegistry sqlRegistry) {
                try {
                  StreamSource source = new StreamSource(sqlmap.getInputStream());
                  ...
                } catch (IOException e) {
                  throw new RuntimeException(sqlmap.getFilename() + "을 가져올 수 없습니다.", e);
                }
              }
            }
          }
          </pre>
          </details>
### 4. 인터페이스 상속을 통한 안전한 기능확장
1. DI와 기능의 확장
    - DI를 의식하는 설계
        - 적절한 책임에 따라 객체를 분리해줘야 한다.
        - 항상 의존 객체는 자유롭게 확장될 수 있다는 점을 염두해야 한다.
        - DI란 결국 미래를 프로그래밍 하는 것이다.
    - DI와 인터페이스 프로그래밍
        - 가능한 한 인터페이스를 사용해서 느슨하게 연결돼야 한다.
        - ```
          이유
          1. 다형성을 얻기 위해 : 여러 개의 구현을 바꿔가며 사용할 수 있게 하는 것.
          2. 인터페이스 분리 원칙을 통해 클라이언트와 의존 객체 간 관계를 명확하게 해줄 수 있기 때문
          ```
2. 인터페이스 상속
    - 인터페이스 분리 원칙이 주는 장점 : 모든 클라이언트가 자신의 관심에 따른 접근 방식을 불필요한 간섭 없이 유지할 수 있다.
    - SqlRegistry에 이미 등록된 SQL을 변경할 수 있는 기능을 넣어서 확장하고싶을 때? -> SqlRegistry를 상속받는 인터페이스를 정의한다.
    - 잘 적용된 DI는 잘 설계된 객체 의존관계에 달려있다.
    - DI와 객체지향 설계는 서로 밀접한 관계를 맺고 있다.
    - ```
      public interface UpdatableSqlRegistry extends SqlRegistry {
        void updateSql(String key, String sql) throws SqlUpdateFailureException;
        void updateSql(Map<String, String> sqlmap) throws SqlUpdateFailureException;
      }
      ```
    - ```
      @Slf4j
      public class MyUpdatableSqlRegistry implements UpdatableSqlRegistry {
        public void updateSql(String key, String sql) throws SqlUpdateFailureException { log.info("update sql. key : {}, value : {}", key, sql); }
        public void updateSql(Map<String, String> sqlmap) throws SqlUpdateFailureException { log.info("update sql. sqlmap : {}", sqlmap); }
        private Map<String, String> sqlMap = new HashMap();
        public void registerSql(String key, String sql) { sqlMap.put(key, sql); }
        public String findSql(String key) throws SqlNotFoundException { ... }      
      }
      ```
    - ```
      // 빈 등록
      @Bean
      public SqlService sqlService() {
        OxmSqlService sqlService = new OxmSqlService();
        sqlService.setUnmarshaller(unmarshaller());
        sqlService.setSqlRegistry(sqlRegistry());
        return sqlService;
      }      
      @Bean
      public SqlRegistry sqlRegistry() {
        return new MyUpdatableSqlRegistry();
      }
      ```
### 5. DI를 이용해 다양한 구현 방법 적용하기
1. ConcurrentHashMap을 이용한 수정 가능 SQL 레지스트리
    - ConcurrentHashMap은 데이터 조작 시 전체 데이터에 대해 락을 걸지 않고 조회는 락을 전혀 사용하지 않는다.
    - 수정 가능 SQL 레지스트리 테스트
        - 동시성에 대한 테스트는 배제
        - ```
          // ConcurrentHashMap을 이용한 SQL 레지스트리 테스트
          public class ConcurrentHashMapSqlRegistryTest {          
            UpdatableSqlRegistry updatableSqlRegistry;          
            private final String KEY_1 = "KEY1";
            private final String KEY_2 = "KEY2";
            private final String KEY_3 = "KEY3";
            private final String SQL_1 = "SQL1";
            private final String SQL_2 = "SQL2";
            private final String SQL_3 = "SQL3";
                    
            @Before
            public void setUp() throws Exception {
              updatableSqlRegistry = new ConcurrentHashMapSqlRegistry();
              updatableSqlRegistry.registerSql(KEY_1, SQL_1);
              updatableSqlRegistry.registerSql(KEY_2, SQL_2);
              updatableSqlRegistry.registerSql(KEY_3, SQL_3);
            }
          
            @Test
            public void find() {
              checkFindResult(SQL_1, SQL_2, SQL_3);
            }
          
            private void checkFindResult(String expected1, String expected2, String expected3) {
              assertThat(updatableSqlRegistry.findSql(KEY_1)).isEqualTo(expected1);
              assertThat(updatableSqlRegistry.findSql(KEY_2)).isEqualTo(expected2);
              assertThat(updatableSqlRegistry.findSql(KEY_3)).isEqualTo(expected3);
            }
          
            @Test(expected = SqlNotFoundException.class)
            public void unknownKey() {
              updatableSqlRegistry.findSql("unknown");
            }
          
            @Test
            public void updateSingle() {
              String modifiedSql = "MODIFIED";
              updatableSqlRegistry.updateSql(KEY_2, modifiedSql);
              checkFindResult(SQL_1, modifiedSql, SQL_3);
            }
          
            @Test
            public void updateMulti() {
              String modifiedSql1 = "modified1";
              String modifiedSql2 = "modified2";
              Map<String, String> sqlMap = new HashMap();
              sqlMap.put(KEY_1, modifiedSql1);
              sqlMap.put(KEY_3, modifiedSql2);
              updatableSqlRegistry.updateSql(sqlMap);
              checkFindResult(modifiedSql1, SQL_2, modifiedSql2);
            }
          
            @Test(expected = SqlUpdateFailureException.class)
            public void updateWithNotExistingKey() {
              updatableSqlRegistry.updateSql("notExistingKey", "modified2");
            }
          
          }
          ```
    - 수정 가능 SQL 레지스트리 구현
        - ```
          public class ConcurrentHashMapSqlRegistry implements UpdatableSqlRegistry {
          
            private final ConcurrentHashMap<String, String> sqlMap = new ConcurrentHashMap<>();
          
            @Override
            public String findSql(String key) throws SqlNotFoundException {
              String sql = sqlMap.get(key);
              if (sql == null) {
                throw new SqlNotFoundException("not found. key : " + key);
              }
              return sql;
            }
          
            @Override
            public void registerSql(String key, String value) {
              sqlMap.put(key, value);
            }
          
            @Override
            public void updateSql(String key, String newValue) throws SqlUpdateFailureException {
              String foundSql = sqlMap.get(key);
              if (foundSql == null) {
                throw new SqlUpdateFailureException();
              }
              sqlMap.put(key, newValue);
            }
          
            @Override
            public void updateSql(Map<String, String> sqlMap) throws SqlUpdateFailureException {
              sqlMap.forEach((k, v) -> updateSql(k, v));
            }
          
          }
          ```
2. 내장형 데이터베이스를 이용한 SQL 레지스트리 만들기
    - 스프링의 내장형 DB 지원 기능
        - 내장형 DB : 애플리케이션과 함께 시작되고 종료되는 DB
        - EmbeddedDatabase 인터페이스 제공        
    - 내장형 DB 빌더 학습 테스트
        - SimpleJdbcTemplate deprecated -> JdbcTemplate 사용
        - <details markdown="1">
          <summary>내장 DB DDL, DML 접기/펼치기</summary>
          <pre>
          // embedded-embedded-schema.sql
          CREATE TABLE SQLMAP (
            KEY_ VARCHAR(100) PRIMARY KEY,
            SQL_ VARCHAR(100) NOT NULL
          );
          // embedded-data.sql
          INSERT INTO SQLMAP(KEY_, SQL_) values ('KEY1', 'SQL1');
          INSERT INTO SQLMAP(KEY_, SQL_) values ('KEY2', 'SQL2');
          </pre>
          </details>
        - ```
          public class EmbeddedDBTest {
          
            EmbeddedDatabase db;
            JdbcTemplate jdbcTemplate;
          
            @Before
            public void setUp() {
              db = new EmbeddedDatabaseBuilder()
                      .setType(EmbeddedDatabaseType.HSQL)
                      .addScript("/embedded-embedded-schema.sql")
                      .addScript("/embedded-data.sql")
                      .build();
              jdbcTemplate = new JdbcTemplate(db);
            }
          
            @After
            public void tearDown() {
              // 테스트 후 DB 종료
              db.shutdown();
            }
          
            @Test
            public void initData() {
              Integer sqlmapCount = jdbcTemplate.queryForObject("select count(*) from sqlmap", Integer.class);
              assertThat(sqlmapCount).isEqualTo(2);
              List<Map<String, Object>> list = jdbcTemplate.queryForList("select * from sqlmap order by key_");
              Map<String, Object> firstObject = list.get(0);
              assertThat(firstObject.get("key_")).isEqualTo("KEY1");
              assertThat(firstObject.get("sql_")).isEqualTo("SQL1");
              Map<String, Object> secondObject = list.get(1);
              assertThat(secondObject.get("key_")).isEqualTo("KEY2");
              assertThat(secondObject.get("sql_")).isEqualTo("SQL2");
            }
          
            @Test
            public void insert() {
              jdbcTemplate.update("insert into sqlmap(key_, sql_) values(?, ?)", "KEY3", "SQL3");
              Integer sqlmapCount = jdbcTemplate.queryForObject("select count(*) from sqlmap", Integer.class);
              assertThat(sqlmapCount).isEqualTo(3);
            }
          
          }
          ```
    - 내장형 DB를 이용한 SQL 레지스트리 만들기
        - EmbeddedDatabaseBuilder는 직접 빈으로 등록한다고 바로 사용할 수 있는게 아니다.
        - 적절한 메서드를 호출해주는 초기화 코드가 필요하다.
        - 초기화 코드가 필요하다면 팩토리 빈으로 만드는 것이 좋다.
        - ```
          public class EmbeddedDBSqlRegistry implements UpdatableSqlRegistry {
          
            JdbcTemplate jdbcTemplate;
          
            public void setDataSource(DataSource dataSource) {
              this.jdbcTemplate = new JdbcTemplate(dataSource);
            }
          
            public void registerSql(String key, String sql) {
              jdbcTemplate.update("insert into sqlmap(key_, sql_) values(?, ?)", key, sql);
            }
          
            public String findSql(String key) throws SqlNotFoundException {
              try {
                return jdbcTemplate.queryForObject("select sql_ from sqlmap where key_ = ?", String.class, key);
              } catch (EmptyResultDataAccessException e) {
                throw new SqlNotFoundException("not found. key : " + key);
              }
            }
          
            public void updateSql(String key, String sql) throws SqlUpdateFailureException {
              int affected = jdbcTemplate.update("update sqlmap set sql_ = ? where key_ = ?", sql, key);
              if (affected == 0) {
                throw new SqlUpdateFailureException("update failed. key : " + key);
              }
            }
          
            public void updateSql(Map<String, String> sqlmap) throws SqlUpdateFailureException {
              sqlmap.forEach((k, v) -> updateSql(k, v));
            }
          }
          ```
    - UpdatableSqlRegistry 테스트 코드의 재사용
        - 테스트코드를 상속구조로 설정
        - 변경된 ConcurrentHashMapSqlRegistryTest
        - <details markdown="1">
          <summary>코드 접기/펼치기</summary>
          <pre>
          public class ConcurrentHashMapSqlRegistryTest extends AbstractUpdatableSqlRegistryTest {          
            protected UpdatableSqlRegistry createUpdatableSqlRegistry() {
              return new ConcurrentHashMapSqlRegistry();
            }          
          }
          </pre>
          </details>
        - EmbeddedDBSqlRegistry 테스트
        - <details markdown="1">
          <summary>코드 접기/펼치기</summary>
          <pre>
          public class EmbeddedDBSqlRegistryTest extends AbstractUpdatableSqlRegistryTest {
            EmbeddedDatabase db;          
            @Override
            protected UpdatableSqlRegistry createUpdatableSqlRegistry() {
              db = new EmbeddedDatabaseBuilder()
                      .setType(EmbeddedDatabaseType.HSQL)
                      .addScript("/embedded-embedded-schema.sql")
                      .build();          
              EmbeddedDBSqlRegistry sqlRegistry = new EmbeddedDBSqlRegistry();
              sqlRegistry.setDataSource(db);          
              return sqlRegistry;
            }          
            @After
            public void tearDown() {
              db.shutdown();
            }          
          }
          </pre>
          </details>
    - XML 설정을 통한 내장형 DB의 생성과 적용
        - xml설정은 계속 안따라해서 팩토리빈으로 등록
        - <details markdown="1">
          <summary>코드 접기/펼치기</summary>
          <pre>
          @Bean
          public SqlService sqlService() {
            OxmSqlService sqlService = new OxmSqlService();
            sqlService.setUnmarshaller(unmarshaller());
            sqlService.setSqlRegistry(sqlRegistry());
            return sqlService;
          }
          @Bean
          public SqlRegistry sqlRegistry() {
            EmbeddedDBSqlRegistry sqlRegistry = new EmbeddedDBSqlRegistry();
            sqlRegistry.setDataSource(embeddedDatabase());
            return sqlRegistry;
          }          
          @Bean(name = "embeddedDatabase")
          public DataSource embeddedDatabase() {
            EmbeddedDatabaseFactoryBean factoryBean = new EmbeddedDatabaseFactoryBean();
            factoryBean.setDatabaseType(EmbeddedDatabaseType.HSQL);
            ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
            Resource resource = new ClassPathResource("/sql/embedded-schema.sql");
            databasePopulator.addScript(resource);
            factoryBean.setDatabasePopulator(databasePopulator);
            factoryBean.afterPropertiesSet();
            return factoryBean.getObject();
          }
          </pre>
          </details>
3. 트랜잭션 적용
    - EmbeddedRegistry를 적용했다.
        - EmbeddedRegistry는 빈번한 조회 중에도 데이터가 깨지는 일 없이 안전하게 SQL을 수정하도록 보장해준다.
        - 여러 개의 SQL을 변경하는 중 존재하지 않는 키가 발견돼서 예외가 발생한다면?
        - 단순 JdbcTemplate을 사용하기 때문에 트랜잭션이 적용되어있지 않다.
        - 내장형 DB는 DB 자체가 기본적으로 트랜잭션 기반의 작업에 충실하게 설계돼있다.
    - 다중 SQL 수정에 대한 트랜잭션 테스트
        - ```
          public class EmbeddedDBSqlRegistryTest extends AbstractUpdatableSqlRegistryTest {
            ...
            @Test
            public void transactionalUpdate() {
              checkFindResult(SQL_1, SQL_2, SQL_3);
              Map<String, String> sqlmap = new HashMap();
              sqlmap.put(KEY_1, "modified1");
              sqlmap.put("unknownKey", "modified2");          
              try {
                updatableSqlRegistry.updateSql(sqlmap);
                fail();
              } catch (SqlUpdateFailureException e) {
                // ignore
              }
              checkFindResult(SQL_1, SQL_2, SQL_3);
            }          
          }
          ```
    - 코드를 이용한 트랜잭션 적용
        - 간단히 TransactionTemplate 활용
        - ```
          public class EmbeddedDBSqlRegistry implements UpdatableSqlRegistry {            
            TransactionTemplate transactionTemplate;          
            public void setDataSource(DataSource dataSource) {
              this.jdbcTemplate = new JdbcTemplate(dataSource);
              this.transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
            }
            ...
            public void updateSql(Map<String, String> sqlmap) throws SqlUpdateFailureException {
              transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                  sqlmap.forEach((k, v) -> updateSql(k, v));
                }
              });
            }
          }
          ```
### 6. 스프링 3.1의 DI
- 자바 언어의 변화와 스프링 : 대표적인 두 가지 변화
    - 스프링은 점차 애노테이션 메타정보 활용을 늘리고 미리 정해진 정책과 관례를 활용하는 방식을 적극 도입하고 있다.
    - 이 절에서는 최신 스타일로 바꾸는 과정을 설명한다.
    1. 애노테이션의 메타정보 활용
        - 컴파일된 클래스파일은 다른 자바 코드에 의해 데이터처럼 취급되기도 한다.
        - 애노테이션은 옵션에 따라 컴파일된 클래스에 존재하거나 애플리케이션이 동작할 때 메모리에 로딩되기도 한다.
        - 애노테이션은 자바 코드가 실행되는 데 직접 참여하지 못한다.
        - 애노테이션 활용이 늘어난 이유? 스프링 구조에 잘 어울렸음(자바 코드 + IoC방식 + 프레임워크의 메타정보)
        - 애노테이션 방식, XML 방식 각각의 장점
        - ```
          애노테이션 장점 : 
           - 작성해야 하는 코드량이 적고 직관적이다.
          XML 장점 : 
           - 내용이 변경되어도 빌드가 필요 없다.
          ```
    2. 정책과 관례를 이용한 프로그래밍
        - 반복되는 부분을 관례화하면 더 많은 내용을 생략할 수 있지만 학습 비용이 늘어날 수 있다.
        - @Transactional 처럼
        
1. 자바 코드를 이용한 빈 설정
    - 테스트 컨텍스트의 변경
        - 최종 목적 : XML을 더 이상 사용하지 않게 하는 것
        - @ContextConfiguration(classes = TestApplicationContext.class)
    - <context:annotation-config /> 제거
    - <bean>의 전환
    - 전용 태그 전환
        - <jdbc-embedded-database/> : embeddedDatabase 빈 등록
        - <tx:annotation-driven/> : 클래스에 @EnableTransactionManagement 어노테이션 추가
    - 테스트 컨텍스트 코드
        - ```
          @Configuration
          @EnableTransactionManagement
          public class TestApplicationContext {
          
            private String dataSourceUrl = "jdbc:h2:tcp://localhost/~/test";
            private String dataSourceDriverClass = "org.h2.Driver";
            private String dbUsername = "sa";
            private String dbPassword = "";
          
            @Autowired
            private SqlService sqlService;
          
            @Bean
            public DataSource dataSource() {
              SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
              try {
                dataSource.setDriverClass((Class<? extends Driver>) Class.forName(dataSourceDriverClass));
              } catch (Exception e) { }
              dataSource.setUrl(dataSourceUrl);
              dataSource.setUsername(dbUsername);
              dataSource.setPassword(dbPassword);
              return dataSource;
            }
          
            @Bean
            public PlatformTransactionManager transactionManager() {
              return new DataSourceTransactionManager(dataSource());
            }
          
            @Bean
            public UserDao userDao() {
              UserDaoJdbc userDaoJdbc = new UserDaoJdbc(dataSource());
              userDaoJdbc.setSqlService(sqlService);
              return userDaoJdbc;
            }
          
            @Bean
            public UserService userService() {
              UserServiceImpl userService = new UserServiceImpl();
              userService.setUserDao(userDao());
              userService.setMailSender(mailSender());
              return userService;
            }
          
            @Bean
            public UserService testUserService() {
              TestUserServiceImpl testUserService = new TestUserServiceImpl();
              testUserService.setUserDao(userDao());
              testUserService.setMailSender(mailSender());
              return testUserService;
            }
          
            @Bean
            public MailSender mailSender() {
              return new DummyMailSender();
            }
          
            @Bean
            public SqlService sqlService() {
              OxmSqlService sqlService = new OxmSqlService();
              sqlService.setUnmarshaller(unmarshaller());
              sqlService.setSqlRegistry(sqlRegistry());
              return sqlService;
            }
          
            @Bean
            public Unmarshaller unmarshaller() {
              Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
              marshaller.setContextPath("toby.service.sql");
              return marshaller;
            }
          
            @Bean
            public SqlRegistry sqlRegistry() {
              EmbeddedDBSqlRegistry sqlRegistry = new EmbeddedDBSqlRegistry();
              sqlRegistry.setDataSource(embeddedDatabase());
              return sqlRegistry;
            }
          
            @Bean
            public DataSource embeddedDatabase() {
              return new EmbeddedDatabaseBuilder()
                      .setName("embeddedDatabase")
                      .setType(EmbeddedDatabaseType.HSQL)
                      .addScript("/embedded-schema.sql")
                      .build();
            }
          
          }
          ```
2. 빈 스캐닝과 자동와이어링
    - @Autowired를 이용한 자동 와이어링
        - 스프링은 @Autowired가 붙은 메서드의 파라미터 타입을 보고 주입 가능한 타입의 빈을 모두 찾아서 한개면 넣어주고 두 개 이상이면 맞는 이름으로 넣어준다.
        - 타입 -> 이름 -> 못찾으면 오류
    - @Component를 이용한 자동 빈 등록
        - @Component가 붙은 클래스는 빈 스캐너를 통해 자동으로 빈으로 등록된다.
        - @ComponentScan 특정 패키지 아래에서만 찾도록 기준이 되는 패키지를 지정
        - ```
          @ComponentScan(basePackages = "toby.common")
          // basePackages : @Component가 붙은 클래스를 스캔할 기준 패키지를 지정할 때 사용한다.
          // 지정한 패키지 아래의 모든 서브패키지에 대해서 검색한다.
          ```
        - 메타 애노테이션을 이용할 수도 있다.
        - ```
          @Component // 메타 애노테이션
          public @interface SnsConnector { ... }
          ```
### 7. 정리
- 

<details markdown="1">
<summary>코드 접기/펼치기</summary>
<pre>
</pre>
</details>