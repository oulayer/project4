package com.grandline;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Combate corpo a corpo: sequencia de 4 golpes (jab, direto, gancho, impacto final).
 * Cada golpe seguido dentro da janela de tempo aumenta o combo.
 */
@Mod.EventBusSubscriber(modid = GrandLine.MODID)
public final class Combat {
    /** Marcado como true enquanto uma habilidade causa dano, para nao contar como golpe de combo. */
    public static boolean inAbility = false;

    private static final int WINDOW = 24;

    private Combat() {}

    /** Valor mostrado no HUD (1 a 4), ou 0 se o combo expirou. */
    public static int displayCombo(ServerPlayer p) {
        CompoundTag t = PlayerData.tag(p);
        long last = t.getLong("combolast");
        if (last == 0 || p.level().getGameTime() - last > 30) return 0;
        return t.getInt("combodisp");
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void melee(LivingHurtEvent e) {
        if (inAbility) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        if (e.getSource().getDirectEntity() != p || !e.getSource().is(DamageTypes.PLAYER_ATTACK)) return;
        if (!(p.level() instanceof ServerLevel l)) return;
        if (FireStyle.active(p)) return; // a Mera tem estilo proprio
        if (p.getAttackStrengthScale(0.5f) < 0.6f) return; // spam de clique nao conta

        LivingEntity t = e.getEntity();
        long now = l.getGameTime();
        CompoundTag tag = PlayerData.tag(p);
        int combo = tag.getInt("combo");
        if (now - tag.getLong("combolast") > WINDOW) combo = 0;
        int step = combo % 4;

        Vec3 look = p.getLookAngle();
        Vec3 d = new Vec3(look.x, 0, look.z);
        d = d.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : d.normalize();
        final Vec3 dir = d;
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 c = t.position().add(0, t.getBbHeight() * 0.6, 0);
        Vec3 from = c.subtract(dir.scale(1.2));

        float bonus;
        switch (step) {
            case 0 -> { // jab: corte rapido da direita para a esquerda
                bonus = 1f;
                Fx.arc(l, Fx.dust(0xFFFFFF, 1.0f), from, dir, side, 1.4, -1.0, 1.0, 10);
                Fx.spread(l, ParticleTypes.CRIT, c, 6, 0.25, 0.3);
                l.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, 1.3f);
            }
            case 1 -> { // direto: corte contrario + empurrao
                bonus = 2f;
                Fx.arc(l, Fx.dust(0xFFE08A, 1.2f), from, dir, side, 1.5, 1.0, -1.0, 10);
                Fx.spread(l, ParticleTypes.CRIT, c, 10, 0.3, 0.4);
                Fx.spread(l, ParticleTypes.POOF, c, 4, 0.2, 0.05);
                push(t, dir.scale(0.6), 0.1);
                l.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1f, 1.0f);
            }
            case 2 -> { // gancho: arco vertical subindo, lanca o alvo para cima
                bonus = 3f;
                Fx.arc(l, Fx.dust(0xFFB347, 1.4f), from, dir, up, 1.6, -0.9, 1.1, 12);
                Fx.arc(l, ParticleTypes.CRIT, from, dir, up, 1.8, -0.9, 1.1, 10);
                Fx.spread(l, ParticleTypes.CLOUD, c, 6, 0.3, 0.1);
                push(t, dir.scale(0.25), 0.95);
                l.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.8f);
            }
            default -> { // impacto final: onda de choque + tremor
                bonus = 6f;
                Fx.ring(l, ParticleTypes.CLOUD, c.add(0, -0.5, 0), 1.0, 14);
                Fx.ring(l, Fx.dust(0xFFFFFF, 1.8f), c.add(0, -0.4, 0), 1.6, 20);
                Fx.ring(l, ParticleTypes.CRIT, c, 2.2, 24);
                Fx.spread(l, ParticleTypes.EXPLOSION, c, 2, 0.3, 0);
                Fx.spread(l, ParticleTypes.CRIT, c, 24, 0.5, 0.6);
                push(t, dir.scale(1.9), 0.35);
                l.playSound(null, t.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6f, 1.4f);
                l.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 0.7f);
                Network.shake(l, c, 0.9f, 8, 0xFFFFFF, 0.08f, 16);
            }
        }
        e.setAmount(e.getAmount() + bonus);

        tag.putInt("combo", step == 3 ? 0 : combo + 1);
        tag.putInt("combodisp", step + 1);
        tag.putLong("combolast", now);
        Network.sendSync(p);
    }

    /** Aplica o empurrao um tick depois, porque o knockback vanilla acontece logo apos o dano. */
    private static void push(LivingEntity t, Vec3 horizontal, double up) {
        Scheduler.later(1, () -> {
            if (!t.isAlive()) return;
            t.setDeltaMovement(t.getDeltaMovement().add(horizontal.x, up, horizontal.z));
            t.hurtMarked = true;
        });
    }
}
