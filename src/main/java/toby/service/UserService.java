package toby.service;

import toby.domain.User;

public interface UserService {
  void add(User user);
  void upgradeLevels();
  void createOrIncreaseRecommend(User user);
}
