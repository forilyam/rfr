package io.student.rangiffler.data;

import io.student.rangiffler.utils.RandomDataUtils;

public record TestUser(String username, String password) {
  public static TestUser unique() {
    return new TestUser(RandomDataUtils.randomUsername(), RandomDataUtils.randomPassword());
  }

  @Override
  public String toString() {
    return "TestUser[username=" + username + ", password=<redacted>]";
  }
}
