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

public class LugerItem extends Item {
    public static final int MAGAZINE_CAPACITY = 8;

    public LugerItem(Properties properties) {
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
        return 0x55FFFF; // Cyan / silver precision bar
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
                player.displayClientMessage(Component.translatable("message.wolfcraft.mag_full", "Luger P08", "8", "8"), true);
            }
            return InteractionResult.PASS;
        }

        if (player.getAbilities().instabuild) {
            setLoadedAmmo(stack, MAGAZINE_CAPACITY);
            playReloadEffects(level, player);
            player.getCooldowns().addCooldown(stack, 22);
            player.displayClientMessage(Component.translatable("message.wolfcraft.reloaded_creative", "Luger P08", "8", "8"), true);
            return InteractionResult.SUCCESS;
        }

        int available = countInventoryAmmo(player);
        if (available <= 0) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.wolfcraft.no_ammo", "Luger P08", "9mm"), true);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.LUGER_RELOAD, SoundSource.PLAYERS, 1.0f, 1.8f);
            return InteractionResult.FAIL;
        }

        int needed = MAGAZINE_CAPACITY - current;
        int toLoad = Math.min(needed, available);
        consumeInventoryAmmo(player, toLoad);
        setLoadedAmmo(stack, current + toLoad);

        playReloadEffects(level, player);
        player.getCooldowns().addCooldown(stack, 22);

        if (!level.isClientSide()) {
            player.displayClientMessage(Component.translatable("message.wolfcraft.reloaded", "Luger P08", current + toLoad, "8"), true);
        }
        return InteractionResult.SUCCESS;
    }

    private int countInventoryAmmo(Player player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (!invStack.isEmpty() && invStack.is(ModItems.AMMO_9MM)) {
                total += invStack.getCount();
            }
        }
        return total;
    }

    private void consumeInventoryAmmo(Player player, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (!invStack.isEmpty() && invStack.is(ModItems.AMMO_9MM)) {
                int count = invStack.getCount();
                if (count <= remaining) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                    remaining -= count;
                } else {
                    invStack.shrink(remaining);
                    remaining = 0;
                }
                if (remaining <= 0) break;
            }
        }
    }

    private void playReloadEffects(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.LUGER_RELOAD, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    public void shoot(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(stack)) {
            return;
        }

        int ammo = getLoadedAmmo(stack);
        if (ammo <= 0 && !player.getAbilities().instabuild) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.LUGER_RELOAD, SoundSource.PLAYERS, 0.8f, 2.0f);
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.wolfcraft.mag_empty", "Luger P08"), true);
            }
            return;
        }

        if (!player.getAbilities().instabuild) {
            setLoadedAmmo(stack, ammo - 1);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.LUGER_FIRE, SoundSource.PLAYERS, 1.05f, 0.98f + (level.random.nextFloat() * 0.06f));

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            Vec3 eyePos = player.getEyePosition(1.0f);
            Vec3 lookVec = player.getViewVector(1.0f);

            float spread = 0.010f; // very high precision
            double spreadX = (serverLevel.random.nextDouble() - 0.5) * spread;
            double spreadY = (serverLevel.random.nextDouble() - 0.5) * spread;
            double spreadZ = (serverLevel.random.nextDouble() - 0.5) * spread;
            Vec3 dir = lookVec.add(spreadX, spreadY, spreadZ).normalize();

            double maxDistance = 64.0;
            Vec3 targetPos = eyePos.add(dir.scale(maxDistance));

            BlockHitResult blockHit = level.clip(new ClipContext(
                    eyePos, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
            ));

            Vec3 endPos = targetPos;
            if (blockHit.getType() != HitResult.Type.MISS) {
                endPos = blockHit.getLocation();
            }

            LivingEntity hitEntity = null;
            double closestDistSq = maxDistance * maxDistance;

            AABB searchBox = player.getBoundingBox().expandTowards(dir.scale(maxDistance)).inflate(1.0);
            for (Entity entity : level.getEntities(player, searchBox, e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator())) {
                AABB entityBox = entity.getBoundingBox().inflate(0.3);
                Optional<Vec3> clip = entityBox.clip(eyePos, endPos);
                if (clip.isPresent()) {
                    double distSq = eyePos.distanceToSqr(clip.get());
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        hitEntity = (LivingEntity) entity;
                        endPos = clip.get();
                    }
                }
            }

            if (hitEntity != null) {
                hitEntity.hurtServer(serverLevel, player.damageSources().playerAttack(player), 7.0f);
                hitEntity.knockback(0.35, -dir.x, -dir.z);

                serverLevel.sendParticles(ParticleTypes.CRIT,
                        endPos.x, endPos.y, endPos.z,
                        5, 0.1, 0.1, 0.1, 0.12);
            } else if (blockHit.getType() == HitResult.Type.BLOCK) {
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        endPos.x, endPos.y, endPos.z,
                        3, 0.05, 0.05, 0.05, 0.02);
            }

            com.wolfcraft.WolfCraftMod.spawnMuzzleFlashAndTracer(serverLevel, player, endPos);
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        int ammo = getLoadedAmmo(stack);
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.luger.ammo", ammo));
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.luger.damage"));
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.controls_reload"));
    }
}
