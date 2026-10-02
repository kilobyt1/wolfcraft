package com.wolfcraft.entity;

import com.wolfcraft.item.ModItems;
import com.wolfcraft.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class PanzerfaustRocketEntity extends Entity implements ItemSupplier {
    private int life = 0;
    private Entity owner;

    public PanzerfaustRocketEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public ItemStack getItem() {
        return ModItems.PANZERFAUST_ROCKET.getDefaultInstance();
    }

    public PanzerfaustRocketEntity(EntityType<?> type, Level level, Entity owner, Vec3 spawnPos, Vec3 velocity) {
        this(type, level);
        this.owner = owner;
        this.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        this.setDeltaMovement(velocity);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        this.life++;

        Vec3 pos = this.position();
        Vec3 vel = this.getDeltaMovement();
        Vec3 nextPos = pos.add(vel);

        // Rocket flight smoke plume
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.x, pos.y, pos.z,
                    -vel.x * 0.15, -vel.y * 0.15 + 0.02, -vel.z * 0.15);
            this.level().addParticle(ParticleTypes.FLAME,
                    pos.x - vel.x * 0.1, pos.y - vel.y * 0.1, pos.z - vel.z * 0.1,
                    0, 0, 0);
        }

        // Raycast hit detection
        BlockHitResult blockHit = this.level().clip(new ClipContext(
                pos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        boolean hit = false;
        Vec3 hitPos = nextPos;

        if (blockHit.getType() != HitResult.Type.MISS) {
            hit = true;
            hitPos = blockHit.getLocation();
        }

        AABB aabb = this.getBoundingBox().expandTowards(vel).inflate(1.0);
        for (Entity e : this.level().getEntities(this, aabb, ent -> ent != owner && ent.isAlive() && !ent.isSpectator())) {
            AABB eBox = e.getBoundingBox().inflate(0.3);
            Optional<Vec3> clip = eBox.clip(pos, nextPos);
            if (clip.isPresent()) {
                hit = true;
                hitPos = clip.get();
                break;
            }
        }

        if (hit || this.life > 100) {
            explode(hitPos);
            return;
        }

        this.setPos(nextPos.x, nextPos.y, nextPos.z);
        // Slight rocket gravity
        this.setDeltaMovement(vel.add(0, -0.012, 0));
    }

    private void explode(Vec3 hitLocation) {
        if (!this.level().isClientSide()) {
            this.level().playSound(null, hitLocation.x, hitLocation.y, hitLocation.z,
                    ModSounds.PANZERFAUST_EXPLODE, SoundSource.BLOCKS, 1.2f, 1.0f);

            // True block destruction and terrain damage
            this.level().explode(
                    this.owner != null ? this.owner : this,
                    hitLocation.x, hitLocation.y, hitLocation.z,
                    4.2f,
                    Level.ExplosionInteraction.BLOCK
            );

            // Direct blast damage to nearby entities
            if (this.level() instanceof ServerLevel serverLevel) {
                AABB blastBox = new AABB(hitLocation.add(-5, -5, -5), hitLocation.add(5, 5, 5));
                for (Entity e : serverLevel.getEntities(this, blastBox, ent -> ent instanceof LivingEntity && ent.isAlive())) {
                    LivingEntity target = (LivingEntity) e;
                    double dist = target.position().distanceTo(hitLocation);
                    if (dist < 5.0) {
                        float dmg = (float) ((1.0 - (dist / 5.0)) * 65.0f);
                        target.hurtServer(serverLevel, this.level().damageSources().explosion(this, this.owner), dmg);
                    }
                }
            }
        }
        this.discard();
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
