package com.grandline;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;

/** Estado visual de cada jogador (inclui os outros): corpo de fogo/gelo e animacao de golpe. */
public final class ClientVisuals {
    public static final int FIRE = 1, ICE = 2, HARD = 4, ARM = 8;

    public static final class Vis {
        public int flags;
        public int anim;
        public long animStart;
    }

    private static final Map<UUID, Vis> MAP = new HashMap<>();

    private ClientVisuals() {}

    public static Vis get(UUID id) { return MAP.get(id); }

    public static void clear() { MAP.clear(); }

    public static void apply(VisualPacket m) {
        Vis v = MAP.computeIfAbsent(m.id, k -> new Vis());
        v.flags = m.flags;
        if (m.anim > 0) {
            v.anim = m.anim;
            Minecraft mc = Minecraft.getInstance();
            v.animStart = mc.level == null ? 0 : mc.level.getGameTime();
        }
        if (v.flags == 0 && v.anim == 0) MAP.remove(m.id);
    }
}
