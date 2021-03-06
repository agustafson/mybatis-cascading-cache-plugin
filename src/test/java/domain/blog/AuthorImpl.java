/*
 *    Copyright 2009-2012 The MyBatis Team
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package domain.blog;

class AuthorImpl implements Author {

  private final int id;
  private final String username;
  private final String password;
  private final String email;
  private final String bio;

  public AuthorImpl(int id, String username, String password, String email, String bio) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.email = email;
    this.bio = bio;
  }

  public int getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getEmail() {
    return email;
  }

  public String getBio() {
    return bio;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AuthorImpl)) return false;

    AuthorImpl author = (AuthorImpl) o;

    if (id != author.id) return false;
    if (bio != null ? !bio.equals(author.bio) : author.bio != null) return false;
    if (email != null ? !email.equals(author.email) : author.email != null) return false;
    if (password != null ? !password.equals(author.password) : author.password != null) return false;
    if (username != null ? !username.equals(author.username) : author.username != null) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = id;
    result = 31 * result + (username != null ? username.hashCode() : 0);
    result = 31 * result + (password != null ? password.hashCode() : 0);
    result = 31 * result + (email != null ? email.hashCode() : 0);
    result = 31 * result + (bio != null ? bio.hashCode() : 0);
    return result;
  }

  public String toString() {
    return "Author : " + id + " : " + username + " : " + email;
  }
}