package domain.blog;

import java.io.Serializable;

public interface Author extends Serializable {
  int getId();

  String getUsername();

  String getPassword();

  String getEmail();

  String getBio();
}
