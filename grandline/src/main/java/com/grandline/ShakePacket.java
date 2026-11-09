package com.grandline;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Servidor -> clientes proximos: tremor de camera + flash de tela. */
public class ShakePacket {
    public final double x, y, z;
    public final float intensity, flash, range;
    public final int ticks, color;

    public ShakePacket(double x, double y, double z, float intensity, int ticks, int color, float flash, float range) {
        this.x = x; this.y = y; this.z = z; this.intensity = intensity; this.ticks = ticks;
        this.color = color; this.flash = flash; this.range = range;
    }

    public static void encode(ShakePacket m, FriendlyByteBuf b) {
        b.writeDouble(m.x); b.writeDouble(m.y); b.writeDouble(m.z);
        b.writeFloat(m.intensity); b.writeVarInt(m.ticks); b.writeInt(m.color);
        b.writeFloat(m.flash); b.writeFloat(m.range);
    }

    public static ShakePacket decode(FriendlyByteBuf b) {
        return new ShakePacket(b.readDouble(), b.readDouble(), b.readDouble(),
                b.readFloat(), b.readVarInt(), b.readInt(), b.readFloat(), b.readFloat());
    }

    public static void handle(ShakePacket m, Supplier<NetworkEvent.Context> s) {
        NetworkEvent.Context c = s.get();
        c.enqueueWork(() -> ClientState.applyShake(m));
        c.setPacketHandled(true);
    }
}
