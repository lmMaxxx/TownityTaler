package de.townity.taler.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.townity.taler.common.TalerAmounts;
import de.townity.taler.common.db.TalerRepository;
import de.townity.taler.common.msg.TalerMessages;
import de.townity.taler.velocity.TalerVelocityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Shop-/Webhook-Befehl: gutschreiben + Ingame-Kaufbenachrichtigung.
 * Nutzung: {@code /boughttaler <Spieler> <Anzahl>}
 */
public final class BoughtTalerCommand implements SimpleCommand {

    private static final String PERMISSION = "townity.taler.bought";

    private final TalerVelocityPlugin plugin;

    public BoughtTalerCommand(TalerVelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission(PERMISSION)) {
            source.sendMessage(Component.text("Keine Berechtigung.", NamedTextColor.RED));
            return;
        }
        String[] args = invocation.arguments();
        if (args.length < 2) {
            source.sendMessage(Component.text("Nutzung: /boughttaler <Spieler> <Anzahl>", NamedTextColor.RED));
            return;
        }
        Optional<BigDecimal> amountOpt = TalerAmounts.parse(args[1]);
        if (amountOpt.isEmpty() || amountOpt.get().signum() <= 0) {
            source.sendMessage(Component.text("Ungültiger Betrag.", NamedTextColor.RED));
            return;
        }
        BigDecimal amount = amountOpt.get();
        resolveTarget(args[0]).thenCompose(target -> {
            if (target.isEmpty()) {
                source.sendMessage(Component.text("Spieler nicht gefunden: " + args[0], NamedTextColor.RED));
                return CompletableFuture.completedFuture(null);
            }
            Resolved player = target.get();
            return plugin.service().add(player.uuid(), player.name(), amount)
                    .thenAccept(balance -> {
                        notifyPurchase(player, amount);
                        source.sendMessage(Component.text()
                                .append(Component.text("TOWNITY.DE ", NamedTextColor.GOLD))
                                .append(Component.text("⇒ ", NamedTextColor.GRAY))
                                .append(Component.text(
                                        "Kauf gutgeschrieben: " + plugin.service().format(amount)
                                                + " an " + player.name()
                                                + " (neu: " + plugin.service().format(balance) + ")",
                                        NamedTextColor.GRAY
                                ))
                                .build());
                    })
                    .exceptionally(error -> {
                        source.sendMessage(Component.text("Datenbankfehler.", NamedTextColor.RED));
                        return null;
                    });
        });
    }

    private void notifyPurchase(Resolved player, BigDecimal amount) {
        Optional<Player> online = plugin.proxy().getPlayer(player.uuid());
        if (online.isPresent()) {
            Player proxyPlayer = online.get();
            if (proxyPlayer.getCurrentServer().isPresent()) {
                byte[] payload = TalerMessages.encodePurchaseNotify(player.uuid(), player.name(), amount);
                proxyPlayer.getCurrentServer().get()
                        .sendPluginMessage(TalerVelocityPlugin.CHANNEL, payload);
                return;
            }
        }
        plugin.executor().execute(() -> {
            try {
                plugin.pendingNotify().enqueue(player.uuid(), amount);
            } catch (Exception exception) {
                plugin.logger().error("Pending-Notify speichern fehlgeschlagen", exception);
            }
        });
    }

    private CompletableFuture<Optional<Resolved>> resolveTarget(String rawName) {
        Optional<Player> online = plugin.proxy().getPlayer(rawName);
        if (online.isPresent()) {
            Player player = online.get();
            return CompletableFuture.completedFuture(
                    Optional.of(new Resolved(player.getUniqueId(), player.getUsername()))
            );
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<TalerRepository.PlayerLookup> known =
                        plugin.service().repository().findByName(rawName);
                if (known.isPresent()) {
                    TalerRepository.PlayerLookup row = known.get();
                    return Optional.of(new Resolved(row.uuid(), row.name()));
                }
            } catch (Exception exception) {
                plugin.logger().error("Lookup fehlgeschlagen", exception);
            }
            return Optional.<Resolved>empty();
        }, plugin.executor()).thenCompose(fromDb -> {
            if (fromDb.isPresent()) {
                return CompletableFuture.completedFuture(fromDb);
            }
            CompletableFuture<Optional<Resolved>> mojang = new CompletableFuture<>();
            plugin.uuidResolver().resolve(rawName).whenComplete((profile, error) -> {
                if (error != null || profile == null || profile.isEmpty()) {
                    mojang.complete(Optional.empty());
                    return;
                }
                var resolved = profile.get();
                mojang.complete(Optional.of(new Resolved(resolved.uuid(), resolved.name())));
            });
            return mojang;
        });
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(PERMISSION);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return plugin.proxy().getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .limit(20)
                    .toList();
        }
        return List.of();
    }

    private record Resolved(UUID uuid, String name) {
    }
}
