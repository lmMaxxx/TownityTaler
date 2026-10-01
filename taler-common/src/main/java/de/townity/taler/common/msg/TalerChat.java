package de.townity.taler.common.msg;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Townity-Chat-Prefix wie Citybuild: {@code &6&lTOWNITY.DE &7⇒}
 */
public final class TalerChat {

    private TalerChat() {
    }

    public static Component prefix() {
        return Component.text()
                .append(Component.text("TOWNITY.DE", NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text(" ⇒ ", NamedTextColor.GRAY)
                        .decoration(TextDecoration.BOLD, false))
                .build();
    }

    /** Prefix + grauer Body. */
    public static Component info(String grayBody) {
        return prefix().append(Component.text(grayBody, NamedTextColor.GRAY)
                .decoration(TextDecoration.BOLD, false));
    }

    /** Prefix + freier Body (Styles selbst setzen). */
    public static Component of(Component body) {
        return prefix().append(body);
    }
}
