package com.grandline;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Haki {
    public static final int CONQUEROR_CD = 900;
    private static final UUID ARM_ID = UUID.fromString("5d4c2a10-7a1e-4b8f-9a55-1f0a6b9c1d01");

    private Haki() {}

    // ------------------------------------------------------------- ARMAMENTO

    public static void toggleArmament(ServerPlayer p) {
        boolean on = !PlayerData.isArmament(p);
        if (on && p.getFoodData().getFoodLevel() <= 6 && !p.isCreative()) {
            p.displayClientMessage(Component.translatable("msg.grandline.exhausted"), true);
            return;
        }
        setArmament(p, on);
    }

    public static void setArmament(ServerPlayer p, boolean on) {
        PlayerData.setArmamentFlag(p, on);
        if (!on) PlayerData.setHardFlag(p, false);
        ensureAttr(p);
        p.displayClientMessage(Component.translatable(on ? "msg.grandline.arm_on" : "msg.grandline.arm_off"), true);
        if (on) armamentIntro(p);
        Network.sendSync(p);
    }

    public static void ensureAttr(ServerPlayer p) {
        AttributeInstance a = p.getAttribute(Attributes.ATTACK_DAMAGE);
        if (a == null) return;
        boolean has = a.getModifier(ARM_ID) != null;
        boolean want = PlayerData.isArmament(p);
        if (want && !has) a.addTransientModifier(new AttributeModifier(ARM_ID, "haki_armament", 4.0, AttributeModifier.Operation.ADDITION));
        if (!want && has) a.removeModifier(ARM_ID);
    }

    /** Helice de particulas negras subindo ao redor do corpo + anel de choque. */
    private static void armamentIntro(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        l.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 0.5f);
        for (int i = 0; i < 26; i++) {
            final int k = i;
            Scheduler.later(i, () -> {
                if (!p.isAlive()) return;
                Vec3 base = p.position();
                for (int s = 0; s < 3; s++) {
                    double a = k * 0.55 + s * (Math.PI * 2 / 3);
                    double rad = 1.0 - k * 0.012;
                    Vec3 pos = base.add(Math.cos(a) * rad, k * 0.08, Math.sin(a) * rad);
                    Fx.p(l, Fx.DARK, pos);
                    Fx.p(l, Fx.VIOLET, pos.add(0, 0.05, 0));
                    if (k % 4 == 0) Fx.p(l, ParticleTypes.ELECTRIC_SPARK, pos);
                }
            });
        }
        Scheduler.later(26, () -> {
            Vec3 c = p.position();
            for (int r = 1; r <= 4; r++) Fx.ring(l, Fx.DARK, c.add(0, 0.1 + r * 0.05, 0), r * 0.7, 28);
            Fx.spread(l, ParticleTypes.LARGE_SMOKE, c.add(0, 1, 0), 18, 0.5, 0.05);
            Network.shake(l, c, 0.4f, 8, 0x000000, 0.0f, 12);
        });
    }

    /** Aura continua: bracos enegrecidos com faiscas roxas. */
    static void tickAura(ServerPlayer p) {
        if (p.tickCount % 2 != 0) return;
        ServerLevel l = p.serverLevel();
        if (PlayerData.isArmament(p)) {
            float yaw = p.yBodyRot * Mth.DEG_TO_RAD;
            Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
            Vec3 body = p.position().add(0, 1.05, 0);
            for (int side = -1; side <= 1; side += 2) {
                Vec3 arm = body.add(right.scale(0.38 * side));
                Fx.spread(l, Fx.DARK, arm, 2, 0.14, 0);
                Fx.spread(l, Fx.DARK, arm.add(0, -0.35, 0), 1, 0.12, 0);
                if (p.getRandom().nextFloat() < 0.25f) Fx.spread(l, Fx.VIOLET, arm, 1, 0.18, 0);
            }
            if (p.tickCount % 6 == 0) {
                Fx.spread(l, ParticleTypes.SMOKE, body.add(0, 0.3, 0), 2, 0.3, 0.01);
                Fx.ring(l, Fx.DARK, p.position().add(0, 0.05, 0), 0.7, 10);
            }
        }
        if (PlayerData.isObservation(p)) {
            if (p.tickCount % 4 == 0) Fx.spread(l, Fx.CYAN, p.position().add(0, p.getBbHeight() + 0.3, 0), 1, 0.25, 0);
            if (p.tickCount % 40 == 0) {
                Vec3 c = p.position().add(0, 0.1, 0);
                for (int i = 0; i < 10; i++) {
                    final int r = i + 1;
                    Scheduler.later(i, () -> Fx.ring(l, Fx.dust(0x6FF2FF, 0.8f), c, r * 1.2, Math.min(40, r * 5)));
                }
            }
        }
    }

    // ------------------------------------------------------------- OBSERVACAO

    public static void toggleObservation(ServerPlayer p) {
        boolean on = !PlayerData.isObservation(p);
        if (on && p.getFoodData().getFoodLevel() <= 6 && !p.isCreative()) {
            p.displayClientMessage(Component.translatable("msg.grandline.exhausted"), true);
            return;
        }
        PlayerData.setObservationFlag(p, on);
        p.displayClientMessage(Component.translatable(on ? "msg.grandline.obs_on" : "msg.grandline.obs_off"), true);
        if (on) {
            ServerLevel l = p.serverLevel();
            l.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 2f, 1.4f);
            Vec3 c = p.position().add(0, 0.1, 0);
            for (int i = 0; i < 12; i++) {
                final int r = i + 1;
                Scheduler.later(i, () -> {
                    Fx.ring(l, Fx.CYAN, c, r, Math.min(56, r * 6));
                    Fx.ring(l, ParticleTypes.END_ROD, c.add(0, 0.4, 0), r * 0.9, Math.min(24, r * 3));
                });
            }
        }
        Network.sendSync(p);
    }

    /** Esquiva da Observacao: imagem residual e pulo lateral. */
    public static void dodge(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        Vec3 old = p.position().add(0, 1, 0);
        Fx.spread(l, ParticleTypes.CLOUD, old, 14, 0.3, 0.02);
        Fx.spread(l, Fx.CYAN, old, 18, 0.35, 0);
        Fx.spread(l, ParticleTypes.END_ROD, old, 8, 0.3, 0.05);
        Vec3 look = p.getLookAngle();
        double s = p.getRandom().nextBoolean() ? 1 : -1;
        p.setDeltaMovement(-look.z * 0.8 * s, 0.2, look.x * 0.8 * s);
        p.hurtMarked = true;
        l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.6f);
    }

    // ------------------------------------------------------------------- REI

    public static void conqueror(ServerPlayer p) {
        int rem = PlayerData.remaining(p, "cd4");
        if (rem > 0) {
            p.displayClientMessage(Component.translatable("msg.grandline.cooldown", rem / 20 + 1), true);
            return;
        }
        PlayerData.setCooldown(p, "cd4", CONQUEROR_CD);
        ServerLevel l = p.serverLevel();
        Vec3 c = p.position();
        l.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 4f, 0.5f);
        l.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5f, 0.5f);
        Network.shake(l, c, 3.2f, 40, 0xB3101F, 1.0f, 48);

        for (int i = 0; i < 24; i++) {
            final int k = i;
            Scheduler.later(i, () -> {
                double r = 1 + k * 0.9;
                int n = Math.min(52, 16 + k * 2);
                Fx.ring(l, Fx.CRIMSON, c.add(0, 0.15, 0), r, n);
                Fx.ring(l, Fx.DARK, c.add(0, 0.6, 0), r * 0.92, n);
                Fx.ring(l, Fx.CRIMSON, c.add(0, 1.4 + k * 0.03, 0), r * 0.8, n / 2);
                if (k % 2 == 0) Fx.ring(l, ParticleTypes.ELECTRIC_SPARK, c.add(0, 0.9, 0), r, n / 2);
                Fx.spread(l, ParticleTypes.LARGE_SMOKE, c.add(0, 0.5, 0), 4, 0.4 + k * 0.1, 0.02);
            });
        }
        Scheduler.later(4, () -> { // raios cenograficos
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8 + p.getRandom().nextDouble() * 0.4;
                double rr = 7 + p.getRandom().nextDouble() * 5;
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(l);
                if (bolt != null) {
                    bolt.moveTo(c.x + Math.cos(a) * rr, c.y, c.z + Math.sin(a) * rr);
                    bolt.setVisualOnly(true);
                    l.addFreshEntity(bolt);
                }
            }
        });
        Scheduler.later(2, () -> {
            for (LivingEntity t : l.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(24), e -> e != p && e.isAlive())) {
                if (t instanceof Player) {
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));
                    t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
                    t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
                    continue;
                }
                if (t.getMaxHealth() > 60f) {
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    continue;
                }
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 6));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));
                Fx.spread(l, ParticleTypes.SOUL, t.position().add(0, t.getBbHeight(), 0), 6, 0.3, 0.03);
                if (t instanceof Mob m) {
                    m.setTarget(null);
                    boolean was = m.isNoAi();
                    m.setNoAi(true);
                    Scheduler.later(160, () -> { if (m.isAlive() && !was) m.setNoAi(false); });
                }
            }
        });
        Network.sendSync(p);
    }
}
