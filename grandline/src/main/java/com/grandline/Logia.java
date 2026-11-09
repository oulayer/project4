package com.grandline;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Corpo de Logia: o jogador vira fogo (Mera Mera) ou gelo (Hie Hie). */
public final class Logia {
    private Logia() {}

    public static boolean can(Fruit f) { return f == Fruit.FLAME || f == Fruit.ICE; }

    public static void toggle(ServerPlayer p) {
        Fruit f = PlayerData.getFruit(p);
        if (f == null) {
            p.displayClientMessage(Component.translatable("msg.grandline.no_fruit"), true);
            return;
        }
        if (!can(f)) {
            p.displayClientMessage(Component.translatable("msg.grandline.not_logia"), true);
            return;
        }
        boolean on = !PlayerData.isLogia(p);
        if (on && p.getFoodData().getFoodLevel() <= 6 && !p.isCreative()) {
            p.displayClientMessage(Component.translatable("msg.grandline.exhausted"), true);
            return;
        }
        PlayerData.setLogiaFlag(p, on);
        p.displayClientMessage(Component.translatable(on ? "msg.grandline.logia_on" : "msg.grandline.logia_off"), true);
        if (on) intro(p, f);
        Network.sendSync(p);
    }

    private static void intro(ServerPlayer p, Fruit f) {
        ServerLevel l = p.serverLevel();
        boolean fire = f == Fruit.FLAME;
        l.playSound(null, p.blockPosition(), fire ? SoundEvents.BLAZE_SHOOT : SoundEvents.PLAYER_HURT_FREEZE,
                SoundSource.PLAYERS, 2f, 0.6f);
        ParticleOptions main = fire ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE;
        ParticleOptions glow = fire ? Fx.dust(0xFF6A00, 2.0f) : Fx.dust(0xBFF6FF, 1.8f);
        // helice subindo e envolvendo o corpo
        for (int i = 0; i < 22; i++) {
            final int k = i;
            Scheduler.later(i, () -> {
                if (!p.isAlive()) return;
                Vec3 base = p.position();
                for (int s = 0; s < 3; s++) {
                    double a = k * 0.6 + s * (Math.PI * 2 / 3);
                    Vec3 pos = base.add(Math.cos(a) * 0.8, k * 0.09, Math.sin(a) * 0.8);
                    Fx.spread(l, main, pos, 2, 0.1, 0.03);
                    Fx.p(l, glow, pos);
                }
            });
        }
        Scheduler.later(22, () -> {
            if (!p.isAlive()) return;
            Vec3 c = p.position().add(0, 1, 0);
            Fx.sphere(l, main, c, 1.6, 60);
            Fx.sphere(l, glow, c, 1.2, 24);
            for (int r = 1; r <= 4; r++) Fx.ring(l, main, p.position().add(0, 0.2, 0), r * 0.9, 18 + r * 4);
            Network.shake(l, p.position(), 0.8f, 10, fire ? 0xFF6A00 : 0x9FEFFF, 0.2f, 18);
        });
    }

    /** Chamado todo tick pelo servidor enquanto o Logia esta ligado. */
    static void tick(ServerPlayer p) {
        Fruit f = PlayerData.getFruit(p);
        if (f == null || !can(f)) {
            PlayerData.setLogiaFlag(p, false);
            Network.sendSync(p);
            return;
        }
        if (p.tickCount % 2 != 0) return;
        ServerLevel l = p.serverLevel();
        RandomSource rnd = p.getRandom();
        boolean fire = f == Fruit.FLAME;
        ParticleOptions main = fire ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE;
        ParticleOptions glow = fire ? Fx.dust(0xFF6A00, 1.7f) : Fx.dust(0xBFF6FF, 1.5f);
        Vec3 base = p.position();
        double h = p.getBbHeight();

        // corpo envolto em chamas / cristais
        for (int k = 0; k < 6; k++) {
            double y = rnd.nextDouble() * h;
            double a = rnd.nextDouble() * Math.PI * 2;
            Vec3 pos = base.add(Math.cos(a) * 0.42, y, Math.sin(a) * 0.42);
            Fx.spread(l, main, pos, 1, 0.05, 0.02);
            if (k % 2 == 0) Fx.p(l, glow, pos);
        }
        // helice lenta
        double t = p.tickCount * 0.5;
        Fx.spread(l, main, base.add(Math.cos(t) * 0.7, (t * 0.15) % h, Math.sin(t) * 0.7), 1, 0.05, 0.02);
        // coroa de fogo acima da cabeca
        Fx.spread(l, main, base.add(0, h + 0.1, 0), 3, 0.2, 0.06);
        if (fire && p.tickCount % 6 == 0) Fx.spread(l, ParticleTypes.LARGE_SMOKE, base.add(0, h, 0), 1, 0.2, 0.02);

        // rastro no chao quando se move
        Vec3 mv = p.getDeltaMovement();
        if (mv.x * mv.x + mv.z * mv.z > 0.006) {
            Fx.spread(l, main, base.add(0, 0.1, 0), 4, 0.3, 0.02);
            if (fire) Fx.spread(l, ParticleTypes.SMOKE, base.add(0, 0.2, 0), 1, 0.2, 0.01);
        }

        // contato: queima ou congela quem chega perto
        if (p.tickCount % 10 == 0) {
            for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(1.6), e -> e != p && e.isAlive())) {
                if (fire) {
                    e.setSecondsOnFire(3);
                } else {
                    e.setTicksFrozen(e.getTicksFrozen() + 60);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                }
            }
        }
        // gelo: congela a agua sob os pes
        if (!fire && p.tickCount % 6 == 0) {
            BlockPos b = p.blockPosition().below();
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                BlockPos bp = b.offset(dx, 0, dz);
                if (l.getFluidState(bp).is(FluidTags.WATER) && l.getFluidState(bp).isSource() && l.getBlockState(bp.above()).isAir()) {
                    l.setBlockAndUpdate(bp, Blocks.FROSTED_ICE.defaultBlockState());
                }
            }
        }
    }
}
