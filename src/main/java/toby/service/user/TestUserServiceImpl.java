package toby.service.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import toby.common.exception.TestUserServiceException;
import toby.domain.User;

import java.util.List;

@Service("testUserService")
public class TestUserServiceImpl extends UserServiceImpl {
  private String id = "0004";

  protected void upgradeLevel(User user) {
    if (user.getId().equals(this.id)) {
      throw new TestUserServiceException();
    }
    super.upgradeLevel(user);
  }

  @Transactional(readOnly = true)
  public List<User> getAll() {
    for (User user : super.getAll()) {
      user.setName("OMG");
      super.update(user);
    }
    return null;
  }
}
