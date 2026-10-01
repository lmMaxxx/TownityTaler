package de.townity.taler.common.msg;

import de.townity.taler.common.TalerChannels;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Binär-Payloads für Plugin-Messaging. */
public final class TalerMessages {

    private TalerMessages() {
    }

    public static byte[] encodePurchaseNotify(UUID uuid, String playerName, BigDecimal amount) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buffer);
            out.writeByte(TalerChannels.TYPE_PURCHASE_NOTIFY);
            out.writeLong(uuid.getMostSignificantBits());
            out.writeLong(uuid.getLeastSignificantBits());
            out.writeUTF(playerName == null ? "" : playerName);
            out.writeUTF(amount.toPlainString());
            return buffer.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static Optional<PurchaseNotify> decode(byte[] payload) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload));
            byte type = in.readByte();
            if (type != TalerChannels.TYPE_PURCHASE_NOTIFY) {
                return Optional.empty();
            }
            UUID uuid = new UUID(in.readLong(), in.readLong());
            String name = in.readUTF();
            BigDecimal amount = new BigDecimal(in.readUTF());
            return Optional.of(new PurchaseNotify(uuid, name, amount));
        } catch (IOException | NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public record PurchaseNotify(UUID uuid, String playerName, BigDecimal amount) {
    }
}
