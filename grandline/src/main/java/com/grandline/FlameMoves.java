package com.grandline;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Habilidades da Mera Mera no Mi: Hiken, Higan, Explosao de Chamas e Entei. */
public final class FlameMoves {
    private FlameMoves() {}

    public static void use(ServerPlayer p, ServerLevel l, int slot) {
        switch (slot) {
            case 0 -> hiken(p, l);
            case 1 -> higan(p, l);
            case 2 -> burst(p, l);
            default -> entei(p, l);
        }
    }

    // ------------------------------------------------------------ HIKEN

    /** Hiken: lanca um punho de fogo gigante (entidade com modelo proprio) que explode ao bater. */
    private static void hiken(ServerPlayer p, ServerLevel l) {
        Vec3 d = p.getLookAngle().normalize();
        Vec3 start = p.getEyePosition().add(d.scale(1.5)).add(0, -0.4, 0);
        FireFistEntity.spawn(p, l, start, d, false, 1.5f);
        Network.sendVisual(p, 7);
        l.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2f, 0.5f);
        Fx.fireBurst(l, Abilities.handPos(p).add(d.scale(0.8)), 0.9, 40);
    }

    static void explode(ServerPlayer p, ServerLevel l, Vec3 c) { explode(p, l, c, false); }

    static void explode(ServerPlayer p, ServerLevel l, Vec3 c, boolean big) {
        double r = big ? 6.0 : 4.5;
        float dmg = big ? 16f : 9f;
        l.playSound(null, BlockPos.containing(c), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, big ? 3f : 2f, 0.7f);
        Fx.p(l, ParticleTypes.EXPLOSION_EMITTER, c);
        Fx.fireBurst(l, c, big ? 4.0 : 3.0, big ? 170 : 110);
        for (int i = 1; i <= (big ? 6 : 4); i++) Fx.ring(l, ParticleTypes.FLAME, c.add(0, 0.2, 0), i * (big ? 1.2 : 1.0), i * 8);
        for (LivingEntity t : Abilities.near(p, c, r)) {
            t.setSecondsOnFire(big ? 8 : 6);
            Vec3 push = t.position().subtract(c);
            push = push.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : push.normalize();
            t.invulnerableTime = 0;
            Abilities.hit(p, t, dmg, 1.4, push);
        }
        igniteGround(p, l, c, big ? 5 : 3, big ? 16 : 10);
        Network.shake(l, c, big ? 2.6f : 1.8f, big ? 22 : 16, 0xFF6A00, 0.25f, big ? 36 : 28);
    }

    static void igniteGround(ServerPlayer p, ServerLevel l, Vec3 c, int radius, int tries) {
        if (!l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;
        RandomSource rnd = p.getRandom();
        BlockPos b = BlockPos.containing(c);
        for (int i = 0; i < tries; i++) {
            BlockPos bp = b.offset(rnd.nextInt(radius * 2 + 1) - radius, rnd.nextInt(3) - 1, rnd.nextInt(radius * 2 + 1) - radius);
            if (l.isEmptyBlock(bp) && BaseFireBlock.canBePlacedAt(l, bp, Direction.UP)) {
                l.setBlockAndUpdate(bp, Blocks.FIRE.defaultBlockState());
            }
        }
    }

    // ------------------------------------------------------------ HIGAN

    /** Rajada de balas de fogo rapidas. */
    private static void higan(ServerPlayer p, ServerLevel l) {
        Network.sendVisual(p, 6);
        for (int i = 0; i < 9; i++) {
            Scheduler.later(i * 2, () -> {
                if (!p.isAlive()) return;
                RandomSource rnd = p.getRandom();
                Vec3 dir = p.getLookAngle()
                        .add(rnd.nextGaussian() * 0.05, rnd.nextGaussian() * 0.05, rnd.nextGaussian() * 0.05).normalize();
                Vec3 hand = Abilities.handPos(p);
                Abilities.Ray r = Abilities.ray(p, dir, 28);
                Fx.line(l, ParticleTypes.FLAME, hand, r.end(), 0.3);
                Fx.line(l, Fx.dust(0xFF7A1A, 1.2f), hand, r.end(), 0.5);
                Fx.spread(l, ParticleTypes.FLAME, r.end(), 12, 0.25, 0.1);
                Fx.spread(l, ParticleTypes.LAVA, r.end(), 2, 0.3, 0);
                Fx.spread(l, ParticleTypes.FLAME, hand, 6, 0.12, 0.05);
                if (r.target() != null) {
                    LivingEntity t = r.target();
                    t.invulnerableTime = 0;
                    t.setSecondsOnFire(5);
                    Abilities.hit(p, t, 4.5f, 0.5, dir);
                }
                l.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8f, 1.5f);
                p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            });
        }
    }

    // ------------------------------------------------------------ EXPLOSAO

    /** Onda de anéis de fogo com colunas de chamas subindo. */
    private static void burst(ServerPlayer p, ServerLevel l) {
        Vec3 c = p.position();
        l.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.5f, 0.6f);
        for (int i = 0; i < 7; i++) {
            final int r = i + 1;
            Scheduler.later(i * 2, () -> {
                int n = Math.min(48, r * 10);
                Fx.ring(l, ParticleTypes.FLAME, c.add(0, 0.3, 0), r, n);
                Fx.ring(l, ParticleTypes.LARGE_SMOKE, c.add(0, 0.6, 0), r * 0.85, n / 2);
                Fx.spread(l, ParticleTypes.LAVA, c.add(0, 1, 0), 4, r * 0.5, 0);
                int pillars = Math.min(10, r * 2);
                for (int a = 0; a < pillars; a++) {
                    double ang = Math.PI * 2 * a / pillars + r * 0.4;
                    Vec3 base = c.add(Math.cos(ang) * r, 0.1, Math.sin(ang) * r);
                    Fx.line(l, ParticleTypes.FLAME, base, base.add(0, 2.6, 0), 0.35);
                    Fx.p(l, Fx.dust(0xFF5A00, 2.0f), base.add(0, 1.3, 0));
                }
            });
        }
        Scheduler.later(5, () -> {
            for (LivingEntity t : Abilities.near(p, c, 7.5)) {
                t.setSecondsOnFire(6);
                Abilities.hit(p, t, 7f, 1.3, t.position().subtract(c).normalize());
            }
            igniteGround(p, l, c, 5, 12);
            Network.shake(l, c, 1.2f, 12, 0xFF6A00, 0.2f, 24);
        });
    }

    // ------------------------------------------------------------ ENTEI

    /** Grande chama em espiral que avança 24 blocos. */
    private static void entei(ServerPlayer p, ServerLevel l) {
        Vec3 start = p.position().add(0, 1.0, 0);
        Vec3 d = p.getLookAngle().normalize();
        Vec3 side = new Vec3(-d.z, 0, d.x);
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = d.cross(side).normalize();
        final Vec3 fSide = side;
        Set<LivingEntity> done = new HashSet<>();
        for (int i = 1; i <= 20; i++) {
            final int k = i;
            Scheduler.later(i, () -> {
                Vec3 pos = start.add(d.scale(k * 1.2));
                for (int s = 0; s < 3; s++) {
                    double a = k * 0.8 + s * (Math.PI * 2 / 3);
                    Vec3 o = pos.add(fSide.scale(Math.cos(a) * 1.4)).add(up.scale(Math.sin(a) * 1.4));
                    Fx.spread(l, ParticleTypes.FLAME, o, 3, 0.15, 0.03);
                    Fx.p(l, Fx.dust(0xFF5A00, 2.0f), o);
                }
                Fx.spread(l, ParticleTypes.FLAME, pos, 14, 0.6, 0.05);
                Fx.spread(l, ParticleTypes.LARGE_SMOKE, pos, 3, 0.5, 0.01);
                Fx.spread(l, ParticleTypes.LAVA, pos, 1, 0.6, 0);
                for (LivingEntity t : Abilities.near(p, pos, 2.6)) {
                    if (done.add(t)) {
                        t.setSecondsOnFire(8);
                        Abilities.hit(p, t, 8f, 1.0, d);
                    }
                }
                igniteGround(p, l, pos, 1, 2);
                if (k % 4 == 0) l.playSound(null, BlockPos.containing(pos), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.2f, 0.6f);
            });
        }
        Network.shake(l, p.position(), 0.8f, 20, 0xFF6A00, 0.12f, 20);
    }
}
