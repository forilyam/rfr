package io.student.rangiffler.data.db;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

public final class Jdbc {
  private Jdbc() {
  }

  public static JdbcClient client(Databases database) {
    return JdbcClient.create(database.dataSource());
  }

  public static void inTransaction(Databases database, Runnable action) {
    new TransactionTemplate(database.transactionManager()).executeWithoutResult(status -> action.run());
  }

  public static String sql(String path) {
    try {
      return new ClassPathResource("sql/" + path).getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException error) {
      throw new UncheckedIOException("Cannot read SQL resource sql/" + path, error);
    }
  }
}
