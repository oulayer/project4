package com.grandline;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GrandLine.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GrandLine.MODID);

    public static final Map<Fruit, RegistryObject<Item>> FRUITS = new EnumMap<>(Fruit.class);

    static {
        for (Fruit f : Fruit.values()) {
            FRUITS.put(f, ITEMS.register(f.id + "_fruit", () -> new DevilFruitItem(f)));
        }
    }

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.grandline"))
                    .icon(() -> new ItemStack(FRUITS.get(Fruit.RUBBER).get()))
                    .displayItems((params, out) -> FRUITS.values().forEach(r -> out.accept(r.get())))
                    .build());

    private ModItems() {}
}
