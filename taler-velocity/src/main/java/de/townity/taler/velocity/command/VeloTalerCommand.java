package de.townity.taler.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.townity.taler.common.TalerAmounts;
import de.townity.taler.common.db.TalerRepository;
import de.townity.taler.velocity.TalerVelocityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class VeloTalerCommand implements SimpleCommand {

    private static final String PERMISSION = "townity.taler.admin";

    private final TalerVelocityPlugin plugin;

    public VeloTalerCommand(TalerVelocityPlugin plugin) {
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
        if (args.length == 0) {
            sendUsage(source);
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "info", "balance", "bal" -> handleInfo(source, args);
            case "add", "give" -> handleMutate(source, args, Mutate.ADD);
            case "remove", "take" -> handleMutate(source, args, Mutate.REMOVE);
            case "set" -> handleMutate(source, args, Mutate.SET);
            default -> sendUsage(source);
        }
    }

    private void handleInfo(CommandSource source, String[] args) {
        if (args.length < 2) {
            source.sendMessage(Component.text("Nutzung: /velotaler info <Spieler>", NamedTextColor.RED));
            return;
        }
        resolveTarget(args[1]).thenAccept(target -> {
            if (target.isEmpty()) {
                source.sendMessage(Component.text("Spieler nicht gefunden: " + args[1], NamedTextColor.RED));
                return;
            }
            Resolved targetPlayer = target.get();
            plugin.service().getBalance(targetPlayer.uuid()).thenAccept(balance ->
                    source.sendMessage(Component.text()
                            .append(Component.text("Taler von ", NamedTextColor.GRAY))
                            .append(Component.text(targetPlayer.name(), NamedTextColor.GOLD))
                            .append(Component.text(": ", NamedTextColor.GRAY))
                            .append(Component.text(plugin.service().format(balance), NamedTextColor.GOLD))
                            .build())
            ).exceptionally(error -> {
                source.sendMessage(Component.text("Datenbankfehler.", NamedTextColor.RED));
                return null;
            });
        });
    }

    private void handleMutate(CommandSource source, String[] args, Mutate mutate) {
        if (args.length < 3) {
            source.sendMessage(Component.text(
                    "Nutzung: /velotaler " + mutate.label + " <Spieler> <Betrag>",
                    NamedTextColor.RED
            ));
            return;
        }
        Optional<BigDecimal> amountOpt = TalerAmounts.parse(args[2]);
        if (amountOpt.isEmpty() || amountOpt.get().signum() < 0) {
            source.sendMessage(Component.text("Ungültiger Betrag.", NamedTextColor.RED));
            return;
        }
        BigDecimal amount = amountOpt.get();
        resolveTarget(args[1]).thenAccept(target -> {
            if (target.isEmpty()) {
                source.sendMessage(Component.text("Spieler nicht gefunden: " + args[1], NamedTextColor.RED));
                return;
            }
            Resolved targetPlayer = target.get();
            CompletableFuture<?> future = switch (mutate) {
                case ADD -> plugin.service().add(targetPlayer.uuid(), targetPlayer.name(), amount)
                        .thenAccept(balance -> source.sendMessage(success(
                                "Hinzugefügt: " + plugin.service().format(amount)
                                        + " → " + targetPlayer.name()
                                        + " (neu: " + plugin.service().format(balance) + ")"
                        )));
                case REMOVE -> plugin.service().remove(targetPlayer.uuid(), amount)
                        .thenAccept(result -> {
                            if (result.isEmpty()) {
                                source.sendMessage(Component.text(
                                        "Nicht genug Guthaben bei " + targetPlayer.name() + ".",
                                        NamedTextColor.RED
                                ));
                                return;
                            }
                            source.sendMessage(success(
                                    "Abgezogen: " + plugin.service().format(amount)
                                            + " → " + targetPlayer.name()
                                            + " (neu: " + plugin.service().format(result.get()) + ")"
                            ));
                        });
                case SET -> plugin.service().set(targetPlayer.uuid(), targetPlayer.name(), amount)
                        .thenAccept(balance -> source.sendMessage(success(
                                "Gesetzt: " + targetPlayer.name()
                                        + " = " + plugin.service().format(balance)
                        )));
            };
            future.exceptionally(error -> {
                source.sendMessage(Component.text("Datenbankfehler.", NamedTextColor.RED));
                return null;
            });
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

    private static Component success(String text) {
        return Component.text()
                .append(Component.text("TOWNITY.DE ", NamedTextColor.GOLD))
                .append(Component.text("⇒ ", NamedTextColor.GRAY))
                .append(Component.text(text, NamedTextColor.GRAY))
                .build();
    }

    private static void sendUsage(CommandSource source) {
        source.sendMessage(Component.text(
                "Nutzung: /velotaler <add|remove|give|set|info> <Spieler> [Betrag]",
                NamedTextColor.RED
        ));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(PERMISSION);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            return List.of("add", "remove", "give", "set", "info");
        }
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return plugin.proxy().getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .limit(20)
                    .toList();
        }
        return List.of();
    }

    private enum Mutate {
        ADD("add"),
        REMOVE("remove"),
        SET("set");

        private final String label;

        Mutate(String label) {
            this.label = label;
        }
    }

    private record Resolved(UUID uuid, String name) {
    }
}
