package com.grandline;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Endurecimento (Koka): o Haki do Armamento cobre o corpo inteiro com uma armadura negra. */
public final class HakiHard {
    private HakiHard() {}

    public static void toggle(ServerPlayer p) {
        if (!PlayerData.isArmament(p)) {
            p.displayClientMessage(Component.translatable("msg.grandline.needs_arm"), true);
            return;
        }
        boolean on = !PlayerData.isHard(p);
        if (on && p.getFoodData().getFoodLevel() <= 6 && !p.isCreative()) {
            p.displayClientMessage(Component.translatable("msg.grandline.exhausted"), true);
            return;
        }
        PlayerData.setHardFlag(p, on);
        p.displayClientMessage(Component.translatable(on ? "msg.grandline.hard_on" : "msg.grandline.hard_off"), true);
        if (on) intro(p);
        Network.sendSync(p);
    }

    /** Onda negra sobe dos pes a cabeca, com brilho metalico e raios vermelhos, e termina num estalo. */
    private static void intro(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        l.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8f, 1.6f);
        l.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2f, 0.5f);
        double h = p.getBbHeight();
        for (int i = 0; i < 14; i++) {
            final int k = i;
            Scheduler.later(i, () -> {
                if (!p.isAlive()) return;
                Vec3 c = p.position().add(0, h * k / 13.0, 0);
                Fx.ring(l, Fx.DARK, c, 0.65, 16);
                Fx.ring(l, Fx.CRIMSON, c.add(0, 0.05, 0), 0.5, 10);
                Fx.ring(l, ParticleTypes.END_ROD, c.add(0, 0.1, 0), 0.6, 6);
                Fx.spread(l, ParticleTypes.ELECTRIC_SPARK, c, 3, 0.4, 0.2);
            });
        }
        Scheduler.later(14, () -> {
            if (!p.isAlive()) return;
            Vec3 c = p.position();
            for (int r = 1; r <= 5; r++) Fx.ring(l, Fx.CRIMSON, c.add(0, 0.1 + r * 0.04, 0), r * 0.8, 24);
            Fx.sphere(l, Fx.DARK, c.add(0, 1, 0), 1.1, 30);
            Fx.spread(l, ParticleTypes.FLASH, c.add(0, 1, 0), 1, 0, 0);
            l.playSound(null, p.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 1f, 0.6f);
            Network.shake(l, c, 0.9f, 10, 0x22000A, 0.18f, 18);
        });
    }

    /** Chamado todo tick pelo servidor enquanto o endurecimento esta ligado. */
    static void tick(ServerPlayer p) {
        if (!PlayerData.isArmament(p)) {
            PlayerData.setHardFlag(p, false);
            Network.sendSync(p);
            return;
        }
        if (p.tickCount % 2 != 0) return;
        ServerLevel l = p.serverLevel();
        RandomSource rnd = p.getRandom();
        Vec3 base = p.position();
        double h = p.getBbHeight();

        // armadura de particulas negras nos 5 niveis do corpo
        for (int lv = 0; lv < 5; lv++) {
            double y = h * (lv + 0.5) / 5.0;
            double a = rnd.nextDouble() * Math.PI * 2;
            Vec3 pos = base.add(Math.cos(a) * 0.36, y, Math.sin(a) * 0.36);
            Fx.p(l, Fx.DARK, pos);
            if (rnd.nextFloat() < 0.18f) Fx.spread(l, ParticleTypes.END_ROD, pos, 1, 0.02, 0.01); // brilho metalico
            if (rnd.nextFloat() < 0.12f) Fx.spread(l, ParticleTypes.ELECTRIC_SPARK, pos, 1, 0.15, 0.1);
        }
        // raios vermelhos correndo pelos bracos
        if (rnd.nextFloat() < 0.35f) {
            Vec3 a = base.add(rnd.nextGaussian() * 0.3, 0.6 + rnd.nextDouble() * 0.9, rnd.nextGaussian() * 0.3);
            Fx.line(l, Fx.dust(0xD0182A, 0.7f), a, a.add(rnd.nextGaussian() * 0.5, rnd.nextGaussian() * 0.4, rnd.nextGaussian() * 0.5), 0.15);
        }
        // pulso de brilho subindo pelo corpo
        if (p.tickCount % 30 == 0) {
            for (int i = 0; i < 6; i++) {
                final int k = i;
                Scheduler.later(i, () -> {
                    if (!p.isAlive()) return;
                    Vec3 c = p.position().add(0, h * k / 5.0, 0);
                    Fx.ring(l, ParticleTypes.END_ROD, c, 0.5, 8);
                });
            }
        }
    }
}
