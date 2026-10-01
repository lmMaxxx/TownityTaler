package de.townity.taler.paper.notify;

import de.townity.taler.common.TalerAmounts;
import de.townity.taler.common.msg.TalerChat;
import de.townity.taler.paper.TalerPaperPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

/** Kauf-Benachrichtigung (Chat hervorgehoben, ohne Title). */
public final class PurchaseNotifier {

    private final TalerPaperPlugin plugin;

    public PurchaseNotifier(TalerPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public void notifyPurchase(Player player, BigDecimal amount) {
        String formatted = TalerAmounts.format(amount);

        player.sendMessage(Component.empty());
        player.sendMessage(TalerChat.of(Component.text()
                .append(Component.text("Du hast ", NamedTextColor.GRAY)
                        .decoration(TextDecoration.BOLD, false))
                .append(Component.text(formatted, NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text(" erhalten!", NamedTextColor.GRAY)
                        .decoration(TextDecoration.BOLD, false))
                .build()));
        player.sendMessage(TalerChat.of(Component.text()
                .append(Component.text("Danke für deinen Einkauf im ", NamedTextColor.GRAY)
                        .decoration(TextDecoration.BOLD, false))
                .append(Component.text("Onlineshop", NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text("!", NamedTextColor.GRAY)
                        .decoration(TextDecoration.BOLD, false))
                .build()));
        player.sendMessage(Component.empty());

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }
}
