package com.grandline;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Network {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GrandLine.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private Network() {}

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, ActionPacket.class, ActionPacket::encode, ActionPacket::decode, ActionPacket::handle);
        CHANNEL.registerMessage(id++, SyncPacket.class, SyncPacket::encode, SyncPacket::decode, SyncPacket::handle);
        CHANNEL.registerMessage(id++, ShakePacket.class, ShakePacket::encode, ShakePacket::decode, ShakePacket::handle);
        CHANNEL.registerMessage(id++, VisualPacket.class, VisualPacket::encode, VisualPacket::decode, VisualPacket::handle);
    }

    public static void sendSync(ServerPlayer p) {
        Fruit f = PlayerData.getFruit(p);
        int[] cd = new int[SyncPacket.CDS];
        for (int i = 0; i < SyncPacket.CDS; i++) cd[i] = PlayerData.remaining(p, "cd" + i);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new SyncPacket(f == null ? "" : f.id, PlayerData.isArmament(p), PlayerData.isObservation(p),
                        PlayerData.isLogia(p), PlayerData.isHard(p), Combat.displayCombo(p), cd));
        sendVisual(p, 0);
    }

    /** Estado visual (corpo de fogo/gelo, endurecimento) e animacao de golpe para quem esta perto. */
    public static void sendVisual(ServerPlayer p, int anim) {
        int flags = 0;
        Fruit f = PlayerData.getFruit(p);
        if (PlayerData.isLogia(p) && f == Fruit.FLAME) flags |= 1;
        if (PlayerData.isLogia(p) && f == Fruit.ICE) flags |= 2;
        if (PlayerData.isHard(p)) flags |= 4;
        if (PlayerData.isArmament(p)) flags |= 8;
        CompoundTag t = PlayerData.tag(p);
        int last = t.getInt("vflags");
        if (anim == 0 && flags == last && (flags == 0 || p.tickCount % 20 != 0)) return;
        t.putInt("vflags", flags);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), new VisualPacket(p.getUUID(), flags, anim));
    }

    public static void shake(ServerLevel l, Vec3 c, float intensity, int ticks, int color, float flash, float range) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(c.x, c.y, c.z, range, l.dimension())),
                new ShakePacket(c.x, c.y, c.z, intensity, ticks, color, flash, range));
    }
}
