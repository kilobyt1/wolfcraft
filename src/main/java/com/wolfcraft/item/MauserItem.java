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

public class MauserItem extends Item {
    public static final int MAGAZINE_CAPACITY = 10;

    public MauserItem(Properties properties) {
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

    public static boolean isScoped(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            return data.copyTag().getBoolean("wolfcraft_scoped").orElse(false);
        }
        return false;
    }

    public static void setScoped(ItemStack stack, boolean scoped) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("wolfcraft_scoped", scoped));
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
        return 0xDDAA00; // Sniper brass color
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
                player.displayClientMessage(Component.translatable("message.wolfcraft.mag_full", "Mauser 98k", "10", "10"), true);
            }
            return InteractionResult.PASS;
        }

        if (player.getAbilities().instabuild) {
            setLoadedAmmo(stack, MAGAZINE_CAPACITY);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.MAUSER_RELOAD, SoundSource.PLAYERS, 0.9f, 1.0f);
            player.getCooldowns().addCooldown(stack, 25);
            player.displayClientMessage(Component.translatable("message.wolfcraft.reloaded_creative", "Mauser 98k", "10", "10"), true);
            return InteractionResult.SUCCESS;
        }

        int available = countInventoryAmmo(player);
        if (available <= 0) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.wolfcraft.no_ammo", "Mauser 98k", "7.92mm"), true);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.MAUSER_EJECT, SoundSource.PLAYERS, 1.0f, 1.5f);
            return InteractionResult.FAIL;
        }

        int needed = MAGAZINE_CAPACITY - current;
        int toLoad = Math.min(needed, available);
        consumeInventoryAmmo(player, toLoad);
        int newAmmo = current + toLoad;
        setLoadedAmmo(stack, newAmmo);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.MAUSER_RELOAD, SoundSource.PLAYERS, 0.9f, 1.0f);
        player.getCooldowns().addCooldown(stack, 30);
        player.displayClientMessage(Component.translatable("message.wolfcraft.reloaded", "Mauser 98k", newAmmo, "10"), true);
        return InteractionResult.SUCCESS;
    }

    public void shoot(Level level, Player player, InteractionHand hand) {
        ItemStack gunStack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(gunStack)) {
            return;
        }

        int ammo = getLoadedAmmo(gunStack);
        if (!player.getAbilities().instabuild) {
            if (ammo <= 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.MAUSER_EJECT, SoundSource.PLAYERS, 1.0f, 1.5f);
                player.displayClientMessage(Component.translatable("message.wolfcraft.mag_empty", "Mauser 98k"), true);
                player.getCooldowns().addCooldown(gunStack, 10);
                return;
            }
            int newAmmo = ammo - 1;
            setLoadedAmmo(gunStack, newAmmo);
            player.displayClientMessage(Component.translatable("message.wolfcraft.ammo_status", "Mauser 98k", newAmmo, "10"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.wolfcraft.infinite", "§6[Mauser 98k]"), true);
        }

        // Loud sharp gunshot
        float pitch = 0.98f + player.getRandom().nextFloat() * 0.05f;
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.MAUSER_FIRE, SoundSource.PLAYERS, 1.0f, pitch);

        // Strong bolt-action recoil
        player.setXRot(player.getXRot() - 1.8f);
        gunStack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);

        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);

        double yawRad = Math.toRadians(player.getYRot() + 90.0f);
        Vec3 right = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 up = right.cross(look).normalize();
        Vec3 muzzle = eyePos.add(look.scale(0.85)).add(right.scale(0.02)).add(up.scale(-0.55));

        boolean scoped = isScoped(gunStack);
        Vec3 spread = scoped ? Vec3.ZERO : new Vec3(
                (player.getRandom().nextDouble() - 0.5) * 0.015,
                (player.getRandom().nextDouble() - 0.5) * 0.015,
                (player.getRandom().nextDouble() - 0.5) * 0.015
        );
        Vec3 shootDir = look.add(spread).normalize();
        double range = 120.0; // Extreme sniper range
        Vec3 reach = eyePos.add(shootDir.scale(range));

        BlockHitResult blockHit = level.clip(new ClipContext(
                eyePos, reach, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDist = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation().distanceTo(eyePos) : range;

        AABB aabb = player.getBoundingBox().expandTowards(shootDir.scale(maxDist)).inflate(1.5);
        LivingEntity hitTarget = null;
        double minDistance = maxDist;
        boolean headshot = false;

        for (Entity entity : level.getEntities(player, aabb, e -> !e.isSpectator() && e.isAlive() && e != player)) {
            if (entity instanceof LivingEntity living) {
                AABB box = living.getBoundingBox().inflate(0.2);
                Optional<Vec3> hit = box.clip(eyePos, reach);
                if (hit.isPresent()) {
                    double dist = eyePos.distanceTo(hit.get());
                    if (dist < minDistance) {
                        minDistance = dist;
                        hitTarget = living;
                        // Headshot detection: top 25% of target hitbox
                        double relY = hit.get().y - living.getY();
                        headshot = relY > (living.getBbHeight() * 0.75);
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
            float baseDmg = 25.0f;
            float totalDmg = headshot ? baseDmg * 2.5f : baseDmg;

            if (level instanceof ServerLevel serverLevel) {
                hitTarget.hurtServer(serverLevel, level.damageSources().playerAttack(player), totalDmg);
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        hitTarget.getX(), hitTarget.getEyeY(), hitTarget.getZ(),
                        headshot ? 15 : 8, 0.3, 0.3, 0.3, 0.2);
            } else {
                hitTarget.hurt(level.damageSources().playerAttack(player), totalDmg);
            }

            if (headshot && !level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.wolfcraft.headshot", (int) totalDmg), true);
            }
        } else if (blockHit.getType() == HitResult.Type.BLOCK && level instanceof ServerLevel serverLevel) {
            Vec3 hitPos = blockHit.getLocation();
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    hitPos.x, hitPos.y, hitPos.z,
                    4, 0.06, 0.06, 0.06, 0.03);
        }

        // 26 ticks bolt-action cycle delay
        player.getCooldowns().addCooldown(gunStack, 26);
    }

    private int countInventoryAmmo(Player player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.AMMO_792MM)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void consumeInventoryAmmo(Player player, int amount) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.AMMO_792MM)) {
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
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.mauser.damage"));
        int ammo = getLoadedAmmo(stack);
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.mauser.ammo", ammo));
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.mauser.controls"));
    }
}
