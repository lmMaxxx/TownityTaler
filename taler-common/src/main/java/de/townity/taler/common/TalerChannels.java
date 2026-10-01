package de.townity.taler.common;

/** Plugin-Messaging zwischen Velocity und Paper. */
public final class TalerChannels {

    public static final String CHANNEL = "townity:taler";

    /** Velocity → Paper: Kauf/Gutschrift-Benachrichtigung. */
    public static final byte TYPE_PURCHASE_NOTIFY = 1;

    private TalerChannels() {
    }
}
