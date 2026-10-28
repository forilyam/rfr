package io.student.rangiffler.utils;

import java.util.UUID;

public final class RandomDataUtils {
  public static final String PREFIX = "rfr_";

  private RandomDataUtils() {
  }

  public static String randomUsername() {
    return PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
  }

  public static String randomPassword() {
    return "Aa1" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
  }
}
