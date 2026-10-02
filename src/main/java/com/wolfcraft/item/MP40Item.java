package com.wolfcraft.item;

import com.wolfcraft.sound.ModSounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Consumer;

public class MP40Item extends Item {
    public static final int MAGAZINE_CAPACITY = 32;

    public MP40Item(Properties properties) {
        super(properties);
    }

    public static int getLoadedAmmo(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            return data.copyTag().getInt("wolfcraft_ammo").orElse(MAGAZINE_CAPACITY);
        }
        return MAGAZINE_CAPACITY;
    }

    public static void setLoadedAmmo(ItemStack stack, int ammo) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("wolfcraft_ammo", Math.max(0, ammo)));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int ammo = getLoadedAmmo(stack);
        return Math.round((float) ammo * 13.0f / (float) MAGAZINE_CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFCC00; // Gold magazine bar
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return use(context.getLevel(), context.getPlayer(), context.getHand());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int current = getLoadedAmmo(stack);

        if (current >= MAGAZINE_CAPACITY) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.wolfcraft.mag_full", "MP40", "32", "32"), true);
            }
            return InteractionResult.PASS;
        }

        if (player.getAbilities().instabuild) {
            setLoadedAmmo(stack, MAGAZINE_CAPACITY);
            playReloadEffects(level, player);
            player.getCooldowns().addCooldown(stack, 20);
            player.displayClientMessage(Component.translatable("message.wolfcraft.reloaded_creative", "MP40", "32", "32"), true);
            return InteractionResult.SUCCESS;
        }

        int available = countInventoryAmmo(player);
        if (available <= 0) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.wolfcraft.no_ammo", "MP40", "9mm"), true);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.MP40_RELOAD, SoundSource.PLAYERS, 1.0f, 2.0f);
            return InteractionResult.FAIL;
        }

        int needed = MAGAZINE_CAPACITY - current;
        int toLoad = Math.min(needed, available);
        consumeInventoryAmmo(player, toLoad);
        int newAmmo = current + toLoad;
        setLoadedAmmo(stack, newAmmo);

        playReloadEffects(level, player);
        player.getCooldowns().addCooldown(stack, 25);
        player.displayClientMessage(Component.translatable("message.wolfcraft.reloaded", "MP40", newAmmo, "32"), true);
        return InteractionResult.SUCCESS;
    }

    private void playReloadEffects(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.MP40_RELOAD, SoundSource.PLAYERS, 0.85f, 1.0f);
    }

    public void shoot(Level level, Player player, InteractionHand hand) {
        ItemStack gunStack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(gunStack)) {
            return;
        }

        int ammo = getLoadedAmmo(gunStack);

        if (!player.getAbilities().instabuild) {
            if (ammo <= 0) {
                // Dry click
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.MP40_RELOAD, SoundSource.PLAYERS, 1.1f, 2.0f);
                player.displayClientMessage(Component.translatable("message.wolfcraft.mag_empty", "MP40"), true);
                player.getCooldowns().addCooldown(gunStack, 6);
                return;
            }
            int newAmmo = ammo - 1;
            setLoadedAmmo(gunStack, newAmmo);
            player.displayClientMessage(Component.translatable("message.wolfcraft.ammo_status", "MP40", newAmmo, "32"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.wolfcraft.infinite", "§6[MP40]"), true);
        }

        // Authentic RTCW MP40 sound - volume reduced by 15% (0.85 -> 0.72)
        float pitch = 0.95f + player.getRandom().nextFloat() * 0.1f;
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.MP40_FIRE, SoundSource.PLAYERS, 0.72f, pitch);

        // Recoil
        player.setXRot(player.getXRot() - 0.7f);
        gunStack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);

        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);

        // Perpendicular camera coordinate frame for precise muzzle position
        double yawRad = Math.toRadians(player.getYRot() + 90.0f);
        Vec3 right = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 up = right.cross(look).normalize();
        Vec3 muzzle = eyePos.add(look.scale(0.75)).add(right.scale(0.02)).add(up.scale(-0.55));

        Vec3 spread = new Vec3(
                (player.getRandom().nextDouble() - 0.5) * 0.03,
                (player.getRandom().nextDouble() - 0.5) * 0.03,
                (player.getRandom().nextDouble() - 0.5) * 0.03
        );
        Vec3 shootDir = look.add(spread).normalize();
        double range = 50.0;
        Vec3 reach = eyePos.add(shootDir.scale(range));

        BlockHitResult blockHit = level.clip(new ClipContext(
                eyePos, reach, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDist = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation().distanceTo(eyePos) : range;

        AABB aabb = player.getBoundingBox().expandTowards(shootDir.scale(maxDist)).inflate(1.5);
        LivingEntity hitTarget = null;
        double minDistance = maxDist;

        for (Entity entity : level.getEntities(player, aabb, e -> !e.isSpectator() && e.isAlive() && e != player)) {
            if (entity instanceof LivingEntity living) {
                AABB box = living.getBoundingBox().inflate(0.3);
                Optional<Vec3> hit = box.clip(eyePos, reach);
                if (hit.isPresent()) {
                    double dist = eyePos.distanceTo(hit.get());
                    if (dist < minDistance) {
                        minDistance = dist;
                        hitTarget = living;
                    }
                }
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            Vec3 targetPoint = hitTarget != null ? hitTarget.position().add(0, hitTarget.getEyeHeight() * 0.75, 0)
                    : (blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : reach);
            com.wolfcraft.WolfCraftMod.spawnMuzzleFlashAndTracer(serverLevel, player, targetPoint);
        }

        if (hitTarget != null) {
            if (level instanceof ServerLevel serverLevel) {
                hitTarget.hurtServer(serverLevel, level.damageSources().playerAttack(player), 7.0f);
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        hitTarget.getX(), hitTarget.getEyeY(), hitTarget.getZ(),
                        6, 0.2, 0.2, 0.2, 0.1);
            } else {
                hitTarget.hurt(level.damageSources().playerAttack(player), 7.0f);
            }
        } else if (blockHit.getType() == HitResult.Type.BLOCK && level instanceof ServerLevel serverLevel) {
            Vec3 hitPos = blockHit.getLocation();
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    hitPos.x, hitPos.y, hitPos.z,
                    3, 0.05, 0.05, 0.05, 0.03);
        }
    }

    private int countInventoryAmmo(Player player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.AMMO_9MM)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void consumeInventoryAmmo(Player player, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (remaining <= 0) break;
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.AMMO_9MM)) {
                int toTake = Math.min(remaining, stack.getCount());
                stack.shrink(toTake);
                remaining -= toTake;
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.mp40.damage"));
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.mp40.ammo", getLoadedAmmo(stack)));
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.controls_fire_reload"));
    }
}
