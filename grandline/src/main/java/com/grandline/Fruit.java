package com.grandline;

import net.minecraft.network.chat.Component;

public enum Fruit {
    RUBBER("rubber", 0xE8456B, new int[]{50, 100, 220, 300}),
    FLAME("flame", 0xFF7A1A, new int[]{40, 70, 150, 320}),
    ICE("ice", 0x8FE3FF, new int[]{50, 200, 260, 240}),
    QUAKE("quake", 0xC8B8FF, new int[]{80, 200, 160, 340});

    public final String id;
    public final int color;
    public final int[] cooldowns;

    Fruit(String id, int color, int[] cooldowns) {
        this.id = id;
        this.color = color;
        this.cooldowns = cooldowns;
    }

    public static Fruit byId(String id) {
        for (Fruit f : values()) if (f.id.equals(id)) return f;
        return null;
    }

    public Component displayName() {
        return Component.translatable("fruit.grandline." + id);
    }

    public Component abilityName(int slot) {
        return Component.translatable("ability.grandline." + id + "." + slot);
    }
}
