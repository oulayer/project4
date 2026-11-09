package com.grandline;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Cliente -> servidor: 0-3 habilidades, 4 armamento, 5 observacao, 6 rei, 7 logia, 8 endurecimento, 9 soru, 10 geppo, 11 rankyaku. */
public class ActionPacket {
    public final int action;

    public ActionPacket(int action) { this.action = action; }

    public static void encode(ActionPacket m, FriendlyByteBuf b) { b.writeVarInt(m.action); }
    public static ActionPacket decode(FriendlyByteBuf b) { return new ActionPacket(b.readVarInt()); }

    public static void handle(ActionPacket m, Supplier<NetworkEvent.Context> s) {
        NetworkEvent.Context c = s.get();
        c.enqueueWork(() -> {
            ServerPlayer p = c.getSender();
            if (p == null || !p.isAlive() || p.isSpectator()) return;
            switch (m.action) {
                case 0, 1, 2, 3 -> Abilities.use(p, m.action);
                case 4 -> Haki.toggleArmament(p);
                case 5 -> Haki.toggleObservation(p);
                case 6 -> Haki.conqueror(p);
                case 7 -> Logia.toggle(p);
                case 8 -> HakiHard.toggle(p);
                case 9 -> Techniques.soru(p);
                case 10 -> Techniques.geppo(p);
                case 11 -> Techniques.rankyaku(p);
                case 12 -> FireStyle.attack(p);
                default -> { }
            }
        });
        c.setPacketHandled(true);
    }
}
