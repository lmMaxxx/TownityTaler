package de.townity.taler.velocity.notify;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;
import de.townity.taler.common.msg.TalerMessages;
import de.townity.taler.velocity.TalerVelocityPlugin;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Sammelt Kauf-Notifies pro Spieler: 10 s nach dem ersten {@code /boughttaler}
 * wird einmalig die Summe angezeigt (z. B. 2×100 → „200 Taler“).
 */
public final class PurchaseNotifyBatcher {

    private static final long WINDOW_SECONDS = 10L;

    private final TalerVelocityPlugin plugin;
    private final Map<UUID, Batch> batches = new ConcurrentHashMap<>();

    public PurchaseNotifyBatcher(TalerVelocityPlugin plugin) {
        this.plugin = plugin;
    }

    /** Gutschrift ist bereits erfolgt – hier nur die Spieler-Benachrichtigung sammeln. */
    public void enqueue(UUID uuid, String name, BigDecimal amount) {
        synchronized (lock(uuid)) {
            Batch existing = batches.get(uuid);
            if (existing == null) {
                Batch batch = new Batch(uuid, name, amount);
                batches.put(uuid, batch);
                batch.task.set(plugin.proxy().getScheduler()
                        .buildTask(plugin, () -> flush(uuid))
                        .delay(WINDOW_SECONDS, TimeUnit.SECONDS)
                        .schedule());
            } else {
                existing.add(amount);
                if (name != null && !name.isBlank()) {
                    existing.name = name;
                }
            }
        }
    }

    public void shutdown() {
        for (UUID uuid : Map.copyOf(batches).keySet()) {
            flush(uuid);
        }
    }

    private void flush(UUID uuid) {
        Batch batch;
        synchronized (lock(uuid)) {
            batch = batches.remove(uuid);
            if (batch == null) {
                return;
            }
            ScheduledTask task = batch.task.getAndSet(null);
            if (task != null) {
                try {
                    task.cancel();
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }
        BigDecimal total = batch.totalSnapshot();
        if (total.signum() <= 0) {
            return;
        }
        notifyOrEnqueue(batch.uuid, batch.name, total);
    }

    private void notifyOrEnqueue(UUID uuid, String name, BigDecimal amount) {
        Optional<Player> online = plugin.proxy().getPlayer(uuid);
        if (online.isPresent()) {
            Player proxyPlayer = online.get();
            if (proxyPlayer.getCurrentServer().isPresent()) {
                byte[] payload = TalerMessages.encodePurchaseNotify(uuid, name, amount);
                proxyPlayer.getCurrentServer().get()
                        .sendPluginMessage(TalerVelocityPlugin.CHANNEL, payload);
                return;
            }
        }
        plugin.executor().execute(() -> {
            try {
                plugin.pendingNotify().enqueue(uuid, amount);
            } catch (Exception exception) {
                plugin.logger().error("Pending-Notify speichern fehlgeschlagen", exception);
            }
        });
    }

    private static Object lock(UUID uuid) {
        // interned string lock per uuid – ausreichend bei niedriger Kauf-Frequenz
        return ("taler-notify-" + uuid).intern();
    }

    private static final class Batch {
        private final UUID uuid;
        private volatile String name;
        private BigDecimal total;
        private final AtomicReference<ScheduledTask> task = new AtomicReference<>();

        private Batch(UUID uuid, String name, BigDecimal amount) {
            this.uuid = uuid;
            this.name = name;
            this.total = amount;
        }

        private synchronized void add(BigDecimal amount) {
            this.total = this.total.add(amount);
        }

        private synchronized BigDecimal totalSnapshot() {
            return total;
        }
    }
}
