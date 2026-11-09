package com.grandline;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Tecnicas de combate: Soru (dash), Geppo (pulos no ar) e Rankyaku (lamina de ar). */
public final class Techniques {
    public static final int SORU_CD = 50;
    public static final int RANKYAKU_CD = 40;
    public static final int GEPPO_MAX = 3;

    private Techniques() {}

    private static boolean ready(ServerPlayer p, String key) {
        int rem = PlayerData.remaining(p, key);
        if (rem > 0) {
            p.displayClientMessage(Component.translatable("msg.grandline.cooldown", rem / 20 + 1), true);
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------ SORU

    public static void soru(ServerPlayer p) {
        if (!ready(p, "cd5")) return;
        ServerLevel l = p.serverLevel();
        Vec3 look = p.getLookAngle();
        Vec3 dir = new Vec3(look.x, look.y * 0.25, look.z).normalize();
        Vec3 start = p.position();

        // avanca em passos de 0,5 bloco ate bater em algo (maximo 9 blocos)
        Vec3 end = start;
        for (double dist = 0.5; dist <= 9.0; dist += 0.5) {
            Vec3 off = dir.scale(dist);
            if (!l.noCollision(p, p.getBoundingBox().move(off.x, off.y, off.z))) break;
            end = start.add(off);
        }
        if (end.distanceToSqr(start) < 1.0) {
            p.displayClientMessage(Component.translatable("msg.grandline.blocked"), true);
            return;
        }
        PlayerData.setCooldown(p, "cd5", SORU_CD);

        // imagem residual: fumaça e faiscas ao longo do caminho
        Fx.spread(l, ParticleTypes.CLOUD, start.add(0, 1, 0), 14, 0.3, 0.05);
        Fx.line(l, Fx.dust(0xFFFFFF, 1.0f), start.add(0, 0.9, 0), end.add(0, 0.9, 0), 0.3);
        Fx.line(l, ParticleTypes.CLOUD, start.add(0, 0.4, 0), end.add(0, 0.4, 0), 0.7);
        Fx.line(l, ParticleTypes.ELECTRIC_SPARK, start.add(0, 1.4, 0), end.add(0, 1.4, 0), 0.6);
        l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.9f);

        // quem estava no caminho leva um golpe
        Set<LivingEntity> done = new HashSet<>();
        Vec3 delta = end.subtract(start);
        for (double t = 0; t <= 1.0; t += 0.1) {
            Vec3 pt = start.add(delta.scale(t)).add(0, 1, 0);
            for (LivingEntity e : Abilities.near(p, pt, 1.6)) {
                if (done.add(e)) Abilities.hit(p, e, 4f, 0.6, dir);
            }
        }

        p.teleportTo(end.x, end.y, end.z);
        p.setDeltaMovement(dir.scale(0.6));
        p.hurtMarked = true;
        p.fallDistance = 0;
        Fx.spread(l, ParticleTypes.POOF, end.add(0, 1, 0), 10, 0.3, 0.05);
        Fx.ring(l, ParticleTypes.CLOUD, end.add(0, 0.2, 0), 0.8, 12);
        Network.sendSync(p);
    }

    // ------------------------------------------------------------ GEPPO

    /** Chamado todo tick: recarrega os pulos ao tocar o chao. */
    static void tickGround(ServerPlayer p) {
        if (p.onGround() || p.isInWater()) {
            if (PlayerData.tag(p).getInt("geppo") != 0) PlayerData.tag(p).putInt("geppo", 0);
        }
    }

    public static void geppo(ServerPlayer p) {
        if (PlayerData.remaining(p, "gepcd") > 0) return;
        ServerLevel l = p.serverLevel();
        Vec3 look = p.getLookAngle();
        Vec3 feet = p.position();
        if (p.onGround()) { // impulso inicial do chao, nao gasta pulo aereo
            p.setDeltaMovement(look.x * 0.5, 0.95, look.z * 0.5);
        } else {
            int used = PlayerData.tag(p).getInt("geppo");
            if (used >= GEPPO_MAX) {
                p.displayClientMessage(Component.translatable("msg.grandline.no_geppo"), true);
                return;
            }
            PlayerData.tag(p).putInt("geppo", used + 1);
            p.setDeltaMovement(look.x * 0.7, 0.85, look.z * 0.7);
        }
        PlayerData.setCooldown(p, "gepcd", 6);
        p.hurtMarked = true;
        p.fallDistance = 0;
        // anel de ar sob os pes, como um chute no vazio
        Fx.ring(l, ParticleTypes.CLOUD, feet.add(0, 0.1, 0), 0.9, 14);
        Fx.ring(l, Fx.dust(0xFFFFFF, 1.4f), feet.add(0, 0.15, 0), 0.6, 10);
        Fx.ring(l, ParticleTypes.CLOUD, feet.add(0, 0.1, 0), 0.4, 8);
        Fx.spread(l, ParticleTypes.CRIT, feet, 6, 0.3, 0.2);
        l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.8f, 1.8f);
    }

    // ------------------------------------------------------------ RANKYAKU

    public static void rankyaku(ServerPlayer p) {
        if (!ready(p, "cd6")) return;
        PlayerData.setCooldown(p, "cd6", RANKYAKU_CD);
        ServerLevel l = p.serverLevel();
        Vec3 d = p.getLookAngle().normalize();
        Vec3 side = new Vec3(-d.z, 0, d.x);
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
        // inclina a lamina em ~25 graus
        final Vec3 blade = side.scale(Math.cos(0.45)).add(0, Math.sin(0.45), 0);
        Vec3 start = p.getEyePosition().add(d.scale(1.0)).add(0, -0.4, 0);
        Set<LivingEntity> done = new HashSet<>();
        l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 0.6f);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);

        for (int i = 1; i <= 14; i++) {
            final int k = i;
            Scheduler.later(i, () -> {
                Vec3 tip = start.add(d.scale(k * 1.6));
                BlockPos bp = BlockPos.containing(tip);
                if (!l.getBlockState(bp).getCollisionShape(l, bp).isEmpty()) {
                    Fx.spread(l, ParticleTypes.CRIT, tip.subtract(d.scale(0.5)), 14, 0.5, 0.4);
                    return;
                }
                Vec3 center = tip.subtract(d.scale(2.0));
                Fx.arc(l, Fx.dust(0xFFFFFF, 1.5f), center, d, blade, 2.2, -1.1, 1.1, 18);
                Fx.arc(l, Fx.dust(0xBFD8FF, 1.1f), center, d, blade, 2.5, -1.0, 1.0, 14);
                Fx.arc(l, ParticleTypes.END_ROD, center, d, blade, 2.2, -1.1, 1.1, 6);
                Fx.spread(l, ParticleTypes.CLOUD, tip, 3, 0.4, 0.02);
                for (LivingEntity t : Abilities.near(p, tip, 2.4)) {
                    if (done.add(t)) {
                        Abilities.hit(p, t, 8f, 1.0, d);
                        Fx.spread(l, ParticleTypes.CRIT, t.position().add(0, t.getBbHeight() / 2, 0), 12, 0.4, 0.4);
                    }
                }
            });
        }
        Network.sendSync(p);
    }
}
