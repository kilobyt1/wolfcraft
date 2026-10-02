package com.wolfcraft.item;

import com.wolfcraft.sound.ModSounds;
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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.Optional;
import java.util.function.Consumer;

public class StenItem extends Item {
    public static final int MAGAZINE_CAPACITY = 32;
    public static final int MAX_HEAT = 100;
    public static final int HEAT_PER_SHOT = 9;

    public StenItem(Properties properties) {
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

    public static int getHeat(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            return data.copyTag().getInt("wolfcraft_heat").orElse(0);
        }
        return 0;
    }

    public static void setHeat(ItemStack stack, int heat) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("wolfcraft_heat", Math.clamp(heat, 0, MAX_HEAT)));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (entity instanceof Player) {
            int heat = getHeat(stack);
            if (heat > 0) {
                setHeat(stack, Math.max(0, heat - 2));
            }
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int heat = getHeat(stack);
        if (heat > 10) {
            return Math.round((float) heat * 13.0f / (float) MAX_HEAT);
        }
        int ammo = getLoadedAmmo(stack);
        return Math.round((float) ammo * 13.0f / (float) MAGAZINE_CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int heat = getHeat(stack);
        if (heat > 75) {
            return 0xFF1100; // Glowing red on near overheat
        } else if (heat > 35) {
            return 0xFF7700; // Orange warm
        }
        return 0x88CCFF; // Silenced cyan-blue ammo bar
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
                player.displayClientMessage(Component.translatable("message.wolfcraft.mag_full", "Sten", "32", "32"), true);
            }
            return InteractionResult.PASS;
        }

        if (player.getAbilities().instabuild) {
            setLoadedAmmo(stack, MAGAZINE_CAPACITY);
            playReloadEffects(level, player);
            player.getCooldowns().addCooldown(stack, 20);
            player.displayClientMessage(Component.translatable("message.wolfcraft.reloaded_creative", "Sten", "32", "32"), true);
            return InteractionResult.SUCCESS;
        }

        int available = countInventoryAmmo(player);
        if (available <= 0) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.wolfcraft.no_ammo", "Sten", "9mm"), true);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.STEN_RELOAD, SoundSource.PLAYERS, 1.0f, 2.0f);
            return InteractionResult.FAIL;
        }

        int needed = MAGAZINE_CAPACITY - current;
        int toLoad = Math.min(needed, available);
        consumeInventoryAmmo(player, toLoad);
        int newAmmo = current + toLoad;
        setLoadedAmmo(stack, newAmmo);

        playReloadEffects(level, player);
        player.getCooldowns().addCooldown(stack, 25);
        player.displayClientMessage(Component.translatable("message.wolfcraft.reloaded", "Sten", newAmmo, "32"), true);
        return InteractionResult.SUCCESS;
    }

    private void playReloadEffects(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.STEN_RELOAD, SoundSource.PLAYERS, 0.85f, 1.0f);
    }

    public void shoot(Level level, Player player, InteractionHand hand) {
        ItemStack gunStack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(gunStack)) {
            return;
        }

        int heat = getHeat(gunStack);
        if (heat >= MAX_HEAT) {
            // Overheat lock
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.STEN_OVERHEAT, SoundSource.PLAYERS, 1.0f, 1.0f);
            player.displayClientMessage(Component.translatable("message.wolfcraft.overheat", "Sten"), true);
            player.getCooldowns().addCooldown(gunStack, 50); // 2.5s overheat cooldown
            return;
        }

        int ammo = getLoadedAmmo(gunStack);
        if (!player.getAbilities().instabuild) {
            if (ammo <= 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.STEN_RELOAD, SoundSource.PLAYERS, 1.1f, 2.0f);
                player.displayClientMessage(Component.translatable("message.wolfcraft.mag_empty", "Sten"), true);
                player.getCooldowns().addCooldown(gunStack, 6);
                return;
            }
            int newAmmo = ammo - 1;
            setLoadedAmmo(gunStack, newAmmo);
            int newHeat = heat + HEAT_PER_SHOT;
            setHeat(gunStack, newHeat);

            if (newHeat >= MAX_HEAT) {
                // Just hit overheat
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.STEN_OVERHEAT, SoundSource.PLAYERS, 1.0f, 1.0f);
                player.getCooldowns().addCooldown(gunStack, 50);
                player.displayClientMessage(Component.translatable("message.wolfcraft.overheat_lock", "Sten", newAmmo, "32"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.wolfcraft.sten_ammo", newAmmo, newHeat), true);
            }
        } else {
            player.displayClientMessage(Component.translatable("message.wolfcraft.infinite", "§7[Sten]"), true);
        }

        // Authentic RTCW Sten sound (quiet, muffled silenced gunshot)
        float pitch = 0.95f + player.getRandom().nextFloat() * 0.1f;
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.STEN_FIRE, SoundSource.PLAYERS, 0.45f, pitch);

        // Low recoil (silenced 9mm)
        player.setXRot(player.getXRot() - 0.45f);
        gunStack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);

        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);


        Vec3 spread = new Vec3(
                (player.getRandom().nextDouble() - 0.5) * 0.02,
                (player.getRandom().nextDouble() - 0.5) * 0.02,
                (player.getRandom().nextDouble() - 0.5) * 0.02
        );
        Vec3 shootDir = look.add(spread).normalize();
        double range = 45.0;
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
                hitTarget.hurtServer(serverLevel, level.damageSources().playerAttack(player), 6.5f);
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        hitTarget.getX(), hitTarget.getEyeY(), hitTarget.getZ(),
                        5, 0.2, 0.2, 0.2, 0.1);
            } else {
                hitTarget.hurt(level.damageSources().playerAttack(player), 6.5f);
            }
        } else if (blockHit.getType() == HitResult.Type.BLOCK && level instanceof ServerLevel serverLevel) {
            Vec3 hitPos = blockHit.getLocation();
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    hitPos.x, hitPos.y, hitPos.z,
                    2, 0.03, 0.03, 0.03, 0.02);
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
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.AMMO_9MM)) {
                int count = stack.getCount();
                if (count <= amount) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                    amount -= count;
                } else {
                    stack.shrink(amount);
                    amount = 0;
                }
                if (amount <= 0) break;
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.sten.damage"));
        int ammo = getLoadedAmmo(stack);
        int heat = getHeat(stack);
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.sten.ammo", ammo, heat));
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.controls_fire_reload"));
    }
}
