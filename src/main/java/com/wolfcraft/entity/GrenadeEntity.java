package com.wolfcraft.entity;

import com.wolfcraft.item.ModItems;
import com.wolfcraft.sound.ModSounds;
import net.minecraft.core.Direction;
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

import java.util.List;
import java.util.Optional;

public class GrenadeEntity extends Entity implements ItemSupplier {
    private int life = 0;
    private static final int MAX_FUSE = 70; // 3.5 seconds
    private Entity owner;

    public GrenadeEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public GrenadeEntity(EntityType<?> type, Level level, Entity owner, Vec3 spawnPos, Vec3 velocity) {
        this(type, level);
        this.owner = owner;
        this.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        this.setDeltaMovement(velocity);
    }

    @Override
    public ItemStack getItem() {
        return ModItems.GRENADE.getDefaultInstance();
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

        // Fuse spark / smoke
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.SMOKE,
                    pos.x, pos.y + 0.2, pos.z,
                    0, 0.04, 0);
            if (this.life % 4 == 0) {
                this.level().addParticle(ParticleTypes.SMALL_FLAME,
                        pos.x, pos.y + 0.2, pos.z,
                        0, 0.02, 0);
            }
        }

        // Raycast for obstacles / bounces
        BlockHitResult blockHit = this.level().clip(new ClipContext(
                pos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        if (blockHit.getType() != HitResult.Type.MISS) {
            Direction hitDir = blockHit.getDirection();
            Vec3 normal = new Vec3(hitDir.getStepX(), hitDir.getStepY(), hitDir.getStepZ());
            double dot = vel.dot(normal);
            vel = vel.subtract(normal.scale(2.0 * dot)).scale(0.55); // bounce restitution
            nextPos = blockHit.getLocation().add(normal.scale(0.08));

            if (vel.length() > 0.1) {
                this.level().playSound(null, pos.x, pos.y, pos.z,
                        ModSounds.GRENADE_BOUNCE, SoundSource.PLAYERS, 0.9f, 0.9f + (this.random.nextFloat() * 0.2f));
            } else {
                vel = new Vec3(0, 0, 0);
            }
        }

        // Entity collision check
        AABB aabb = this.getBoundingBox().expandTowards(vel).inflate(0.5);
        for (Entity e : this.level().getEntities(this, aabb, ent -> ent != owner && ent.isAlive() && !ent.isSpectator())) {
            AABB eBox = e.getBoundingBox().inflate(0.2);
            Optional<Vec3> clip = eBox.clip(pos, nextPos);
            if (clip.isPresent()) {
                // Direct impact on enemy explodes grenade immediately!
                explode(pos);
                return;
            }
        }

        if (this.life >= MAX_FUSE) {
            explode(pos);
            return;
        }

        this.setPos(nextPos.x, nextPos.y, nextPos.z);
        // Gravity & air friction
        this.setDeltaMovement(vel.scale(0.98).add(0, -0.04, 0));
    }

    private void explode(Vec3 hitPos) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Visual & sound effects
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, hitPos.x, hitPos.y + 0.2, hitPos.z, 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, hitPos.x, hitPos.y + 0.5, hitPos.z, 20, 0.4, 0.4, 0.4, 0.08);

            this.level().playSound(null, hitPos.x, hitPos.y, hitPos.z,
                    ModSounds.GRENADE_EXPLODE, SoundSource.PLAYERS, 2.5f, 0.95f + (this.random.nextFloat() * 0.1f));

            // Genuine RTCW explosion destruction & shrapnel
            this.level().explode(this.owner != null ? this.owner : this, hitPos.x, hitPos.y + 0.2, hitPos.z, 3.2f, Level.ExplosionInteraction.TNT);

            // Shrapnel damage to surrounding entities
            AABB blastBox = new AABB(hitPos.x - 5.0, hitPos.y - 4.0, hitPos.z - 5.0,
                    hitPos.x + 5.0, hitPos.y + 4.0, hitPos.z + 5.0);
            List<LivingEntity> nearby = this.level().getEntitiesOfClass(LivingEntity.class, blastBox, LivingEntity::isAlive);

            for (LivingEntity ent : nearby) {
                double dist = ent.position().distanceTo(hitPos);
                if (dist <= 5.0) {
                    float dmg = (float) ((5.0 - dist) / 5.0 * 22.0); // Up to 22 shrapnel dmg
                    if (this.owner instanceof Player player) {
                        ent.hurtServer(serverLevel, player.damageSources().playerAttack(player), dmg);
                    } else {
                        ent.hurtServer(serverLevel, this.damageSources().generic(), dmg);
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
