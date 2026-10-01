package de.townity.taler.common.service;

import de.townity.taler.api.TalerAPI;
import de.townity.taler.common.TalerAmounts;
import de.townity.taler.common.db.TalerRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Gemeinsame {@link TalerAPI}-Implementierung (async über Executor). */
public final class DefaultTalerService implements TalerAPI {

    private final TalerRepository repository;
    private final Executor executor;
    private final Logger logger;

    public DefaultTalerService(TalerRepository repository, Executor executor, Logger logger) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public CompletableFuture<BigDecimal> getBalance(UUID uuid) {
        return supplyAsync(() -> repository.getBalance(uuid));
    }

    @Override
    public CompletableFuture<BigDecimal> add(UUID uuid, String knownName, BigDecimal amount) {
        return supplyAsync(() -> repository.add(uuid, knownName, amount));
    }

    @Override
    public CompletableFuture<Optional<BigDecimal>> remove(UUID uuid, BigDecimal amount) {
        return supplyAsync(() -> repository.remove(uuid, amount));
    }

    @Override
    public CompletableFuture<BigDecimal> set(UUID uuid, String knownName, BigDecimal amount) {
        return supplyAsync(() -> repository.set(uuid, knownName, amount));
    }

    @Override
    public CompletableFuture<Boolean> has(UUID uuid, BigDecimal amount) {
        return supplyAsync(() -> repository.has(uuid, amount));
    }

    @Override
    public String format(BigDecimal amount) {
        return TalerAmounts.format(amount);
    }

    public TalerRepository repository() {
        return repository;
    }

    private <T> CompletableFuture<T> supplyAsync(SqlSupplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (SQLException exception) {
                logger.log(Level.SEVERE, "Taler-DB-Fehler", exception);
                throw new RuntimeException(exception);
            }
        }, executor);
    }

    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws SQLException;
    }
}
