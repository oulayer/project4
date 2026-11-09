package com.grandline;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Estilo de combate da Mera Mera: substitui o soco normal (mao vazia) por um combo de 5 golpes de fogo.
 * Sem tempo de recarga do Minecraft: cada golpe tem ritmo proprio, avanca ate o alvo e o gruda no combo.
 */
public final class FireStyle {
    private static final int WINDOW = 26;                       // ticks para continuar o combo
    private static final int[] DELAY = {7, 7, 9, 9, 18};          // ticks ate o proximo golpe
    private static final float[] DAMAGE = {5f, 6f, 7f, 8f, 0f};
    private static final double[] REACH = {4.2, 4.2, 3.8, 4.4, 0};
    private static final double[] KNOCK = {0.5, 0.7, 1.4, 0.4, 0};
    private static final int[] FIRE = {3, 3, 4, 5, 0};

    private FireStyle() {}

    public static boolean active(ServerPlayer p) {
        return PlayerData.getFruit(p) == Fruit.FLAME && p.getMainHandItem().isEmpty();
    }

    public static void attack(ServerPlayer p) {
        if (!active(p)) return;
        ServerLevel l = p.serverLevel();
        long now = l.getGameTime();
        CompoundTag tag = PlayerData.tag(p);
        if (now < tag.getLong("fsnext")) return;

        // ataque aereo olhando para baixo: mergulho de fogo
        if (!p.onGround() && p.getXRot() > 40f) {
            tag.putLong("fsnext", now + 30);
            dive(p, l);
            return;
        }

        int step = (now - tag.getLong("fslast") > WINDOW) ? 0 : tag.getInt("fsstep");
        step = Math.min(step, 4);
        tag.putLong("fslast", now);
        tag.putInt("fsstep", step == 4 ? 0 : step + 1);
        tag.putLong("fsnext", now + DELAY[step]);
        tag.putInt("combodisp", step + 1);
        tag.putLong("combolast", now);

        // mira automatica: vira para o alvo mais proximo na frente e avanca
        Vec3 look = p.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
        LivingEntity auto = nearest(p, dir, 5.5);
        if (auto != null) {
            Vec3 to = auto.position().subtract(p.position());
            Vec3 flat = new Vec3(to.x, 0, to.z);
            if (flat.lengthSqr() > 1.0E-4) dir = flat.normalize();
        }
        final Vec3 fdir = dir;
        double lunge = step == 4 ? 0.35 : 0.85;
        if (auto == null || auto.distanceTo(p) > 2.0) {
            p.setDeltaMovement(fdir.x * lunge, p.onGround() ? 0.1 : 0.0, fdir.z * lunge);
            p.hurtMarked = true;
        }

        Network.sendVisual(p, step + 1);
        p.swing(InteractionHand.MAIN_HAND, true);
        final int fs = step;
        Scheduler.later(2, () -> {
            if (!p.isAlive()) return;
            strike(p, l, fs, fdir);
        });
        Network.sendSync(p);
    }

    private static LivingEntity nearest(ServerPlayer p, Vec3 dir, double range) {
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : Abilities.near(p, p.position(), range)) {
            Vec3 to = e.position().subtract(p.position());
            Vec3 f = new Vec3(to.x, 0, to.z);
            if (f.lengthSqr() < 1.0E-4) continue;
            if (f.normalize().dot(dir) < 0.5) continue;
            double d = e.distanceToSqr(p);
            if (d < bd) { bd = d; best = e; }
        }
        return best;
    }

    private static List<LivingEntity> cone(ServerPlayer p, Vec3 dir, double reach, boolean all) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : Abilities.near(p, p.position().add(0, 1, 0), reach)) {
            Vec3 to = e.position().subtract(p.position());
            Vec3 f = new Vec3(to.x, 0, to.z);
            if (all || f.lengthSqr() < 3.0 || f.normalize().dot(dir) > 0.25) out.add(e);
        }
        return out;
    }

    private static void strike(ServerPlayer p, ServerLevel l, int step, Vec3 dir) {
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 body = p.position().add(0, 1.1, 0);
        Vec3 from = body.add(dir.scale(0.3));

        if (step == 4) { // final: Hiken Enjo, punho de fogo gigante
            Vec3 start = p.getEyePosition().add(dir.scale(1.5)).add(0, -0.45, 0);
            FireFistEntity.spawn(p, l, start, p.getLookAngle(), true, 1.7f);
            Fx.fireBurst(l, from.add(dir.scale(1.2)), 1.6, 60);
            l.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2f, 0.4f);
            l.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.5f);
            Network.shake(l, p.position(), 1.6f, 12, 0xFF6A00, 0.2f, 24);
            return;
        }

        // efeitos visuais de cada golpe
        switch (step) {
            case 0 -> {
                Fx.fireArc(l, from, dir, side, 2.4, -1.1, 1.1, 14);
                l.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1f, 1.6f);
            }
            case 1 -> {
                Fx.fireArc(l, from.add(0, 0.2, 0), dir, side, 2.6, 1.1, -1.1, 14);
                l.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1f, 1.3f);
            }
            case 2 -> { // roda de chamas: aneis expansivos ao redor
                for (int i = 0; i < 4; i++) {
                    final int r = i + 1;
                    Scheduler.later(i, () -> {
                        Vec3 c = p.position().add(0, 0.5, 0);
                        Fx.ring(l, ParticleTypes.FLAME, c, r * 0.9, r * 12);
                        Fx.ring(l, Fx.dust(0xFFB347, 1.8f), c.add(0, 0.3, 0), r * 0.9, r * 8);
                        Fx.ring(l, ParticleTypes.LARGE_SMOKE, c.add(0, 0.8, 0), r * 0.8, r * 4);
                    });
                }
                l.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2f, 0.9f);
            }
            default -> { // gancho: arco vertical subindo + pilar
                Fx.fireArc(l, from, dir, up, 2.6, -0.9, 1.2, 16);
                Fx.line(l, ParticleTypes.FLAME, from.add(dir.scale(1.5)), from.add(dir.scale(1.5)).add(0, 4.5, 0), 0.3);
                l.playSound(null, p.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1f, 1.2f);
            }
        }

        // dano
        boolean wheel = step == 2;
        double reach = wheel ? 3.8 : REACH[step];
        int hits = 0;
        for (LivingEntity t : cone(p, dir, reach, wheel)) {
            Vec3 push = wheel ? t.position().subtract(p.position()).multiply(1, 0, 1) : dir;
            push = push.lengthSqr() < 1.0E-4 ? dir : push.normalize();
            t.invulnerableTime = 0;
            t.setSecondsOnFire(FIRE[step]);
            Abilities.hit(p, t, DAMAGE[step], KNOCK[step], push);
            Vec3 c = t.position().add(0, t.getBbHeight() * 0.6, 0);
            Fx.fireBurst(l, c, 0.8, 24);
            Fx.spread(l, ParticleTypes.CRIT, c, 8, 0.3, 0.4);
            if (step == 3) { // gancho lanca para o alto
                final LivingEntity target = t;
                Scheduler.later(1, () -> {
                    if (!target.isAlive()) return;
                    target.setDeltaMovement(dir.x * 0.2, 1.0, dir.z * 0.2);
                    target.hurtMarked = true;
                });
            }
            hits++;
        }
        if (hits > 0) {
            l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, 0.8f + step * 0.1f);
            Network.shake(l, p.position(), 0.35f + step * 0.12f, 6, 0xFF6A00, 0.05f, 14);
        }
    }

    /** Mergulho de fogo: cai como um meteoro e explode ao tocar o chao. */
    private static void dive(ServerPlayer p, ServerLevel l) {
        Vec3 look = p.getLookAngle();
        p.setDeltaMovement(look.x * 0.5, -2.2, look.z * 0.5);
        p.hurtMarked = true;
        p.fallDistance = 0;
        Network.sendVisual(p, 8);
        l.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5f, 0.5f);
        new Runnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!p.isAlive()) return;
                Vec3 c = p.position().add(0, 0.5, 0);
                Fx.fireBurst(l, c, 0.9, 18);
                Fx.line(l, ParticleTypes.FLAME, c, c.add(0, 4, 0), 0.4);
                p.fallDistance = 0;
                if (p.onGround() || ticks > 25) {
                    if (p.onGround()) {
                        FlameMoves.explode(p, l, p.position(), true);
                        for (int r = 1; r <= 6; r++) Fx.ring(l, ParticleTypes.FLAME, p.position().add(0, 0.2, 0), r, r * 9);
                    }
                    return;
                }
                ticks++;
                Scheduler.later(1, this);
            }
        }.run();
    }
}
