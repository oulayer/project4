package com.grandline;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

public class DevilFruitItem extends Item {
    private final Fruit fruit;

    public DevilFruitItem(Fruit fruit) {
        super(new Item.Properties().stacksTo(1));
        this.fruit = fruit;
    }

    @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.EAT; }
    @Override public int getUseDuration(ItemStack s) { return 48; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer p) {
            if (PlayerData.getFruit(p) != null) {
                p.displayClientMessage(Component.translatable("msg.grandline.rejected"), false);
                p.hurt(p.damageSources().magic(), 1000f);
            } else {
                PlayerData.setFruit(p, fruit);
                p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
                p.displayClientMessage(Component.translatable("msg.grandline.ate", fruit.displayName()), false);
                Network.sendSync(p);
            }
            if (!p.getAbilities().instabuild) stack.shrink(1);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.grandline.fruit").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.grandline.fruit2").withStyle(ChatFormatting.DARK_RED));
        for (int i = 0; i < 4; i++) {
            tip.add(Component.literal("• ").append(fruit.abilityName(i)).withStyle(ChatFormatting.AQUA));
        }
    }
}
