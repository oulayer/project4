package com.grandline;

import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Registra o renderer do punho de fogo e a camada de fogo nos jogadores. */
@Mod.EventBusSubscriber(modid = GrandLine.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientRender {
    private ClientRender() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.FIRE_FIST.get(), FireFistRenderer::new);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.AddLayers e) {
        for (String skin : new String[]{"default", "slim"}) {
            PlayerRenderer pr = e.getSkin(skin);
            if (pr != null) pr.addLayer(new FireLayer(pr));
        }
    }
}
