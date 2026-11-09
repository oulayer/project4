package com.grandline;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Hiken: um punho de fogo de verdade, com modelo proprio, que voa e explode. */
public class FireFistEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> BIG =
            SynchedEntityData.defineId(FireFistEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int LIFETIME = 40;

    public FireFistEntity(EntityType<? extends FireFistEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public static FireFistEntity spawn(ServerPlayer p, ServerLevel l, Vec3 pos, Vec3 dir, boolean big, float speed) {
        FireFistEntity e = new FireFistEntity(ModEntities.FIRE_FIST.get(), l);
        e.setOwner(p);
        e.setPos(pos.x, pos.y, pos.z);
        Vec3 d = dir.normalize();
        e.setDeltaMovement(d.scale(speed));
        float yaw = (float) (Mth.atan2(-d.x, d.z) * 57.29577951308232);
        float pitch = (float) (-Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 57.29577951308232);
        e.setYRot(yaw); e.setXRot(pitch);
        e.yRotO = yaw; e.xRotO = pitch;
        e.getEntityData().set(BIG, big);
        l.addFreshEntity(e);
        return e;
    }

    public boolean isBig() { return this.entityData.get(BIG); }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(BIG, false);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = this.getDeltaMovement();
        if (this.level().isClientSide) {
            int n = isBig() ? 8 : 5;
            double w = isBig() ? 1.4 : 0.9;
            for (int i = 0; i < n; i++) {
                this.level().addParticle(ParticleTypes.FLAME,
                        getX() + (random.nextDouble() - 0.5) * w, getY() + (random.nextDouble() - 0.5) * w,
                        getZ() + (random.nextDouble() - 0.5) * w, -v.x * 0.08, 0.02, -v.z * 0.08);
            }
            if (tickCount % 2 == 0) {
                this.level().addParticle(ParticleTypes.LARGE_SMOKE, getX() - v.x * 0.6, getY(), getZ() - v.z * 0.6, 0, 0.02, 0);
                this.level().addParticle(ParticleTypes.LAVA, getX(), getY(), getZ(), 0, 0, 0);
            }
        } else {
            if (this.tickCount > LIFETIME) { explode(); return; }
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                this.onHit(hit);
                return;
            }
        }
        this.setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (!this.level().isClientSide) explode();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        if (!this.level().isClientSide) explode();
    }

    private void explode() {
        if (this.level() instanceof ServerLevel sl && getOwner() instanceof ServerPlayer sp) {
            Vec3 back = getDeltaMovement().normalize().scale(0.8);
            FlameMoves.explode(sp, sl, position().subtract(back), isBig());
        }
        this.discard();
    }
}
