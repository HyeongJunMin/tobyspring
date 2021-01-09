package dao;

import domain.User;

import javax.xml.transform.Source;
import java.sql.*;

public class UserDao {

  public void add(User user) throws ClassNotFoundException, SQLException {
    Class.forName("org.h2.Driver");
    Connection c = DriverManager.getConnection("jdbc:h2:tcp://localhost/~/test", "sa", "");
    PreparedStatement ps = c.prepareStatement("insert into users(id, name, password) values (?, ?, ?)");
    ps.setString(1, user.getId());
    ps.setString(2, user.getName());
    ps.setString(3, user.getPassword());
    ps.executeUpdate();
    ps.close();
    c.close();
  }

  public User get(String id) throws ClassNotFoundException, SQLException {
    Class.forName("org.h2.Driver");
    Connection c = DriverManager.getConnection("jdbc:h2:tcp://localhost/~/test", "sa", "");

    PreparedStatement ps = c.prepareStatement("select * from users where id = ?");
    ps.setString(1, id);
    ResultSet rs = ps.executeQuery();
    rs.next();

    User user = new User();
    user.setId(rs.getString("id"));
    user.setName(rs.getString("name"));
    user.setPassword(rs.getString("password"));
    rs.close();
    ps.close();
    c.close();
    return user;
  }

  public static void main(String[] args) throws SQLException, ClassNotFoundException {
    System.out.println("start");
    UserDao dao = new UserDao();
    User user = new User();
    user.setId("whiteship");
    user.setName("백기선");
    user.setPassword("married");
    dao.add(user);
    System.out.println("done!");
  }

}

