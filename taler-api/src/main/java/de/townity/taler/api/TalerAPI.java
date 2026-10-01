package de.townity.taler.api;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Netzwerkweite Taler-API für Unterserver (Paper).
 * Zugriff typischerweise über Bukkit-{@code ServicesManager} oder
 * {@link TalerProvider#get()}.
 */
public interface TalerAPI {

    String CURRENCY_SYMBOL = "₮";

    CompletableFuture<BigDecimal> getBalance(UUID uuid);

    /**
     * Addiert Taler (auch offline).
     *
     * @return neuer Kontostand
     */
    CompletableFuture<BigDecimal> add(UUID uuid, String knownName, BigDecimal amount);

    /**
     * Zieht Taler ab.
     *
     * @return empty bei zu wenig Guthaben, sonst neuer Stand
     */
    CompletableFuture<Optional<BigDecimal>> remove(UUID uuid, BigDecimal amount);

    /**
     * Setzt den Kontostand absolut (≥ 0).
     *
     * @return neuer Kontostand
     */
    CompletableFuture<BigDecimal> set(UUID uuid, String knownName, BigDecimal amount);

    CompletableFuture<Boolean> has(UUID uuid, BigDecimal amount);

    /** Formatiert Beträge im DE-Stil, z. B. {@code 1.000,00₮}. */
    String format(BigDecimal amount);
}
