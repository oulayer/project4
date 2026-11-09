package com.grandline;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Servidor -> jogadores proximos: flags visuais (logia, endurecimento) e animacao de golpe. */
public class VisualPacket {
    public final UUID id;
    public final int flags, anim;

    public VisualPacket(UUID id, int flags, int anim) {
        this.id = id; this.flags = flags; this.anim = anim;
    }

    public static void encode(VisualPacket m, FriendlyByteBuf b) {
        b.writeUUID(m.id); b.writeVarInt(m.flags); b.writeVarInt(m.anim);
    }

    public static VisualPacket decode(FriendlyByteBuf b) {
        return new VisualPacket(b.readUUID(), b.readVarInt(), b.readVarInt());
    }

    public static void handle(VisualPacket m, Supplier<NetworkEvent.Context> s) {
        NetworkEvent.Context c = s.get();
        c.enqueueWork(() -> ClientVisuals.apply(m));
        c.setPacketHandled(true);
    }
}
