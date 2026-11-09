package com.grandline;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Defesas: intangibilidade do Logia e endurecimento do Haki. */
@Mod.EventBusSubscriber(modid = GrandLine.MODID)
public final class Defense {
    private Defense() {}

    /** Logia: ataques fisicos atravessam o corpo, a menos que o atacante use Haki do Armamento. */
    @SubscribeEvent
    public static void logia(LivingAttackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !PlayerData.isLogia(p)) return;
        Fruit f = PlayerData.getFruit(p);
        if (f == null || !Logia.can(f)) return;
        DamageSource src = e.getSource();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        boolean fire = f == Fruit.FLAME;
        if (fire && src.is(DamageTypeTags.IS_FIRE)) { e.setCanceled(true); return; }

        Entity direct = src.getDirectEntity();
        if (direct == null) return;
        boolean physical = direct instanceof LivingEntity || src.is(DamageTypeTags.IS_PROJECTILE);
        if (!physical) return;
        Entity atk = src.getEntity();
        if (atk instanceof Player ap && PlayerData.isArmament(ap)) return; // Haki atinge o Logia

        e.setCanceled(true);
        ServerLevel l = p.serverLevel();
        Vec3 c = p.position().add(0, p.getBbHeight() * 0.6, 0);
        if (fire) {
            Fx.spread(l, ParticleTypes.FLAME, c, 28, 0.5, 0.15);
            Fx.spread(l, Fx.dust(0xFF6A00, 2.0f), c, 10, 0.5, 0);
            Fx.ring(l, ParticleTypes.FLAME, c, 0.9, 16);
            l.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8f, 1.4f);
        } else {
            Fx.spread(l, ParticleTypes.SNOWFLAKE, c, 28, 0.5, 0.15);
            Fx.spread(l, Fx.dust(0xBFF6FF, 1.8f), c, 10, 0.5, 0);
            Fx.ring(l, ParticleTypes.SNOWFLAKE, c, 0.9, 16);
            l.playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.6f);
        }
        if (atk instanceof LivingEntity la && la != p) {
            if (fire) {
                la.setSecondsOnFire(5);
            } else {
                la.setTicksFrozen(la.getTicksFrozen() + 100);
                la.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            }
        }
    }

    /** Endurecimento: reduz o dano recebido e reforca os golpes de quem o usa. */
    @SubscribeEvent
    public static void hardening(LivingHurtEvent e) {
        if (e.getEntity() instanceof ServerPlayer v && PlayerData.isHard(v) && v.level() instanceof ServerLevel l) {
            e.setAmount(e.getAmount() * 0.65f);
            Vec3 c = v.position().add(0, v.getBbHeight() * 0.6, 0);
            Fx.spread(l, ParticleTypes.CRIT, c, 10, 0.4, 0.3);
            Fx.spread(l, ParticleTypes.ELECTRIC_SPARK, c, 6, 0.4, 0.3);
            Fx.spread(l, Fx.DARK, c, 8, 0.4, 0);
            l.playSound(null, v.blockPosition(), SoundEvents.ANVIL_HIT, SoundSource.PLAYERS, 0.5f, 1.8f);
        }
        if (e.getSource().getEntity() instanceof ServerPlayer a && PlayerData.isHard(a)
                && e.getSource().getDirectEntity() == a && !Combat.inAbility && a.level() instanceof ServerLevel l) {
            e.setAmount(e.getAmount() * 1.25f);
            LivingEntity t = e.getEntity();
            Vec3 c = t.position().add(0, t.getBbHeight() / 2, 0);
            Fx.ring(l, Fx.CRIMSON, c, 1.0, 18);
            Fx.spread(l, ParticleTypes.ELECTRIC_SPARK, c, 14, 0.5, 0.4);
            Fx.spread(l, ParticleTypes.END_ROD, c, 6, 0.3, 0.2);
            l.playSound(null, t.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.6f, 1.5f);
        }
    }
}
