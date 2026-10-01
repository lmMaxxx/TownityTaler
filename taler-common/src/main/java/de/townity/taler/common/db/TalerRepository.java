package de.townity.taler.common.db;

import de.townity.taler.common.TalerAmounts;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistenz für Netzwerk-Taler. Alle Methoden sind Blocking/SQL und müssen
 * off-thread aufgerufen werden.
 */
public final class TalerRepository {

    private final TalerDatabase database;

    public TalerRepository(TalerDatabase database) {
        this.database = database;
    }

    public BigDecimal getBalance(UUID uuid) throws SQLException {
        String sql = "SELECT balance FROM tt_balances WHERE uuid = ?";
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return TalerAmounts.normalize(resultSet.getBigDecimal("balance"));
                }
                return TalerAmounts.zero();
            }
        }
    }

    public BigDecimal add(UUID uuid, String name, BigDecimal amount) throws SQLException {
        BigDecimal delta = TalerAmounts.normalize(amount);
        if (delta.signum() < 0) {
            throw new IllegalArgumentException("Betrag muss ≥ 0 sein");
        }
        ensureRow(uuid, name);
        String sql = "UPDATE tt_balances SET balance = balance + ?, name = ? WHERE uuid = ?";
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, delta);
            statement.setString(2, sanitizeName(name));
            statement.setString(3, uuid.toString());
            statement.executeUpdate();
        }
        return getBalance(uuid);
    }

    /**
     * @return empty wenn nicht genug Guthaben
     */
    public Optional<BigDecimal> remove(UUID uuid, BigDecimal amount) throws SQLException {
        BigDecimal delta = TalerAmounts.normalize(amount);
        if (delta.signum() < 0) {
            throw new IllegalArgumentException("Betrag muss ≥ 0 sein");
        }
        if (delta.signum() == 0) {
            return Optional.of(getBalance(uuid));
        }
        String sql = """
                UPDATE tt_balances
                SET balance = balance - ?
                WHERE uuid = ? AND balance >= ?
                """;
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, delta);
            statement.setString(2, uuid.toString());
            statement.setBigDecimal(3, delta);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                return Optional.empty();
            }
        }
        return Optional.of(getBalance(uuid));
    }

    public BigDecimal set(UUID uuid, String name, BigDecimal amount) throws SQLException {
        BigDecimal value = TalerAmounts.normalize(amount);
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Betrag muss ≥ 0 sein");
        }
        String sql = """
                INSERT INTO tt_balances (uuid, name, balance)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE name = VALUES(name), balance = VALUES(balance)
                """;
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, sanitizeName(name));
            statement.setBigDecimal(3, value);
            statement.executeUpdate();
        }
        return value;
    }

    public boolean has(UUID uuid, BigDecimal amount) throws SQLException {
        return getBalance(uuid).compareTo(TalerAmounts.normalize(amount)) >= 0;
    }

    public Optional<PlayerLookup> findByName(String name) throws SQLException {
        String sql = "SELECT uuid, name, balance FROM tt_balances WHERE name = ? LIMIT 1";
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sanitizeName(name));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new PlayerLookup(
                        UUID.fromString(resultSet.getString("uuid")),
                        resultSet.getString("name"),
                        TalerAmounts.normalize(resultSet.getBigDecimal("balance"))
                ));
            }
        }
    }

    private void ensureRow(UUID uuid, String name) throws SQLException {
        String sql = """
                INSERT INTO tt_balances (uuid, name, balance)
                VALUES (?, ?, 0.00)
                ON DUPLICATE KEY UPDATE name = VALUES(name)
                """;
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, sanitizeName(name));
            statement.executeUpdate();
        }
    }

    private static String sanitizeName(String name) {
        if (name == null || name.isBlank()) {
            return "unknown";
        }
        String trimmed = name.trim();
        return trimmed.length() > 16 ? trimmed.substring(0, 16) : trimmed;
    }

    public record PlayerLookup(UUID uuid, String name, BigDecimal balance) {
    }
}
