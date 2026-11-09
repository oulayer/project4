package com.grandline;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Estado do lado do cliente (HUD, tremor, flash). */
public final class ClientState {
    public static String fruit = "";
    public static boolean arm, obs, logia, hard;
    public static int combo, comboTimer;
    public static int[] cd = new int[SyncPacket.CDS];

    public static float shakeIntensity;
    public static int shakeTicks, shakeMax = 1;
    public static float flash;
    public static int flashColor;

    private ClientState() {}

    public static void applySync(SyncPacket m) {
        fruit = m.fruit; arm = m.arm; obs = m.obs; logia = m.logia; hard = m.hard; cd = m.cd;
        if (m.combo > 0 && m.combo != combo) comboTimer = 30;
        combo = m.combo;
    }

    public static void applyShake(ShakePacket m) {
        Player pl = Minecraft.getInstance().player;
        if (pl == null) return;
        double d = pl.position().distanceTo(new Vec3(m.x, m.y, m.z));
        float k = (float) Math.max(0, 1 - d / m.range);
        if (k <= 0) return;
        shakeIntensity = Math.max(shakeIntensity * ((float) shakeTicks / shakeMax), m.intensity * k);
        shakeTicks = shakeMax = Math.max(1, m.ticks);
        if (m.flash > 0) { flash = Math.max(flash, m.flash * k); flashColor = m.color; }
    }

    public static void tick() {
        if (shakeTicks > 0) shakeTicks--;
        if (flash > 0) flash = Math.max(0, flash - 0.05f);
        if (comboTimer > 0) comboTimer--;
        // reduz os cooldowns localmente entre sincronizacoes
        for (int i = 0; i < cd.length; i++) if (cd[i] > 0) cd[i]--;
    }

    public static int maxCd(int slot) {
        if (slot == 4) return Haki.CONQUEROR_CD;
        if (slot == 5) return Techniques.SORU_CD;
        if (slot == 6) return Techniques.RANKYAKU_CD;
        Fruit f = Fruit.byId(fruit);
        return f == null ? 1 : f.cooldowns[slot];
    }
}
