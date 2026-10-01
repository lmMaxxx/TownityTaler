package de.townity.taler.common.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.townity.taler.common.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.logging.Logger;

public final class TalerDatabase implements AutoCloseable {

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS tt_balances (
              uuid CHAR(36) NOT NULL,
              name VARCHAR(16) NOT NULL,
              balance BIGINT NOT NULL DEFAULT 0,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (uuid),
              KEY idx_tt_balances_name (name)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private final HikariDataSource dataSource;
    private final Logger logger;

    public TalerDatabase(DatabaseConfig config, Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
        HikariConfig hikari = new HikariConfig();
        // Explizit setzen – nach Shade-Relocation findet DriverManager den Treiber sonst nicht
        hikari.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getName());
        hikari.setJdbcUrl(config.jdbcUrl());
        hikari.setUsername(config.username());
        hikari.setPassword(config.password());
        hikari.setMaximumPoolSize(Math.max(2, config.poolSize()));
        hikari.setPoolName("TownityTaler");
        hikari.setMinimumIdle(1);
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        this.dataSource = new HikariDataSource(hikari);
    }

    public void migrate() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE);
            // Bestehende Installationen: Nachkommastellen entfernen
            try {
                statement.execute("ALTER TABLE tt_balances MODIFY balance BIGINT NOT NULL DEFAULT 0");
            } catch (SQLException ignored) {
                // Spalte ggf. schon BIGINT
            }
        }
        logger.info("Taler-Schema bereit (tt_balances, ganzzahlig).");
    }

    public Connection connection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
