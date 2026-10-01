package de.townity.taler.common.db;

import de.townity.taler.common.TalerAmounts;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Offline-Kauf-Benachrichtigungen bis der Spieler wieder online ist. */
public final class PendingNotifyRepository {

    private final TalerDatabase database;

    public PendingNotifyRepository(TalerDatabase database) {
        this.database = database;
    }

    public void migrate() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS tt_pending_notify (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  uuid CHAR(36) NOT NULL,
                  amount DECIMAL(18,2) NOT NULL,
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  KEY idx_tt_pending_uuid (uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;
        try (Connection connection = database.connection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public void enqueue(UUID uuid, BigDecimal amount) throws SQLException {
        String sql = "INSERT INTO tt_pending_notify (uuid, amount) VALUES (?, ?)";
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setBigDecimal(2, TalerAmounts.normalize(amount));
            statement.executeUpdate();
        }
    }

    public List<PendingNotify> drain(UUID uuid) throws SQLException {
        List<PendingNotify> result = new ArrayList<>();
        try (Connection connection = database.connection()) {
            connection.setAutoCommit(false);
            try {
                String select = "SELECT id, amount FROM tt_pending_notify WHERE uuid = ? ORDER BY id ASC FOR UPDATE";
                try (PreparedStatement statement = connection.prepareStatement(select)) {
                    statement.setString(1, uuid.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            result.add(new PendingNotify(
                                    resultSet.getLong("id"),
                                    TalerAmounts.normalize(resultSet.getBigDecimal("amount"))
                            ));
                        }
                    }
                }
                if (!result.isEmpty()) {
                    String delete = "DELETE FROM tt_pending_notify WHERE uuid = ?";
                    try (PreparedStatement statement = connection.prepareStatement(delete)) {
                        statement.setString(1, uuid.toString());
                        statement.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
        return result;
    }

    public record PendingNotify(long id, BigDecimal amount) {
    }
}
