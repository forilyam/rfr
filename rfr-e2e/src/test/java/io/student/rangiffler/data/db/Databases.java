package io.student.rangiffler.data.db;

import com.zaxxer.hikari.HikariDataSource;
import io.student.rangiffler.config.TestConfig;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

public enum Databases {
  AUTH("rangiffler-auth"),
  API("rangiffler-api");

  private final String schema;
  private volatile HikariDataSource dataSource;
  private volatile DataSourceTransactionManager transactionManager;

  Databases(String schema) {
    this.schema = schema;
  }

  public String schema() {
    return schema;
  }

  public synchronized DataSource dataSource() {
    if (dataSource == null) {
      HikariDataSource created = new HikariDataSource();
      created.setJdbcUrl(TestConfig.dbUrl(schema));
      created.setUsername(TestConfig.dbUser());
      created.setPassword(TestConfig.dbPassword());
      created.setMaximumPoolSize(4);
      created.setPoolName("rfr-e2e-" + schema);
      dataSource = created;
    }
    return dataSource;
  }

  public synchronized PlatformTransactionManager transactionManager() {
    if (transactionManager == null) {
      transactionManager = new DataSourceTransactionManager(dataSource());
    }
    return transactionManager;
  }
}
