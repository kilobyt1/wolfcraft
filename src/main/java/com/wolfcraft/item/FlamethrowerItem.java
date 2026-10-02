package com.wolfcraft.item;

import com.wolfcraft.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class FlamethrowerItem extends Item {
    public static final int TANK_CAPACITY = 150;
    private static final Map<UUID, Long> LAST_FLAME_SOUND_TIME = new ConcurrentHashMap<>();

    public FlamethrowerItem(Properties properties) {
        super(properties);
    }

    public static int getFuel(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            return data.copyTag().getInt("wolfcraft_fuel").orElse(TANK_CAPACITY);
        }
        return TANK_CAPACITY;
    }

    public static void setFuel(ItemStack stack, int fuel) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("wolfcraft_fuel", Math.max(0, fuel)));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int fuel = getFuel(stack);
        return Math.round((float) fuel * 13.0f / (float) TANK_CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFF4400; // Flame orange bar
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return use(context.getLevel(), context.getPlayer(), context.getHand());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int current = getFuel(stack);

        if (current >= TANK_CAPACITY) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.wolfcraft.flamethrower.tank_full"), true);
            }
            return InteractionResult.PASS;
        }

        if (player.getAbilities().instabuild) {
            setFuel(stack, TANK_CAPACITY);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.FLAMETHROWER_START, SoundSource.PLAYERS, 1.0f, 1.0f);
            player.getCooldowns().addCooldown(stack, 30);
            player.displayClientMessage(Component.translatable("message.wolfcraft.reloaded_creative", "Flammenwerfer", "150", "150"), true);
            return InteractionResult.SUCCESS;
        }

        // Look for Fuel Tank
        ItemStack canister = ItemStack.EMPTY;
        int slot = -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack inv = player.getInventory().getItem(i);
            if (!inv.isEmpty() && inv.is(ModItems.FUEL_TANK)) {
                canister = inv;
                slot = i;
                break;
            }
        }

        if (canister.isEmpty()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.wolfcraft.flamethrower.no_fuel"), true);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.FLAMETHROWER_END, SoundSource.PLAYERS, 1.0f, 1.5f);
            return InteractionResult.FAIL;
        }

        canister.shrink(1);
        setFuel(stack, TANK_CAPACITY);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.FLAMETHROWER_START, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.getCooldowns().addCooldown(stack, 30);

        if (!level.isClientSide()) {
            player.displayClientMessage(Component.translatable("message.wolfcraft.flamethrower.refueled"), true);
        }
        return InteractionResult.SUCCESS;
    }

    public void shoot(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(stack)) {
            return;
        }

        int fuel = getFuel(stack);
        if (fuel <= 0 && !player.getAbilities().instabuild) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.FLAMETHROWER_END, SoundSource.PLAYERS, 0.8f, 1.6f);
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.wolfcraft.flamethrower.empty"), true);
            }
            return;
        }

        if (!player.getAbilities().instabuild) {
            setFuel(stack, fuel - 1);
        }

        // Smooth loop: flamethrower_fire.ogg is ~1.45s long, loop every 1350ms to prevent ear-grating audio overlap
        long now = System.currentTimeMillis();
        Long lastSound = LAST_FLAME_SOUND_TIME.get(player.getUUID());
        if (lastSound == null || now - lastSound >= 1350) {
            LAST_FLAME_SOUND_TIME.put(player.getUUID(), now);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.FLAMETHROWER_FIRE, SoundSource.PLAYERS, 0.85f, 1.0f);
        }

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            Vec3 eyePos = player.getEyePosition(1.0f);
            Vec3 lookVec = player.getViewVector(1.0f);

            double streamRange = 12.0;

            // Emit streaming particles along flame cone
            for (int step = 1; step <= 12; step++) {
                double dist = step * 1.0;
                double spreadRadius = dist * 0.08;
                double px = eyePos.x + lookVec.x * dist + (serverLevel.random.nextDouble() - 0.5) * spreadRadius;
                double py = eyePos.y + lookVec.y * dist + (serverLevel.random.nextDouble() - 0.5) * spreadRadius;
                double pz = eyePos.z + lookVec.z * dist + (serverLevel.random.nextDouble() - 0.5) * spreadRadius;

                serverLevel.sendParticles(ParticleTypes.FLAME,
                        px, py, pz,
                        2, 0.05, 0.05, 0.05, 0.02);

                if (step % 3 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                            px, py, pz,
                            1, 0.02, 0.02, 0.02, 0.01);
                }
            }

            // Raycast for obstacles
            Vec3 endPos = eyePos.add(lookVec.scale(streamRange));
            BlockHitResult blockHit = level.clip(new ClipContext(
                    eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
            ));

            if (blockHit.getType() != HitResult.Type.MISS) {
                endPos = blockHit.getLocation();
                BlockPos hitBlockPos = blockHit.getBlockPos();
                BlockPos firePos = hitBlockPos.relative(blockHit.getDirection());
                if (level.isEmptyBlock(firePos) && level.random.nextInt(5) == 0) {
                    level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
                }
            }

            // Damage and ignite entities in cone
            AABB flameCone = player.getBoundingBox().expandTowards(lookVec.scale(streamRange)).inflate(1.5);
            List<Entity> targets = level.getEntities(player, flameCone,
                    e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator());

            for (Entity entity : targets) {
                Vec3 toEntity = entity.position().add(0, entity.getEyeHeight() / 2.0, 0).subtract(eyePos);
                double d = toEntity.length();
                if (d <= streamRange && d > 0.001) {
                    Vec3 norm = toEntity.normalize();
                    double dot = norm.dot(lookVec);
                    if (dot > 0.85) { // within ~30 degree cone
                        LivingEntity living = (LivingEntity) entity;
                        living.hurtServer(serverLevel, player.damageSources().inFire(), 4.5f);
                        living.setRemainingFireTicks(120); // 6 seconds on fire
                    }
                }
            }

            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        int fuel = getFuel(stack);
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.flamethrower.ammo", fuel));
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.flamethrower.damage"));
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.flamethrower.controls"));
    }
}
