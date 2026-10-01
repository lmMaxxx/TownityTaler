package de.townity.taler.api;

/**
 * Globale Provider-Hülle, damit abhängige Plugins die API ohne Bukkit-Service-Lookup
 * nutzen können (wird vom Paper-Plugin beim Enable gesetzt).
 */
public final class TalerProvider {

    private static volatile TalerAPI instance;

    private TalerProvider() {
    }

    public static void register(TalerAPI api) {
        instance = api;
    }

    public static void unregister(TalerAPI api) {
        if (instance == api) {
            instance = null;
        }
    }

    public static boolean isReady() {
        return instance != null;
    }

    public static TalerAPI get() {
        TalerAPI api = instance;
        if (api == null) {
            throw new IllegalStateException("TalerAPI ist nicht registriert (TownityTaler-Paper geladen?).");
        }
        return api;
    }
}
