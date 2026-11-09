package com.grandline;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Servidor -> dono: estado da fruta, haki, combo e cooldowns para o HUD. */
public class SyncPacket {
    public static final int CDS = 7; // 0-3 habilidades, 4 rei, 5 soru, 6 rankyaku

    public final String fruit;
    public final boolean arm, obs, logia, hard;
    public final int combo;
    public final int[] cd;

    public SyncPacket(String fruit, boolean arm, boolean obs, boolean logia, boolean hard, int combo, int[] cd) {
        this.fruit = fruit; this.arm = arm; this.obs = obs; this.logia = logia; this.hard = hard;
        this.combo = combo; this.cd = cd;
    }

    public static void encode(SyncPacket m, FriendlyByteBuf b) {
        b.writeUtf(m.fruit);
        b.writeBoolean(m.arm); b.writeBoolean(m.obs); b.writeBoolean(m.logia); b.writeBoolean(m.hard);
        b.writeVarInt(m.combo);
        for (int i = 0; i < CDS; i++) b.writeVarInt(m.cd[i]);
    }

    public static SyncPacket decode(FriendlyByteBuf b) {
        String f = b.readUtf();
        boolean a = b.readBoolean(), o = b.readBoolean(), l = b.readBoolean(), h = b.readBoolean();
        int combo = b.readVarInt();
        int[] cd = new int[CDS];
        for (int i = 0; i < CDS; i++) cd[i] = b.readVarInt();
        return new SyncPacket(f, a, o, l, h, combo, cd);
    }

    public static void handle(SyncPacket m, Supplier<NetworkEvent.Context> s) {
        NetworkEvent.Context c = s.get();
        c.enqueueWork(() -> ClientState.applySync(m));
        c.setPacketHandled(true);
    }
}
