package com.grandline;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Arrays;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GrandLine.MODID)
public final class ServerEvents {
    private ServerEvents() {}

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        Fruit f = PlayerData.getFruit(p);

        // Maldicao da fruta: agua e fatal
        if (f != null && !p.isCreative() && !p.isSpectator() && p.isInWaterOrBubble()) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 1, false, false, false));
            Vec3 m = p.getDeltaMovement();
            p.setDeltaMovement(m.x * 0.85, Math.min(m.y, -0.12), m.z * 0.85);
            p.hurtMarked = true;
        }
        // Passiva do fogo: imune a chamas
        if (f == Fruit.FLAME) {
            p.clearFire();
            p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false, false));
        }

        // Haki: custo de fome e aura
        boolean arm = PlayerData.isArmament(p), obs = PlayerData.isObservation(p);
        boolean logia = PlayerData.isLogia(p), hard = PlayerData.isHard(p);
        Haki.ensureAttr(p);
        Techniques.tickGround(p);
        if (arm || obs || logia || hard) {
            if (p.getFoodData().getFoodLevel() <= 6 && !p.isCreative()) {
                PlayerData.clearToggles(p);
                Haki.ensureAttr(p);
                p.displayClientMessage(Component.translatable("msg.grandline.exhausted"), true);
                Network.sendSync(p);
            } else {
                p.causeFoodExhaustion((arm ? 0.004f : 0f) + (obs ? 0.003f : 0f) + (logia ? 0.003f : 0f) + (hard ? 0.004f : 0f));
            }
            if (arm || obs) Haki.tickAura(p);
            if (PlayerData.isHard(p)) HakiHard.tick(p);
            if (PlayerData.isLogia(p)) Logia.tick(p);
        }

        if (p.tickCount % 4 == 0) Network.sendSync(p);
    }

    @SubscribeEvent
    public static void fall(LivingFallEvent e) {
        if (e.getEntity() instanceof Player p && !p.level().isClientSide && PlayerData.getFruit(p) == Fruit.RUBBER) {
            e.setDamageMultiplier(0f);
        }
    }

    /** Esquiva da Observacao. */
    @SubscribeEvent
    public static void attacked(LivingAttackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !PlayerData.isObservation(p)) return;
        var src = e.getSource();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || p.isBlocking()) return;
        if (src.getEntity() == null && !src.is(DamageTypeTags.IS_PROJECTILE)) return;
        if (PlayerData.remaining(p, "dodge") > 0 || p.getRandom().nextFloat() >= 0.30f) return;
        PlayerData.setCooldown(p, "dodge", 10);
        e.setCanceled(true);
        Haki.dodge(p);
    }

    /** Impacto do Haki do Armamento e efeitos de fruta nos golpes corpo a corpo. */
    @SubscribeEvent
    public static void hurt(LivingHurtEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel l)) return;
        LivingEntity t = e.getEntity();
        Vec3 c = t.position().add(0, t.getBbHeight() / 2, 0);
        if (PlayerData.isArmament(p)) {
            Fx.spread(l, Fx.DARK, c, 22, 0.4, 0);
            Fx.spread(l, Fx.VIOLET, c, 10, 0.4, 0);
            Fx.spread(l, ParticleTypes.ELECTRIC_SPARK, c, 8, 0.4, 0.3);
            Fx.ring(l, Fx.DARK, c, 0.9, 16);
        }
        Fruit f = PlayerData.getFruit(p);
        if (f == Fruit.ICE && e.getSource().getDirectEntity() == p) t.setTicksFrozen(t.getTicksFrozen() + 80);
        if (f == Fruit.FLAME && e.getSource().getDirectEntity() == p) t.setSecondsOnFire(3);
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide) return;
        boolean boss = v instanceof WitherBoss || v instanceof EnderDragon;
        if (boss || (v instanceof Monster && v.getRandom().nextFloat() < 0.015f)) {
            Fruit f = Fruit.values()[v.getRandom().nextInt(Fruit.values().length)];
            e.getDrops().add(new ItemEntity(v.level(), v.getX(), v.getY(), v.getZ(), new ItemStack(ModItems.FRUITS.get(f).get())));
        }
    }

    /** Estilo da Mera: o soco normal (mao vazia) e substituido pelo combo de fogo. */
    @SubscribeEvent
    public static void attackEntity(AttackEntityEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && FireStyle.active(p)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        CompoundTag old = e.getOriginal().getPersistentData().getCompound(PlayerData.KEY);
        e.getEntity().getPersistentData().put(PlayerData.KEY, old.copy());
        PlayerData.clearToggles(e.getEntity());
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("grandline").requires(s -> s.hasPermission(2))
                .then(Commands.literal("reset").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    PlayerData.setFruit(p, null);
                    PlayerData.clearToggles(p);
                    Network.sendSync(p);
                    c.getSource().sendSuccess(() -> Component.translatable("msg.grandline.reset"), false);
                    return 1;
                }))
                .then(Commands.literal("set").then(Commands.argument("fruit", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Fruit.values()).map(x -> x.id), b))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            Fruit f = Fruit.byId(StringArgumentType.getString(c, "fruit"));
                            if (f == null) return 0;
                            PlayerData.setFruit(p, f);
                            Network.sendSync(p);
                            c.getSource().sendSuccess(() -> Component.translatable("msg.grandline.set", f.displayName()), false);
                            return 1;
                        }))));
    }
}
