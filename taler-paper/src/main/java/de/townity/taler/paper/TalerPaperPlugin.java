package de.townity.taler.paper;

import de.townity.taler.api.TalerAPI;
import de.townity.taler.api.TalerProvider;
import de.townity.taler.common.TalerChannels;
import de.townity.taler.common.config.DatabaseConfig;
import de.townity.taler.common.db.PendingNotifyRepository;
import de.townity.taler.common.db.TalerDatabase;
import de.townity.taler.common.db.TalerRepository;
import de.townity.taler.common.service.DefaultTalerService;
import de.townity.taler.paper.listener.PurchaseNotifyListener;
import de.townity.taler.paper.notify.PurchaseNotifier;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class TalerPaperPlugin extends JavaPlugin {

    private TalerDatabase database;
    private DefaultTalerService service;
    private PendingNotifyRepository pendingNotify;
    private ExecutorService executor;
    private PurchaseNotifier purchaseNotifier;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        DatabaseConfig dbConfig = readDatabaseConfig();
        try {
            database = new TalerDatabase(dbConfig, getLogger());
            database.migrate();
            pendingNotify = new PendingNotifyRepository(database);
            pendingNotify.migrate();
            executor = Executors.newFixedThreadPool(4, r -> {
                Thread thread = new Thread(r, "TownityTaler-DB");
                thread.setDaemon(true);
                return thread;
            });
            TalerRepository repository = new TalerRepository(database);
            service = new DefaultTalerService(repository, executor, getLogger());
            purchaseNotifier = new PurchaseNotifier(this);

            TalerProvider.register(service);
            getServer().getServicesManager().register(TalerAPI.class, service, this, ServicePriority.Normal);

            getServer().getMessenger().registerIncomingPluginChannel(
                    this,
                    TalerChannels.CHANNEL,
                    new PurchaseNotifyListener(this)
            );
            getServer().getPluginManager().registerEvents(
                    new de.townity.taler.paper.listener.JoinPendingNotifyListener(this),
                    this
            );
            getLogger().info("TalerAPI (Paper) aktiv – Service registriert.");
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "TalerAPI konnte nicht starten", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (service != null) {
            TalerProvider.unregister(service);
            getServer().getServicesManager().unregister(TalerAPI.class, service);
        }
        getServer().getMessenger().unregisterIncomingPluginChannel(this, TalerChannels.CHANNEL);
        if (executor != null) {
            executor.shutdownNow();
        }
        if (database != null) {
            database.close();
        }
    }

    public DefaultTalerService service() {
        return service;
    }

    public PendingNotifyRepository pendingNotify() {
        return pendingNotify;
    }

    public PurchaseNotifier purchaseNotifier() {
        return purchaseNotifier;
    }

    public ExecutorService executor() {
        return executor;
    }

    private DatabaseConfig readDatabaseConfig() {
        return new DatabaseConfig(
                getConfig().getString("database.host", "127.0.0.1"),
                getConfig().getInt("database.port", 3306),
                getConfig().getString("database.name", "townity_taler"),
                getConfig().getString("database.user", "taler"),
                getConfig().getString("database.password", ""),
                getConfig().getInt("database.pool-size", 4)
        );
    }
}
