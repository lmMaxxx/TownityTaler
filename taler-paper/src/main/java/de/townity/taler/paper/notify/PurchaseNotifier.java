package de.townity.taler.paper.notify;

import de.townity.taler.common.TalerAmounts;
import de.townity.taler.paper.TalerPaperPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.time.Duration;

/** Schöne Kauf-Benachrichtigung (Chat + Title + Sound). */
public final class PurchaseNotifier {

    private final TalerPaperPlugin plugin;

    public PurchaseNotifier(TalerPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public void notifyPurchase(Player player, BigDecimal amount) {
        String formatted = TalerAmounts.format(amount);

        Component prefix = Component.text()
                .append(Component.text("TOWNITY.DE", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" ⇒ ", NamedTextColor.GRAY))
                .build();

        player.sendMessage(Component.empty());
        player.sendMessage(prefix.append(Component.text("Kauf erfolgreich!", NamedTextColor.GOLD)));
        player.sendMessage(prefix.append(Component.text()
                .append(Component.text("Dir wurden ", NamedTextColor.GRAY))
                .append(Component.text(formatted, NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" gutgeschrieben.", NamedTextColor.GRAY))
                .build()));
        player.sendMessage(Component.empty());

        Title title = Title.title(
                Component.text("Kauf erfolgreich!", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text("+" + formatted, NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(700))
        );
        player.showTitle(title);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }
}
