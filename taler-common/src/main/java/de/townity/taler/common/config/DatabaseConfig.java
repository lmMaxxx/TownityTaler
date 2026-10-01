package de.townity.taler.common.config;

/** Verbindungsdaten für die eigene Taler-MySQL-Datenbank. */
public record DatabaseConfig(
        String host,
        int port,
        String database,
        String username,
        String password,
        int poolSize
) {
    public String jdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8"
                + "&serverTimezone=UTC";
    }
}
