package toby.domain;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class UserTest {

  User user;

  @Before
  public void setUp() {
    user = new User();
  }

  @Test
  public void upgradeLevel() {
    Level[] levels = Level.values();
    for(Level level : levels) {
      if (level.getNextLevel() == null) {
        continue;
      }
      user.setLevel(level);
      user.upgradeLevel();
      assertThat(user.getLevel()).isEqualTo(level.getNextLevel());
    }
  }

  @Test(expected = IllegalStateException.class)
  public void cannotUpgradeLevel() {
    Level[] levels = Level.values();
    for(Level level : levels) {
      if (level.getNextLevel() != null) {
        continue;
      }
      user.setLevel(level);
      user.upgradeLevel();
    }
  }
}