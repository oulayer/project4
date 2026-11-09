package com.grandline;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class Abilities {
    private Abilities() {}

    record Ray(Vec3 end, LivingEntity target) {}

    public static void use(ServerPlayer p, int slot) {
        Fruit f = PlayerData.getFruit(p);
        if (f == null) {
            p.displayClientMessage(Component.translatable("msg.grandline.no_fruit"), true);
            return;
        }
        int rem = PlayerData.remaining(p, "cd" + slot);
        if (rem > 0) {
            p.displayClientMessage(Component.translatable("msg.grandline.cooldown", rem / 20 + 1), true);
            return;
        }
        ServerLevel l = p.serverLevel();
        if (slot == 3 && f != Fruit.FLAME) {
            ExtraMoves.use(p, l, f);
        } else {
            switch (f) {
                case RUBBER -> rubber(p, l, slot);
                case FLAME -> FlameMoves.use(p, l, slot);
                case ICE -> ice(p, l, slot);
                case QUAKE -> quake(p, l, slot);
            }
        }
        PlayerData.setCooldown(p, "cd" + slot, f.cooldowns[slot]);
        p.swing(InteractionHand.MAIN_HAND, true);
        Network.sendSync(p);
    }

    // ---------------------------------------------------------------- helpers

    static Vec3 handPos(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        return p.getEyePosition().add(right.scale(0.4)).add(0, -0.3, 0).add(look.scale(0.5));
    }

    static Ray ray(ServerPlayer p, double range) { return ray(p, p.getLookAngle(), range); }

    static Ray ray(ServerPlayer p, Vec3 look, double range) {
        ServerLevel l = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult bh = l.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double maxD = bh.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? range : bh.getLocation().distanceTo(eye);
        EntityHitResult er = ProjectileUtil.getEntityHitResult(l, p, eye, end,
                p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0),
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator());
        if (er != null && er.getLocation().distanceTo(eye) <= maxD + 0.5 && er.getEntity() instanceof LivingEntity t) {
            return new Ray(er.getLocation(), t);
        }
        Vec3 stop = bh.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? end : bh.getLocation();
        return new Ray(stop, null);
    }

    static void hit(ServerPlayer p, LivingEntity t, float dmg, double kb, Vec3 dir) {
        float mult = PlayerData.isHard(p) ? 1.7f : PlayerData.isArmament(p) ? 1.4f : 1f;
        Combat.inAbility = true;
        try { t.hurt(p.damageSources().playerAttack(p), dmg * mult); } finally { Combat.inAbility = false; }
        t.setDeltaMovement(t.getDeltaMovement().add(dir.x * kb, 0.25 + kb * 0.1, dir.z * kb));
        t.hurtMarked = true;
    }

    static List<LivingEntity> near(ServerPlayer p, Vec3 c, double r) {
        return p.level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> e != p && e.isAlive() && e.distanceToSqr(c) <= r * r);
    }

    // ----------------------------------------------------------------- BORRACHA

    private static void rubber(ServerPlayer p, ServerLevel l, int slot) {
        Vec3 dir = p.getLookAngle();
        switch (slot) {
            case 0 -> { // Pistola: braco estica e volta
                Ray r = ray(p, 14);
                Vec3 hand = handPos(p);
                Vec3 end = r.end();
                var pink = Fx.dust(0xE8456B, 1.3f);
                for (int i = 1; i <= 5; i++) {
                    final int k = i;
                    Scheduler.later(i - 1, () -> {
                        Vec3 tip = hand.add(end.subtract(hand).scale(k / 5.0));
                        Fx.line(l, pink, hand, tip, 0.2);
                        Fx.spread(l, ParticleTypes.CLOUD, tip, 3, 0.1, 0.01);
                    });
                }
                Scheduler.later(5, () -> {
                    if (r.target() != null) {
                        hit(p, r.target(), 9f, 1.7, dir);
                        Fx.spread(l, ParticleTypes.CRIT, end, 16, 0.3, 0.4);
                        Fx.spread(l, ParticleTypes.POOF, end, 8, 0.2, 0.05);
                    }
                    l.playSound(null, BlockPos.containing(end), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, 0.7f);
                });
                for (int i = 1; i <= 4; i++) {
                    final int k = i;
                    Scheduler.later(5 + i, () -> {
                        Vec3 tip = hand.add(end.subtract(hand).scale(1.0 - k / 5.0));
                        Fx.line(l, pink, hand, tip, 0.25);
                    });
                }
            }
            case 1 -> { // Foguete: dash com dano
                Vec3 d = dir.normalize();
                p.setDeltaMovement(d.scale(2.2).add(0, 0.2, 0));
                p.hurtMarked = true;
                p.fallDistance = 0;
                Set<LivingEntity> done = new HashSet<>();
                for (int i = 0; i < 10; i++) {
                    Scheduler.later(i * 2, () -> {
                        if (!p.isAlive()) return;
                        Vec3 c = p.position().add(0, 1, 0);
                        Fx.spread(l, ParticleTypes.CLOUD, c, 6, 0.3, 0.02);
                        Fx.spread(l, Fx.dust(0xE8456B, 1.5f), c, 6, 0.4, 0);
                        for (LivingEntity t : near(p, c, 2.5)) if (done.add(t)) hit(p, t, 6f, 1.4, d);
                    });
                }
                l.playSound(null, p.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1f, 1.2f);
            }
            default -> { // Metralhadora: rajada de socos
                RandomSource rnd = p.getRandom();
                for (int i = 0; i < 14; i++) {
                    Scheduler.later(i * 2 + 1, () -> {
                        if (!p.isAlive()) return;
                        Vec3 look = p.getLookAngle();
                        Vec3 hand = handPos(p);
                        Vec3 fist = p.getEyePosition().add(look.scale(2.8)).add(rnd.nextGaussian() * 0.5, rnd.nextGaussian() * 0.5, rnd.nextGaussian() * 0.5);
                        Fx.line(l, Fx.dust(0xE8456B, 1.0f), hand, fist, 0.25);
                        Fx.spread(l, ParticleTypes.POOF, fist, 4, 0.15, 0.03);
                        Ray r = ray(p, 5);
                        if (r.target() != null) {
                            r.target().invulnerableTime = 0;
                            hit(p, r.target(), 3f, 0.25, look);
                            Fx.spread(l, ParticleTypes.CRIT, r.end(), 6, 0.2, 0.3);
                        }
                        l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.7f, 1.3f);
                        p.swing(InteractionHand.MAIN_HAND, true);
                    });
                }
            }
        }
    }

    // -------------------------------------------------------------------- CHAMAS

    private static void flame(ServerPlayer p, ServerLevel l, int slot) {
        Vec3 dir = p.getLookAngle();
        switch (slot) {
            case 0 -> {
                Vec3 pos = p.getEyePosition().add(dir.scale(1.2));
                LargeFireball fb = new LargeFireball(l, p, dir.x, dir.y, dir.z, 2);
                fb.setPos(pos.x, pos.y - 0.2, pos.z);
                l.addFreshEntity(fb);
                Fx.spread(l, ParticleTypes.FLAME, handPos(p), 20, 0.25, 0.05);
                l.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2f, 0.8f);
            }
            case 1 -> {
                Vec3 c = p.position();
                l.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.5f, 0.6f);
                for (int i = 0; i < 7; i++) {
                    final int r = i + 1;
                    Scheduler.later(i * 2, () -> {
                        int n = Math.min(48, r * 10);
                        Fx.ring(l, ParticleTypes.FLAME, c.add(0, 0.3, 0), r, n);
                        Fx.ring(l, ParticleTypes.LARGE_SMOKE, c.add(0, 0.6, 0), r * 0.85, n / 2);
                        Fx.spread(l, ParticleTypes.LAVA, c.add(0, 1, 0), 4, r * 0.5, 0);
                    });
                }
                Scheduler.later(5, () -> {
                    for (LivingEntity t : near(p, c, 7.5)) {
                        t.setSecondsOnFire(6);
                        hit(p, t, 7f, 1.3, t.position().subtract(c).normalize());
                    }
                    Network.shake(l, c, 1.2f, 12, 0xFF6A00, 0.2f, 24);
                });
            }
            default -> { // linha de fogo progressiva
                Vec3 start = p.position().add(0, 1.0, 0);
                Vec3 d = dir.normalize();
                Set<LivingEntity> done = new HashSet<>();
                for (int i = 1; i <= 18; i++) {
                    final int k = i;
                    Scheduler.later(i, () -> {
                        Vec3 pos = start.add(d.scale(k));
                        Fx.spread(l, ParticleTypes.FLAME, pos, 14, 0.6, 0.04);
                        Fx.spread(l, ParticleTypes.LARGE_SMOKE, pos, 3, 0.5, 0.01);
                        Fx.spread(l, Fx.dust(0xFF5A00, 2.2f), pos, 5, 0.5, 0);
                        for (LivingEntity t : near(p, pos, 2.2)) {
                            if (done.add(t)) { t.setSecondsOnFire(7); hit(p, t, 6f, 0.9, d); }
                        }
                        BlockPos bp = BlockPos.containing(pos);
                        if (l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && l.isEmptyBlock(bp)
                                && BaseFireBlock.canBePlacedAt(l, bp, net.minecraft.core.Direction.UP)) {
                            l.setBlockAndUpdate(bp, Blocks.FIRE.defaultBlockState());
                        }
                    });
                }
                l.playSound(null, p.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.5f, 0.6f);
            }
        }
    }

    // --------------------------------------------------------------------- GELO

    private static void ice(ServerPlayer p, ServerLevel l, int slot) {
        Vec3 dir = p.getLookAngle();
        switch (slot) {
            case 0 -> {
                Ray r = ray(p, 22);
                Vec3 hand = handPos(p);
                Fx.line(l, Fx.dust(0x9FEFFF, 1.2f), hand, r.end(), 0.25);
                Fx.line(l, ParticleTypes.SNOWFLAKE, hand, r.end(), 0.5);
                Fx.spread(l, ParticleTypes.SNOWFLAKE, r.end(), 30, 0.5, 0.12);
                if (r.target() != null) {
                    LivingEntity t = r.target();
                    hit(p, t, 7f, 0.6, dir);
                    t.setTicksFrozen(t.getTicksRequiredToFreeze() + 200);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
                }
                l.playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1f, 1.5f);
            }
            case 1 -> {
                Vec3 c = p.position();
                BlockPos base = p.blockPosition().below();
                for (int i = 0; i < 8; i++) {
                    final int r = i + 1;
                    Scheduler.later(i, () -> {
                        Fx.ring(l, ParticleTypes.SNOWFLAKE, c.add(0, 0.3, 0), r, Math.min(48, r * 8));
                        Fx.ring(l, Fx.dust(0xBFF6FF, 1.6f), c.add(0, 0.5, 0), r, Math.min(40, r * 6));
                    });
                }
                Scheduler.later(3, () -> {
                    for (LivingEntity t : near(p, c, 8)) {
                        t.setTicksFrozen(t.getTicksRequiredToFreeze() + 240);
                        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 4));
                        hit(p, t, 4f, 0.3, new Vec3(0, 0, 0));
                    }
                    for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
                        if (dx * dx + dz * dz > 49) continue;
                        for (int dy = -1; dy <= 0; dy++) {
                            BlockPos bp = base.offset(dx, dy, dz);
                            if (l.getFluidState(bp).is(FluidTags.WATER) && l.getFluidState(bp).isSource() && l.getBlockState(bp.above()).isAir()) {
                                l.setBlockAndUpdate(bp, Blocks.FROSTED_ICE.defaultBlockState());
                            }
                        }
                    }
                    Network.shake(l, c, 0.6f, 10, 0x9FEFFF, 0.25f, 20);
                });
                l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 2f, 0.6f);
            }
            default -> { // Muralha 5x4
                Vec3 h = new Vec3(dir.x, 0, dir.z).normalize();
                Vec3 center = p.position().add(h.scale(3));
                Vec3 side = new Vec3(-h.z, 0, h.x);
                List<BlockPos> placed = new java.util.ArrayList<>();
                for (int w = -2; w <= 2; w++) for (int y = 0; y < 4; y++) {
                    BlockPos bp = BlockPos.containing(center.add(side.scale(w)).add(0, y, 0));
                    if (l.isEmptyBlock(bp)) {
                        l.setBlockAndUpdate(bp, Blocks.PACKED_ICE.defaultBlockState());
                        placed.add(bp);
                        Fx.spread(l, ParticleTypes.SNOWFLAKE, Vec3.atCenterOf(bp), 8, 0.4, 0.05);
                    }
                }
                Scheduler.later(240, () -> {
                    for (BlockPos bp : placed) {
                        if (l.getBlockState(bp).is(Blocks.PACKED_ICE)) {
                            Fx.spread(l, ParticleTypes.SNOWFLAKE, Vec3.atCenterOf(bp), 6, 0.4, 0.05);
                            l.setBlockAndUpdate(bp, Blocks.AIR.defaultBlockState());
                        }
                    }
                });
                l.playSound(null, p.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.5f, 0.5f);
            }
        }
    }

    // ----------------------------------------------------------------- TERREMOTO

    private static void quake(ServerPlayer p, ServerLevel l, int slot) {
        Vec3 dir = p.getLookAngle();
        switch (slot) {
            case 0 -> {
                Ray r = ray(p, 12);
                Vec3 c = r.target() != null ? r.end() : (r.end().distanceTo(p.getEyePosition()) < 3 ? r.end() : r.end());
                Fx.line(l, Fx.dust(0xFFFFFF, 1.2f), handPos(p), c, 0.3);
                shock(p, l, c, 5.0, 12f);
            }
            case 1 -> { // pula e cai com impacto
                p.setDeltaMovement(0, 1.3, 0);
                p.hurtMarked = true;
                l.playSound(null, p.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.5f, 0.5f);
                Scheduler.later(12, () -> {
                    p.setDeltaMovement(0, -2.6, 0);
                    p.hurtMarked = true;
                });
                Scheduler.later(18, () -> {
                    p.fallDistance = 0;
                    shock(p, l, p.position(), 8.0, 9f);
                });
            }
            default -> { // rachadura em linha
                Vec3 start = p.getEyePosition().add(0, -0.4, 0);
                Vec3 d = dir.normalize();
                RandomSource rnd = p.getRandom();
                Set<LivingEntity> done = new HashSet<>();
                for (int i = 1; i <= 11; i++) {
                    final int k = i;
                    Scheduler.later(i, () -> {
                        for (int s = 0; s < 2; s++) {
                            double dist = k * 2 + s;
                            Vec3 pos = start.add(d.scale(dist)).add(rnd.nextGaussian() * 0.25, rnd.nextGaussian() * 0.25, rnd.nextGaussian() * 0.25);
                            Fx.spread(l, ParticleTypes.END_ROD, pos, 4, 0.2, 0.15);
                            Fx.spread(l, Fx.dust(0xFFFFFF, 2.4f), pos, 3, 0.3, 0);
                            Fx.spread(l, ParticleTypes.REVERSE_PORTAL, pos, 8, 0.6, 0.2);
                            for (LivingEntity t : near(p, pos, 3.0)) if (done.add(t)) hit(p, t, 11f, 1.8, d);
                        }
                        if (k % 3 == 0) l.playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 0.4f);
                    });
                }
                Network.shake(l, p.position(), 1.6f, 22, 0xFFFFFF, 0.3f, 30);
            }
        }
    }

    /** Onda de choque: aneis expansivos, rachaduras, dano em area e tremor. */
    static void shock(ServerPlayer p, ServerLevel l, Vec3 c, double r, float dmg) {
        l.playSound(null, BlockPos.containing(c), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.6f, 0.6f);
        Fx.p(l, ParticleTypes.EXPLOSION_EMITTER, c);
        BlockState st = l.getBlockState(BlockPos.containing(c).below());
        if (st.isAir()) st = Blocks.STONE.defaultBlockState();
        l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), c.x, c.y + 0.3, c.z, 70, r * 0.3, 0.3, r * 0.3, 0.3);
        for (int i = 0; i < (int) r; i++) {
            final int k = i + 1;
            Scheduler.later(i, () -> {
                int n = Math.min(56, k * 10);
                Fx.ring(l, ParticleTypes.CLOUD, c.add(0, 0.2, 0), k, n);
                Fx.ring(l, Fx.dust(0xFFFFFF, 2f), c.add(0, 0.5, 0), k, n / 2);
                Fx.ring(l, ParticleTypes.ELECTRIC_SPARK, c.add(0, 0.8, 0), k, n / 3);
            });
        }
        for (int a = 0; a < 8; a++) { // rachaduras radiais
            double ang = Math.PI * 2 * a / 8;
            Vec3 e = c.add(Math.cos(ang) * r, 0.1, Math.sin(ang) * r);
            Fx.line(l, ParticleTypes.CRIT, c.add(0, 0.1, 0), e, 0.4);
        }
        for (LivingEntity t : near(p, c, r)) {
            hit(p, t, dmg, 1.6, t.position().subtract(c).normalize());
        }
        if (l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            int R = (int) Math.min(r, 3);
            BlockPos b = BlockPos.containing(c);
            for (int dx = -R; dx <= R; dx++) for (int dy = -R; dy <= R; dy++) for (int dz = -R; dz <= R; dz++) {
                if (dx * dx + dy * dy + dz * dz > R * R) continue;
                BlockPos bp = b.offset(dx, dy, dz);
                BlockState s = l.getBlockState(bp);
                float h = s.getDestroySpeed(l, bp);
                if (!s.isAir() && h >= 0 && h <= 1.0f) l.destroyBlock(bp, false);
            }
        }
        Network.shake(l, c, (float) Math.min(4.5, 1.5 + r * 0.35), 24, 0xFFFFFF, 0.35f, 36);
    }
}
