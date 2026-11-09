package com.grandline;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class ClientEvents {
    public static final KeyMapping[] KEYS = new KeyMapping[12];
    private static final Set<Integer> GLOWING = new HashSet<>();

    private ClientEvents() {}

    @Mod.EventBusSubscriber(modid = GrandLine.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            int[] codes = {GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_R,
                    GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B,
                    GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_M,
                    GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_K};
            String[] names = {"ability1", "ability2", "ability3", "ability4", "armament", "observation", "conqueror",
                    "logia", "hardening", "soru", "geppo", "rankyaku"};
            for (int i = 0; i < 12; i++) {
                KEYS[i] = new KeyMapping("key.grandline." + names[i], codes[i], "key.categories.grandline");
                e.register(KEYS[i]);
            }
        }

        @SubscribeEvent
        public static void overlay(RegisterGuiOverlaysEvent e) {
            e.registerAboveAll("grandline_hud", ClientEvents::renderHud);
        }
    }

    @Mod.EventBusSubscriber(modid = GrandLine.MODID, value = Dist.CLIENT)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            ClientState.tick();
            if (mc.player == null || mc.level == null) { GLOWING.clear(); ClientVisuals.clear(); return; }

            if (mc.screen == null) {
                for (int i = 0; i < 12; i++) {
                    if (KEYS[i] != null && KEYS[i].consumeClick()) Network.CHANNEL.sendToServer(new ActionPacket(i));
                }
            }

            // Haki da Observacao: contorno brilhante em todos os seres vivos proximos (so no seu cliente)
            for (int id : GLOWING) {
                Entity en = mc.level.getEntity(id);
                if (en != null) en.setGlowingTag(false);
            }
            GLOWING.clear();
            if (ClientState.obs) {
                for (Entity en : mc.level.entitiesForRendering()) {
                    if (en instanceof LivingEntity && en != mc.player && en.distanceToSqr(mc.player) < 28 * 28) {
                        en.setGlowingTag(true);
                        GLOWING.add(en.getId());
                    }
                }
            }
        }

        /** Estilo da Mera: com a mao vazia, o clique esquerdo vira golpe de fogo (blocos continuam sendo quebrados). */
        @SubscribeEvent
        public static void click(InputEvent.InteractionKeyMappingTriggered e) {
            if (!e.isAttack()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || !"flame".equals(ClientState.fruit) || !mc.player.getMainHandItem().isEmpty()) return;
            if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) return;
            e.setCanceled(true);
            Network.CHANNEL.sendToServer(new ActionPacket(12));
        }

        @SubscribeEvent
        public static void camera(ViewportEvent.ComputeCameraAngles e) {
            if (ClientState.shakeTicks <= 0) return;
            float amp = ClientState.shakeIntensity * ((float) ClientState.shakeTicks / ClientState.shakeMax);
            double t = (ClientState.shakeMax - ClientState.shakeTicks) * 1.9;
            e.setPitch(e.getPitch() + (float) Math.sin(t * 1.3) * amp);
            e.setYaw(e.getYaw() + (float) Math.cos(t * 1.1) * amp * 0.8f);
            e.setRoll(e.getRoll() + (float) Math.sin(t * 0.9) * amp * 1.4f);
        }
    }

    // ---------------------------------------------------------------- HUD

    private static void renderHud(ForgeGui gui, GuiGraphics g, float partial, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        float time = (mc.player.tickCount + partial) * 0.15f;

        // Bordas do Haki do Armamento (escuro pulsante) e da Observacao (ciano)
        if (ClientState.arm) {
            int a = 0x50 + (int) (Math.sin(time) * 0x18);
            int c = (a << 24);
            g.fillGradient(0, 0, w, h / 5, c, 0x00000000);
            g.fillGradient(0, h - h / 5, w, h, 0x00000000, c);
        }
        if (ClientState.obs) {
            int a = 0x38 + (int) (Math.sin(time * 1.3) * 0x10);
            int c = (a << 24) | 0x00BFFF;
            g.fillGradient(0, 0, w, h / 7, c, 0x0000BFFF);
            g.fillGradient(0, h - h / 7, w, h, 0x0000BFFF, c);
        }
        if (ClientState.logia) {
            boolean fire = "flame".equals(ClientState.fruit);
            int a = 0x40 + (int) (Math.sin(time * 1.7) * 0x14);
            int col = fire ? 0xFF5A00 : 0x9FEFFF;
            int c = (a << 24) | col;
            g.fillGradient(0, h - h / 4, w, h, 0x00000000 | col, c);
            g.fillGradient(0, 0, w / 12, h, c, 0x00000000 | col);
            g.fillGradient(w - w / 12, 0, w, h, 0x00000000 | col, c);
        }
        if (ClientState.hard) {
            int a = 0x30 + (int) (Math.sin(time * 0.8) * 0x10);
            g.fillGradient(0, 0, w, h / 9, (a << 24) | 0x300008, 0x00300008);
            g.fillGradient(0, h - h / 9, w, h, 0x00300008, (a << 24) | 0x300008);
        }
        // Flash do Haki do Rei / impactos
        if (ClientState.flash > 0) {
            int a = (int) (Math.min(1f, ClientState.flash) * 170);
            g.fill(0, 0, w, h, (a << 24) | (ClientState.flashColor & 0xFFFFFF));
        }

        // Contador de combo
        if (ClientState.comboTimer > 0 && ClientState.combo > 0) {
            float k = ClientState.comboTimer / 30f;
            float scale = 1f + 0.6f * k;
            boolean flame = "flame".equals(ClientState.fruit);
            int col = ClientState.combo >= (flame ? 5 : 4) ? 0xFFFF3030 : 0xFFFFD040;
            g.pose().pushPose();
            g.pose().translate(w / 2f + 40, h / 2f + 18, 0);
            g.pose().scale(scale, scale, 1f);
            g.drawString(mc.font, Component.literal("x" + ClientState.combo + " ")
                    .append(Component.translatable((flame ? "combat.grandline.flame" : "combat.grandline.hit") + ClientState.combo)), 0, 0, col, true);
            g.pose().popPose();
        }

        // Painel de habilidades
        int x = w - 140, rowH = 13, y = h - 176;
        Fruit f = Fruit.byId(ClientState.fruit);
        if (f != null) {
            g.drawString(mc.font, f.displayName(), x, y - 12, 0xFF000000 | f.color, true);
            for (int i = 0; i < 4; i++) {
                row(g, mc, x, y + i * rowH, KEYS[i], f.abilityName(i), ClientState.cd[i], ClientState.maxCd(i), 0xFF000000 | f.color, false);
            }
        }
        int hy = y + 4 * rowH + 4;
        row(g, mc, x, hy, KEYS[4], Component.translatable("haki.grandline.armament"), 0, 1, 0xFF8A3CFF, ClientState.arm);
        row(g, mc, x, hy + rowH, KEYS[5], Component.translatable("haki.grandline.observation"), 0, 1, 0xFF4FE8FF, ClientState.obs);
        row(g, mc, x, hy + rowH * 2, KEYS[6], Component.translatable("haki.grandline.conqueror"), ClientState.cd[4], ClientState.maxCd(4), 0xFFD0182A, false);
        row(g, mc, x, hy + rowH * 3, KEYS[7], Component.translatable("haki.grandline.logia"), 0, 1, 0xFFFF7A1A, ClientState.logia);
        row(g, mc, x, hy + rowH * 4, KEYS[8], Component.translatable("haki.grandline.hardening"), 0, 1, 0xFFB0B0C8, ClientState.hard);
        row(g, mc, x, hy + rowH * 5, KEYS[9], Component.translatable("tech.grandline.soru"), ClientState.cd[5], ClientState.maxCd(5), 0xFFFFFFFF, false);
        row(g, mc, x, hy + rowH * 6, KEYS[10], Component.translatable("tech.grandline.geppo"), 0, 1, 0xFFDDEEFF, false);
        row(g, mc, x, hy + rowH * 7, KEYS[11], Component.translatable("tech.grandline.rankyaku"), ClientState.cd[6], ClientState.maxCd(6), 0xFFBFD8FF, false);
    }

    private static void row(GuiGraphics g, Minecraft mc, int x, int y, KeyMapping key, Component name,
                            int remaining, int max, int color, boolean toggled) {
        boolean ready = remaining <= 0;
        String k = key == null ? "?" : key.getTranslatedKeyMessage().getString();
        Component label = Component.literal("[" + k + "] ").append(name);
        if (toggled) label = label.copy().append(" ").append(Component.translatable("hud.grandline.on"));
        g.drawString(mc.font, label, x, y, ready ? 0xFFFFFFFF : 0xFF999999, true);
        g.fill(x, y + 10, x + 120, y + 12, 0x66000000);
        float frac = toggled ? 1f : (max <= 0 ? 1f : 1f - (float) remaining / max);
        g.fill(x, y + 10, x + (int) (120 * frac), y + 12, color);
    }
}
