package com.grandline;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(GrandLine.MODID)
public class GrandLine {
    public static final String MODID = "grandline";

    public GrandLine() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        ModEntities.ENTITIES.register(bus);
        bus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(Network::register));
    }
}
