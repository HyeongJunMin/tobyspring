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
      if (canUpgradeLevel(user)) {
        upgradeLevel(user);
      }
    });
  }

  private boolean canUpgradeLevel(User user) {
    Level currentLevel = user.getLevel();
    switch (currentLevel) {
      case GOLD: return false;
      case BASIC: return (user.getLogin() >= LOGIN_COUNT_FOR_SILVER);
      case SILVER: return (user.getRecommend() >= RECOMMEND_COUNT_FOR_GOLD);
      default: throw new IllegalArgumentException("Unknown level : " + currentLevel);
    }
  }

  public void upgradeLevel(User user) {
    user.upgradeLevel();
    userDao.update(user);
  }

  public void add(User user) {
    if (user.getLevel() == null) {
      user.setLevel(Level.BASIC);
    }
    userDao.add(user);
  }

}
