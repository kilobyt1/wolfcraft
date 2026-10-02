package com.wolfcraft.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class TeslaArcEntity extends Entity {
    public static final EntityDataAccessor<Vector3fc> TARGET_POS = SynchedEntityData.defineId(TeslaArcEntity.class, EntityDataSerializers.VECTOR3);
    public static final EntityDataAccessor<Long> SEED = SynchedEntityData.defineId(TeslaArcEntity.class, EntityDataSerializers.LONG);

    private int life = 0;

    public TeslaArcEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public TeslaArcEntity(EntityType<?> type, Level level, Vec3 startPos, Vec3 endPos, long seed) {
        this(type, level);
        this.setPos(startPos.x, startPos.y, startPos.z);
        this.entityData.set(TARGET_POS, new Vector3f((float) endPos.x, (float) endPos.y, (float) endPos.z));
        this.entityData.set(SEED, seed);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TARGET_POS, new Vector3f());
        builder.define(SEED, 0L);
    }

    public Vec3 getTargetPos() {
        Vector3fc v = this.entityData.get(TARGET_POS);
        return new Vec3(v.x(), v.y(), v.z());
    }

    public long getSeed() {
        return this.entityData.get(SEED);
    }

    @Override
    public void tick() {
        super.tick();
        this.life++;
        if (this.life >= 3) {
            this.discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
