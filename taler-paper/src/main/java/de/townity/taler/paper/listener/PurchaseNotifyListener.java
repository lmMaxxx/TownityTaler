package de.townity.taler.paper.listener;

import de.townity.taler.common.msg.TalerMessages;
import de.townity.taler.paper.TalerPaperPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class PurchaseNotifyListener implements PluginMessageListener {

    private final TalerPaperPlugin plugin;

    public PurchaseNotifyListener(TalerPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(
            @NotNull String channel,
            @NotNull Player messenger,
            byte @NotNull [] message
    ) {
        Optional<TalerMessages.PurchaseNotify> decoded = TalerMessages.decode(message);
        if (decoded.isEmpty()) {
            return;
        }
        TalerMessages.PurchaseNotify notify = decoded.get();
        Player target = Bukkit.getPlayer(notify.uuid());
        if (target == null || !target.isOnline()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () ->
                plugin.purchaseNotifier().notifyPurchase(target, notify.amount())
        );
    }
}
