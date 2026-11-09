package com.grandline;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Quarta habilidade (tecla R) da Borracha, do Gelo e do Terremoto. */
public final class ExtraMoves {
    private ExtraMoves() {}

    public static void use(ServerPlayer p, ServerLevel l, Fruit f) {
        switch (f) {
            case RUBBER -> bazooka(p, l);
            case ICE -> partisan(p, l);
            case QUAKE -> seismic(p, l);
            default -> { }
        }
    }

    /** Gear Terceiro: o punho infla e depois acerta como um bazuca gigante. */
    private static void bazooka(ServerPlayer p, ServerLevel l) {
        Vec3 d = p.getLookAngle().normalize();
        Abilities.Ray r = Abilities.ray(p, 9);
        Vec3 end = r.end();
        var pink = Fx.dust(0xE8456B, 1.5f);
        l.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1f, 1.6f);
        for (int i = 0; i < 8; i++) {
            final int k = i;
            Scheduler.later(i, () -> {
                if (!p.isAlive()) return;
                Vec3 hand = Abilities.handPos(p).add(p.getLookAngle().scale(0.6));
                Fx.sphere(l, pink, hand, 0.3 + k * 0.12, 14 + k * 3);
                Fx.spread(l, ParticleTypes.CLOUD, hand, 3, 0.3, 0.02);
            });
        }
        Scheduler.later(9, () -> {
            if (!p.isAlive()) return;
            Vec3 hand = Abilities.handPos(p);
            Fx.line(l, pink, hand, end, 0.3);
            Fx.sphere(l, pink, end, 1.7, 70);
            Fx.sphere(l, ParticleTypes.CLOUD, end, 1.9, 30);
            Fx.spread(l, ParticleTypes.CRIT, end, 30, 0.8, 0.5);
            Fx.p(l, ParticleTypes.EXPLOSION, end);
            for (LivingEntity t : Abilities.near(p, end, 3.2)) Abilities.hit(p, t, 16f, 3.2, d);
            l.playSound(null, BlockPos.containing(end), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5f, 1.3f);
            Network.shake(l, end, 2.0f, 14, 0xFFFFFF, 0.1f, 24);
            p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        });
    }

    /** Partisan: lanças de gelo brotam do chão em linha. */
    private static void partisan(ServerPlayer p, ServerLevel l) {
        Vec3 d = p.getLookAngle();
        Vec3 flat = new Vec3(d.x, 0, d.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        final Vec3 dir = flat;
        Vec3 start = p.position();
        RandomSource rnd = p.getRandom();
        Set<LivingEntity> done = new HashSet<>();
        l.playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5f, 0.5f);
        for (int i = 1; i <= 12; i++) {
            final int k = i;
            Scheduler.later(i, () -> {
                Vec3 pos = start.add(dir.scale(k * 1.4));
                for (int s = -1; s <= 1; s++) {
                    Vec3 base = pos.add(-dir.z * s * 0.9, 0, dir.x * s * 0.9);
                    double h = 2.2 + rnd.nextDouble() * 1.8;
                    Fx.line(l, Fx.dust(0xBFF6FF, 1.6f), base, base.add(0, h, 0), 0.25);
                    Fx.line(l, ParticleTypes.SNOWFLAKE, base, base.add(0, h, 0), 0.5);
                    Fx.p(l, ParticleTypes.END_ROD, base.add(0, h, 0));
                }
                Fx.spread(l, ParticleTypes.SNOWFLAKE, pos.add(0, 1, 0), 10, 0.6, 0.1);
                for (LivingEntity t : Abilities.near(p, pos.add(0, 1, 0), 2.0)) {
                    if (done.add(t)) {
                        Abilities.hit(p, t, 8f, 0.5, dir);
                        t.setTicksFrozen(t.getTicksRequiredToFreeze() + 160);
                        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
                        t.setDeltaMovement(t.getDeltaMovement().add(0, 0.9, 0));
                        t.hurtMarked = true;
                    }
                }
                if (k % 3 == 0) l.playSound(null, BlockPos.containing(pos), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.2f, 0.6f);
            });
        }
        Network.shake(l, start, 0.8f, 14, 0x9FEFFF, 0.1f, 20);
    }

    /** Ruptura Sismica: tres ondas de choque seguidas, cada vez maiores. */
    private static void seismic(ServerPlayer p, ServerLevel l) {
        l.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2f, 0.5f);
        for (int i = 0; i < 3; i++) {
            final int k = i;
            Scheduler.later(i * 7, () -> {
                if (!p.isAlive()) return;
                Abilities.shock(p, l, p.position(), 5.0 + k * 3.0, 8f + k * 4f);
                p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            });
        }
        Scheduler.later(20, () -> Network.shake(l, p.position(), 4.5f, 30, 0xFFFFFF, 0.5f, 44));
    }
}
