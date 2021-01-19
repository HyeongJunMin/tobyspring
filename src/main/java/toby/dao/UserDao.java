package toby.dao;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import toby.domain.User;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDao {

  private DataSource dataSource;
  private JdbcContext jdbcContext;

  private JdbcTemplate jdbcTemplate;

  public UserDao(DataSource dataSource) {
    this.dataSource = dataSource;
    this.jdbcContext = new JdbcContext(dataSource);
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  // 책 예제코드
//  public void setDataSource(DataSource dataSource) {
//    this.jdbcTemplate = new JdbcTemplate(dataSource);
//    this.dataSource = dataSource;
//  }

  public void add(User user) throws ClassNotFoundException, SQLException {
    this.jdbcTemplate.update("insert into users(id, name, password) values(?, ?, ?)"
        , user.getId(), user.getName(), user.getPassword());
  }

  public User get(String id) throws SQLException {
    User user = new User();
    try (Connection c = dataSource.getConnection();
         PreparedStatement ps = c.prepareStatement("select * from users where id = ?");) {
      ps.setString(1, id);
      ResultSet rs = ps.executeQuery();
      rs.next();
      user.setId(rs.getString("id"));
      user.setName(rs.getString("name"));
      user.setPassword(rs.getString("password"));
    } catch (Exception e) {
    }
    return user;
  }

  public void deleteAll() throws SQLException {
//    this.jdbcContext.executeSql("delete from users");
//    this.jdbcTemplate.update(connection -> connection.prepareStatement("delete from users"));
    this.jdbcTemplate.update("delete from users");
  }

  public int getCount() {
    return this.jdbcTemplate.query(new PreparedStatementCreator() {
      @Override
      public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        return null;
      }
    }, new ResultSetExtractor<Integer>() {
      @Override
      public Integer extractData(ResultSet resultSet) throws SQLException, DataAccessException {
        return null;
      }
    });
  }

}

