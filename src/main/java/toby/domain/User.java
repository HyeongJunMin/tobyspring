package toby.domain;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class User {
  private String id;
  private String name;
  private String password;
  private Level level;
  private int login;
  private int recommend;
  public void upgradeLevel() {
    Level nextLevel = this.level.getNextLevel();
    if (nextLevel == null) {
      throw new IllegalStateException(this.level + "은 업그레이드가 불가합니다.");
    }
    this.level = nextLevel;
  }
}
