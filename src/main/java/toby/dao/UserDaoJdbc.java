package toby.dao;

import lombok.Setter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;
import toby.common.exception.DuplicateUserIdException;
import toby.domain.Level;
import toby.domain.User;
import toby.service.sql.SqlService;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

@Setter
public class UserDaoJdbc implements UserDao {

  private JdbcTemplate jdbcTemplate;
  private SqlService sqlService;

  public UserDaoJdbc(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  public void add(final User user) {
    this.jdbcTemplate.update(sqlService.getSql(SqlService.USER_ADD)
            , user.getId(), user.getName(), user.getPassword(), user.getLevel().intValue(), user.getLogin()
            , user.getRecommend(), user.getEmail());
  }

  @Transactional
  public void addAll(List<User> userList) {
    userList.forEach(user -> add(user));
  }

  public void addWithDuplicateUserIdException(final User user) throws DuplicateUserIdException {
    try {
      this.jdbcTemplate.update("insert into users(id, name, password, level, login, recommend, email) values(?, ?, ?, ?, ?, ?, ?)"
              , user.getId(), user.getName(), user.getPassword(), user.getLevel().intValue(), user.getLogin(), user.getRecommend(), user.getEmail());
      throw new SQLException("test", "test", DuplicateUserIdException.ERROR_DUPLICATED_ENTRY);
    } catch (SQLException e) {
      if (e.getErrorCode() == DuplicateUserIdException.ERROR_DUPLICATED_ENTRY) {
        throw new DuplicateUserIdException();
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  public User get(String id) {
    return jdbcTemplate.queryForObject(sqlService.getSql(SqlService.USER_GET),
            // SQL에 바인딩 할 파라미터 값. 가변인자 대신 배열 사용
            new Object[] { id },
            // ResultSet 한 로우의 결과를 오브젝트에 매핑해주는 RowMapper콜백
            getUserMapper());
  }
  private RowMapper<User> getUserMapper() {
    return (resultSet, i) -> {
      User user = new User();
      user.setId(resultSet.getString("id"));
      user.setName(resultSet.getString("name"));
      user.setPassword(resultSet.getString("password"));
      user.setLevel(Level.valueOf(resultSet.getInt("level")));
      user.setLogin(resultSet.getInt("login"));
      user.setRecommend(resultSet.getInt("recommend"));
      user.setEmail(resultSet.getString("email"));
      return user;
    };
  }

  public User getUserByName(String name) {
    return jdbcTemplate.queryForObject("select * from users where name = ?", new Object[] { name }, getUserMapper());
  }

  public void deleteAll() {
    this.jdbcTemplate.update(sqlService.getSql(SqlService.USER_DELETE_ALL));
  }

  public int getCount() {
    return jdbcTemplate.queryForObject(sqlService.getSql(SqlService.USER_GET_COUNT), Integer.class);
  }

  public List<User> getAll() {
    return jdbcTemplate.query(sqlService.getSql(SqlService.USER_GET_ALL), getUserMapper());
  }

  public void update(User user) {
    this.jdbcTemplate.update(sqlService.getSql(SqlService.USER_UPDATE)
        , user.getName(), user.getPassword(), user.getLevel().intValue(), user.getLogin(), user.getRecommend(), user.getEmail(), user.getId());
  }

}

