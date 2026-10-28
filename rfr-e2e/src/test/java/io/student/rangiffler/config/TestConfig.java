package io.student.rangiffler.config;

import java.util.Locale;

public final class TestConfig {
  private TestConfig() {
  }

  public static String frontUrl() {
    return value("frontUrl", "http://localhost:3001");
  }

  public static String authUrl() {
    return value("authUrl", "http://localhost:9001");
  }

  public static String apiUrl() {
    return value("apiUrl", "http://localhost:8081");
  }

  public static String dbUrl(String schema) {
    return value("dbUrl", "jdbc:mysql://localhost:3306") + "/" + schema + "?serverTimezone=UTC";
  }

  public static String dbUser() {
    return value("dbUser", "root");
  }

  public static String dbPassword() {
    return value("dbPassword", "secret");
  }

  public static String browser() {
    return value("browser", "chromium");
  }

  public static boolean headless() {
    return Boolean.parseBoolean(value("headless", "true"));
  }

  static String value(String key, String fallback) {
    String property = System.getProperty("rfr." + key);
    if (property != null && !property.isBlank()) {
      return property;
    }
    String variable = System.getenv("RFR_" + key.replaceAll("([A-Z])", "_$1").toUpperCase(Locale.ROOT));
    if (variable != null && !variable.isBlank()) {
      return variable;
    }
    return fallback;
  }
}
