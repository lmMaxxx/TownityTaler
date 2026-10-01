package de.townity.taler.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.townity.taler.common.TalerChannels;
import de.townity.taler.common.config.DatabaseConfig;
import de.townity.taler.common.db.PendingNotifyRepository;
import de.townity.taler.common.db.TalerDatabase;
import de.townity.taler.common.db.TalerRepository;
import de.townity.taler.common.resolve.MojangUuidResolver;
import de.townity.taler.common.service.DefaultTalerService;
import de.townity.taler.velocity.command.BoughtTalerCommand;
import de.townity.taler.velocity.command.VeloTalerCommand;
import de.townity.taler.velocity.config.VelocityConfig;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Plugin(
        id = "townity-taler",
        name = "TownityTaler",
        version = "1.0.0-SNAPSHOT",
        description = "Netzwerkweite Taler-Währung",
        authors = {"Townity"}
)
public final class TalerVelocityPlugin {

    public static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.from(TalerChannels.CHANNEL);

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    private TalerDatabase database;
    private DefaultTalerService service;
    private PendingNotifyRepository pendingNotify;
    private MojangUuidResolver uuidResolver;
    private ExecutorService executor;

    @Inject
    public TalerVelocityPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        try {
            VelocityConfig config = VelocityConfig.load(dataDirectory, logger);
            DatabaseConfig dbConfig = config.database();
            java.util.logging.Logger jdkLogger = java.util.logging.Logger.getLogger("TownityTaler");
            database = new TalerDatabase(dbConfig, jdkLogger);
            database.migrate();
            pendingNotify = new PendingNotifyRepository(database);
            pendingNotify.migrate();
            executor = Executors.newFixedThreadPool(4, r -> {
                Thread thread = new Thread(r, "TownityTaler-DB");
                thread.setDaemon(true);
                return thread;
            });
            TalerRepository repository = new TalerRepository(database);
            service = new DefaultTalerService(repository, executor, jdkLogger);
            uuidResolver = new MojangUuidResolver();

            proxy.getChannelRegistrar().register(CHANNEL);
            proxy.getCommandManager().register(
                    proxy.getCommandManager().metaBuilder("velotaler")
                            .plugin(this)
                            .build(),
                    new VeloTalerCommand(this)
            );
            proxy.getCommandManager().register(
                    proxy.getCommandManager().metaBuilder("boughttaler")
                            .plugin(this)
                            .build(),
                    new BoughtTalerCommand(this)
            );
            logger.info("TownityTaler (Velocity) aktiv.");
        } catch (Exception exception) {
            logger.error("TownityTaler konnte nicht starten", exception);
            throw new IllegalStateException(exception);
        }
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (executor != null) {
            executor.shutdownNow();
        }
        if (database != null) {
            database.close();
        }
    }

    public ProxyServer proxy() {
        return proxy;
    }

    public Logger logger() {
        return logger;
    }

    public DefaultTalerService service() {
        return service;
    }

    public PendingNotifyRepository pendingNotify() {
        return pendingNotify;
    }

    public MojangUuidResolver uuidResolver() {
        return uuidResolver;
    }

    public ExecutorService executor() {
        return executor;
    }
}
