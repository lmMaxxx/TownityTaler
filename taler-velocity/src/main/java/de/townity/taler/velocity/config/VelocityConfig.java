package de.townity.taler.velocity.config;

import de.townity.taler.common.config.DatabaseConfig;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VelocityConfig {

    private final DatabaseConfig database;

    private VelocityConfig(DatabaseConfig database) {
        this.database = database;
    }

    public DatabaseConfig database() {
        return database;
    }

    public static VelocityConfig load(Path dataDirectory, Logger logger) throws IOException {
        // Fester Ordner plugins/TalerAPI (nicht plugins/talerapi)
        Path configDir = resolveTalerApiFolder(dataDirectory);
        Files.createDirectories(configDir);
        Path file = configDir.resolve("config.yml");
        if (!Files.exists(file)) {
            try (InputStream in = VelocityConfig.class.getClassLoader().getResourceAsStream("config.yml")) {
                if (in == null) {
                    throw new IOException("Eingebettete config.yml fehlt");
                }
                Files.copy(in, file);
                logger.info("config.yml erstellt unter {}", file);
            }
        }
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(file)
                .build();
        ConfigurationNode root = loader.load();
        ConfigurationNode db = root.node("database");
        DatabaseConfig config = new DatabaseConfig(
                db.node("host").getString("127.0.0.1"),
                db.node("port").getInt(3306),
                db.node("name").getString("townity_taler"),
                db.node("user").getString("taler"),
                db.node("password").getString(""),
                db.node("pool-size").getInt(4)
        );
        return new VelocityConfig(config);
    }

    /** Preferiert {@code plugins/TalerAPI} neben dem Velocity-DataDirectory. */
    private static Path resolveTalerApiFolder(Path dataDirectory) {
        Path parent = dataDirectory.getParent();
        if (parent != null) {
            return parent.resolve("TalerAPI");
        }
        return dataDirectory;
    }
}
