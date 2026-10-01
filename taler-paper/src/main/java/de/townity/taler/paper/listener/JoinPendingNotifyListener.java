package de.townity.taler.paper.listener;

import de.townity.taler.common.db.PendingNotifyRepository;
import de.townity.taler.paper.TalerPaperPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;

/** Zeigt offline eingegangene Käufe nach dem Join. */
public final class JoinPendingNotifyListener implements Listener {

    private final TalerPaperPlugin plugin;

    public JoinPendingNotifyListener(TalerPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.executor().execute(() -> {
            try {
                List<PendingNotifyRepository.PendingNotify> pending =
                        plugin.pendingNotify().drain(player.getUniqueId());
                if (pending.isEmpty()) {
                    return;
                }
                BigDecimal total = pending.stream()
                        .map(PendingNotifyRepository.PendingNotify::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        plugin.purchaseNotifier().notifyPurchase(player, total);
                    }
                }, 40L);
            } catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING, "Pending-Kauf-Notify fehlgeschlagen", exception);
            }
        });
    }
}
