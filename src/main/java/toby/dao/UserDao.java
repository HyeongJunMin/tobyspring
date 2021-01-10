package toby.dao;

import toby.domain.User;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDao {

  private DataSource dataSource;
  private JdbcContext jdbcContext;

  public UserDao(DataSource dataSource) {
    this.dataSource = dataSource;
    this.jdbcContext = new JdbcContext(dataSource);
  }

//  public UserDao(DataSource dataSource, JdbcContext jdbcContext) {
//    this.dataSource = dataSource;
//    this.jdbcContext = jdbcContext;
//  }

  public void add(User user) throws ClassNotFoundException, SQLException {
    this.jdbcContext.workWithStatementStrategy(c -> {
              PreparedStatement ps = c.prepareStatement("insert into users(id, name, password) values (?, ?, ?)");
              ps.setString(1, user.getId());
              ps.setString(2, user.getName());
              ps.setString(3, user.getPassword());
              return ps;
    });
//    jdbcContextWithStatementStrategy(c -> {
//      PreparedStatement ps = c.prepareStatement("insert into users(id, name, password) values (?, ?, ?)");
//      ps.setString(1, user.getId());
//      ps.setString(2, user.getName());
//      ps.setString(3, user.getPassword());
//      return ps;
//    });
//    전략패턴 적용 전
//    try (Connection c = dataSource.getConnection();
//         PreparedStatement ps = c.prepareStatement("insert into users(id, name, password) values (?, ?, ?)");) {
//      ps.setString(1, user.getId());
//      ps.setString(2, user.getName());
//      ps.setString(3, user.getPassword());
//      ps.executeUpdate();
//    } catch (Exception e) {
//    }
  }

  public User get(String id) throws ClassNotFoundException, SQLException {
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
    this.jdbcContext.executeSql("delete from users"); // 246p
//    this.jdbcContext.workWithStatementStrategy(c -> c.prepareStatement("delete from users"));
//    jdbcContextWithStatementStrategy(new StatementStrategy() {
//      @Override
//      public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
//        return c.prepareStatement("delete from users");
//      }
//    });
    // 익명 내부클래스로 처리
//    jdbcContextWithStatementStrategy(c -> c.prepareStatement("delete from users"));
  }

  public int getCount() {
    int count = 0;
    try (Connection connection = dataSource.getConnection();
         PreparedStatement ps = connection.prepareStatement("select * from user");) {
      ResultSet rs = ps.executeQuery();
      count = rs.getInt(0);
      rs.close();
    } catch (Exception e) {
    }
    return count;
  }

  // StatementStrategy strategy : 클라이언트가 컨텍스트를 호출할 때 넘겨줄 전략 파라미터
  // JdbcContext로 독립시킴(232p)
//  private void jdbcContextWithStatementStrategy(StatementStrategy strategy) throws SQLException {
//    try (Connection c = dataSource.getConnection();
//          PreparedStatement ps = strategy.makePreparedStatement(c)) {
//      ps.executeUpdate();
//    } catch (SQLException e) {
//      throw e;
//    }
//  }

}

