package de.townity.taler.common;

import de.townity.taler.api.TalerAPI;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

/** Betrags-Helfer: max. 2 Nachkommastellen, DE-Format. */
public final class TalerAmounts {

    public static final int SCALE = 2;

    private static final DecimalFormat FORMAT;

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.GERMANY);
        FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    private TalerAmounts() {
    }

    public static BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static Optional<BigDecimal> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String trimmed = raw.trim().replace(" ", "");
        if (trimmed.contains(",") && trimmed.contains(".")) {
            if (trimmed.lastIndexOf(',') > trimmed.lastIndexOf('.')) {
                trimmed = trimmed.replace(".", "").replace(',', '.');
            } else {
                trimmed = trimmed.replace(",", "");
            }
        } else if (trimmed.contains(",")) {
            trimmed = trimmed.replace(',', '.');
        } else if (trimmed.contains(".") && isGermanThousands(trimmed)) {
            trimmed = trimmed.replace(".", "");
        }
        int dot = trimmed.indexOf('.');
        if (dot >= 0 && trimmed.length() - dot - 1 > SCALE) {
            return Optional.empty();
        }
        try {
            return Optional.of(normalize(new BigDecimal(trimmed)));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static String format(BigDecimal amount) {
        synchronized (FORMAT) {
            return FORMAT.format(normalize(amount)) + TalerAPI.CURRENCY_SYMBOL;
        }
    }

    private static boolean isGermanThousands(String value) {
        String[] parts = value.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].length() != 3) {
                return false;
            }
        }
        return parts[0].length() >= 1 && parts[0].chars().allMatch(Character::isDigit);
    }
}
