package toby.service;

import org.springframework.stereotype.Service;
import toby.common.exception.TestUserServiceException;
import toby.domain.User;

@Service("testUserServiceImpl")
public class TestUserServiceImpl extends UserServiceImpl {
  private String id = "0004";
  protected void upgradeLevel(User user) {
    if (user.getId().equals(this.id)) { throw new TestUserServiceException(); }
    super.upgradeLevel(user);
  }
}
