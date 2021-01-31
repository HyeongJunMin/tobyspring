package toby.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import toby.dao.UserDao;
import toby.domain.Level;
import toby.domain.User;

import java.util.List;

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

}
