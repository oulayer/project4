package com.grandline;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, GrandLine.MODID);

    public static final RegistryObject<EntityType<FireFistEntity>> FIRE_FIST = ENTITIES.register("fire_fist",
            () -> EntityType.Builder.<FireFistEntity>of(FireFistEntity::new, MobCategory.MISC)
                    .sized(1.4f, 1.4f).clientTrackingRange(10).updateInterval(1).fireImmune().build("fire_fist"));

    private ModEntities() {}
}
