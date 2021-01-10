package toby.dao;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JdbcContext {

  private DataSource dataSource;

  public JdbcContext(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public void executeSql(final String query) throws SQLException {
    workWithStatementStrategy(c -> c.prepareStatement(query));
//    workWithStatementStrategy(new StatementStrategy() {
//      @Override
//      public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
//        return c.prepareStatement(query);
//      }
//    });
  }

  // UserDao에 있던 jdbcContextWithStatementStrategy를 이쪽으로 가져왔음(232p)
  public void workWithStatementStrategy(StatementStrategy strategy) throws SQLException {
    try (Connection c = dataSource.getConnection();
         PreparedStatement ps = strategy.makePreparedStatement(c)) {
      ps.executeUpdate();
    } catch (SQLException e) {
      throw e;
    }
  }
}
