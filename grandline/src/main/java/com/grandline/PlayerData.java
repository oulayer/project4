package com.grandline;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class PlayerData {
    public static final String KEY = "grandline";

    private PlayerData() {}

    public static CompoundTag tag(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(KEY, 10)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }

    public static Fruit getFruit(Player p) {
        String s = tag(p).getString("fruit");
        return s.isEmpty() ? null : Fruit.byId(s);
    }

    public static void setFruit(Player p, Fruit f) {
        if (f == null) tag(p).remove("fruit"); else tag(p).putString("fruit", f.id);
    }

    public static boolean isLogia(Player p) { return tag(p).getBoolean("logia"); }
    public static boolean isHard(Player p) { return tag(p).getBoolean("hard"); }
    public static void setLogiaFlag(Player p, boolean v) { tag(p).putBoolean("logia", v); }
    public static void setHardFlag(Player p, boolean v) { tag(p).putBoolean("hard", v); }
    public static boolean isArmament(Player p) { return tag(p).getBoolean("arm"); }
    public static boolean isObservation(Player p) { return tag(p).getBoolean("obs"); }
    public static void setArmamentFlag(Player p, boolean v) { tag(p).putBoolean("arm", v); }
    public static void setObservationFlag(Player p, boolean v) { tag(p).putBoolean("obs", v); }

    public static int remaining(Player p, String key) {
        long end = tag(p).getLong(key);
        return (int) Math.max(0, end - p.level().getGameTime());
    }

    public static void setCooldown(Player p, String key, int ticks) {
        tag(p).putLong(key, p.level().getGameTime() + ticks);
    }

    public static void clearToggles(Player p) {
        tag(p).putBoolean("arm", false);
        tag(p).putBoolean("obs", false);
        tag(p).putBoolean("logia", false);
        tag(p).putBoolean("hard", false);
    }
}
