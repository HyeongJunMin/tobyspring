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
}
