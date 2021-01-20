package toby.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import toby.domain.User;

import javax.sql.DataSource;
import java.util.List;

public class UserDao {

  private JdbcTemplate jdbcTemplate;

  public UserDao(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  public void add(final User user) {
    this.jdbcTemplate.update("insert into users(id, name, password) values(?, ?, ?)"
        , user.getId(), user.getName(), user.getPassword());
  }

  public User get(String id) {
    return jdbcTemplate.queryForObject("select * from users where id = ?",
            // SQL에 바인딩 할 파라미터 값. 가변인자 대신 배열 사용
            new Object[] { id },
            // ResultSet 한 로우의 결과를 오브젝트에 매핑해주는 RowMapper콜백
            getUserMapper());
//    User user = new User();
//    try (Connection c = dataSource.getConnection();
//         PreparedStatement ps = c.prepareStatement("select * from users where id = ?");) {
//      ps.setString(1, id);
//      ResultSet rs = ps.executeQuery();
//      rs.next();
//      user.setId(rs.getString("id"));
//      user.setName(rs.getString("name"));
//      user.setPassword(rs.getString("password"));
//    } catch (Exception e) {
//    }
//    return user;
  }
  private RowMapper<User> getUserMapper() {
    return (resultSet, i) -> {
      User user = new User();
      user.setId(resultSet.getString("id"));
      user.setName(resultSet.getString("name"));
      user.setPassword(resultSet.getString("password"));
      return user;
    };
  }

  public User getUserByName(String name) {
    return jdbcTemplate.queryForObject("select * from users where name = ?", new Object[] { name }, getUserMapper());
  }

  public void deleteAll() {
//    this.jdbcContext.executeSql("delete from users");
//    this.jdbcTemplate.update(connection -> connection.prepareStatement("delete from users"));
    this.jdbcTemplate.update("delete from users");
  }

  public int getCount() {
    return jdbcTemplate.queryForObject("select count(*) from users", Integer.class);
//    return this.jdbcTemplate.query(new PreparedStatementCreator() {
//      @Override
//      public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
//        return connection.prepareStatement("select count(*) from users");
//      }
//    }, new ResultSetExtractor<Integer>() {
//      @Override
//      public Integer extractData(ResultSet resultSet) throws SQLException, DataAccessException {
//        resultSet.next();
//        return resultSet.getInt(1);
//      }
//    });
  }

  public List<User> getAll() {
    return jdbcTemplate.query("select * from users order by id", getUserMapper());
//    return jdbcTemplate.query("select * from users order by id",
//            new RowMapper<User>() {
//              public User mapRow(ResultSet rs, int rowNum) throws SQLException {
//                User user = new User();
//                user.setId(rs.getString("id"));
//                user.setName(rs.getString("name"));
//                user.setPassword(rs.getString("password"));
//                return user;
//              }
//            });
  }

}

