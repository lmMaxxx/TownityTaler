package de.townity.taler.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

/**
 * Betrags-Helfer: nur ganze Zahlen, DE-Tausenderpunkte, Suffix „ Taler“.
 * Beispiel: {@code 1.000 Taler}
 */
public final class TalerAmounts {

    public static final int SCALE = 0;
    public static final String UNIT = " Taler";

    private static final DecimalFormat FORMAT;

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.GERMANY);
        FORMAT = new DecimalFormat("#,##0", symbols);
    }

    private TalerAmounts() {
    }

    public static BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(SCALE, RoundingMode.DOWN);
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    /**
     * Parst nur ganze Beträge. Erlaubt DE-Tausender ({@code 2.500} → 2500).
     * Nachkommastellen ({@code 1,5} / {@code 1.50}) → empty.
     */
    public static Optional<BigDecimal> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String trimmed = raw.trim().replace(" ", "");
        if (trimmed.toLowerCase(Locale.ROOT).endsWith("taler")) {
            trimmed = trimmed.substring(0, trimmed.length() - 5).trim();
        }
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        // Komma als Dezimaltrenner → Nachkommastellen nicht erlaubt
        if (trimmed.contains(",")) {
            int comma = trimmed.indexOf(',');
            String after = trimmed.substring(comma + 1).replace(".", "");
            if (!after.isEmpty() && after.chars().anyMatch(c -> c != '0')) {
                return Optional.empty();
            }
            // ,00 o. ä. → Komma und Nachkommastellen streichen
            trimmed = trimmed.substring(0, comma).replace(".", "");
        } else if (trimmed.contains(".")) {
            if (isGermanThousands(trimmed)) {
                trimmed = trimmed.replace(".", "");
            } else {
                // z. B. 1.5 oder 1.50 → Nachkommastellen
                return Optional.empty();
            }
        }

        if (!trimmed.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }
        if (trimmed.length() > 15) {
            return Optional.empty();
        }
        try {
            BigDecimal value = new BigDecimal(trimmed);
            if (value.signum() < 0) {
                return Optional.empty();
            }
            if (value.scale() > 0 && value.stripTrailingZeros().scale() > 0) {
                return Optional.empty();
            }
            return Optional.of(normalize(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static String format(BigDecimal amount) {
        synchronized (FORMAT) {
            return FORMAT.format(normalize(amount)) + UNIT;
        }
    }

    private static boolean isGermanThousands(String value) {
        String[] parts = value.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].length() != 3 || !parts[i].chars().allMatch(Character::isDigit)) {
                return false;
            }
        }
        return parts[0].length() >= 1 && parts[0].chars().allMatch(Character::isDigit);
    }
}
