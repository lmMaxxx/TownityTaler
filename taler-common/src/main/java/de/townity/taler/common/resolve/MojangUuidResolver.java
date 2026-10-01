package de.townity.taler.common.resolve;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Löst Spielernamen zu UUIDs auf (Mojang Session Server).
 * Für Offline-Spieler, die nicht in der DB stehen.
 */
public final class MojangUuidResolver {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F]{32})\"");
    private static final Pattern NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public CompletableFuture<Optional<ResolvedProfile>> resolve(String name) {
        String encoded = URLEncoder.encode(name.trim(), StandardCharsets.UTF_8);
        URI uri = URI.create("https://api.mojang.com/users/profiles/minecraft/" + encoded);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        return Optional.<ResolvedProfile>empty();
                    }
                    String body = response.body();
                    Matcher idMatcher = ID_PATTERN.matcher(body);
                    Matcher nameMatcher = NAME_PATTERN.matcher(body);
                    if (!idMatcher.find()) {
                        return Optional.<ResolvedProfile>empty();
                    }
                    String dashed = toDashedUuid(idMatcher.group(1));
                    String resolvedName = nameMatcher.find() ? nameMatcher.group(1) : name.trim();
                    return Optional.of(new ResolvedProfile(UUID.fromString(dashed), resolvedName));
                })
                .exceptionally(error -> Optional.<ResolvedProfile>empty());
    }

    private static String toDashedUuid(String raw) {
        String hex = raw.toLowerCase();
        return hex.substring(0, 8) + "-"
                + hex.substring(8, 12) + "-"
                + hex.substring(12, 16) + "-"
                + hex.substring(16, 20) + "-"
                + hex.substring(20, 32);
    }

    public record ResolvedProfile(UUID uuid, String name) {
    }
}
