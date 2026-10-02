package com.wolfcraft.item;

import com.wolfcraft.entity.ModEntities;
import com.wolfcraft.entity.TeslaArcEntity;
import com.wolfcraft.sound.ModSounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class TeslaGunItem extends Item {
    public static final int BATTERY_CAPACITY = 50;

    public TeslaGunItem(Properties properties) {
        super(properties);
    }

    public static int getLoadedCharges(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            return data.copyTag().getInt("wolfcraft_ammo").orElse(BATTERY_CAPACITY);
        }
        return BATTERY_CAPACITY;
    }

    public static void setLoadedCharges(ItemStack stack, int charges) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("wolfcraft_ammo", Math.max(0, charges)));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int charges = getLoadedCharges(stack);
        return Math.round((float) charges * 13.0f / (float) BATTERY_CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00FFFF; // Cyan electric battery bar
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return use(context.getLevel(), context.getPlayer(), context.getHand());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int current = getLoadedCharges(stack);

        if (current >= BATTERY_CAPACITY) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.wolfcraft.tesla.battery_full"), true);
            }
            return InteractionResult.PASS;
        }

        if (player.getAbilities().instabuild) {
            setLoadedCharges(stack, BATTERY_CAPACITY);
            playRechargeEffects(level, player);
            player.getCooldowns().addCooldown(stack, 20);
            player.displayClientMessage(Component.translatable("message.wolfcraft.reloaded_creative", "Tesla Gun", "50", "50"), true);
            return InteractionResult.SUCCESS;
        }

        int available = countInventoryCells(player);
        if (available <= 0) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.wolfcraft.tesla.no_cells"), true);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.TESLA_IDLE, SoundSource.PLAYERS, 1.0f, 1.8f);
            return InteractionResult.FAIL;
        }

        consumeInventoryCell(player);
        setLoadedCharges(stack, BATTERY_CAPACITY);

        playRechargeEffects(level, player);
        player.getCooldowns().addCooldown(stack, 25);
        player.displayClientMessage(Component.translatable("message.wolfcraft.tesla.charged"), true);
        return InteractionResult.SUCCESS;
    }

    private void playRechargeEffects(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TESLA_IDLE, SoundSource.PLAYERS, 0.75f, 1.2f);
    }

    public void shoot(Level level, Player player, InteractionHand hand) {
        ItemStack gunStack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(gunStack)) {
            return;
        }

        int charges = getLoadedCharges(gunStack);

        if (!player.getAbilities().instabuild) {
            if (charges <= 0) {
                // Dry spark
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.TESLA_IDLE, SoundSource.PLAYERS, 1.1f, 1.8f);
                player.displayClientMessage(Component.translatable("message.wolfcraft.tesla.empty"), true);
                player.getCooldowns().addCooldown(gunStack, 6);
                return;
            }
            int newCharges = charges - 1;
            setLoadedCharges(gunStack, newCharges);
            player.displayClientMessage(Component.translatable("message.wolfcraft.tesla.status", newCharges), true);
        } else {
            player.displayClientMessage(Component.translatable("message.wolfcraft.infinite", "§b[Tesla Gun]"), true);
        }

        // Authentic RTCW Tesla zap sound - volume reduced by 35% (0.85 -> 0.55)
        float pitch = 0.95f + player.getRandom().nextFloat() * 0.1f;
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TESLA_ZAP, SoundSource.PLAYERS, 0.55f, pitch);

        gunStack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);

        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);

        // Perpendicular camera coordinate frame for precise muzzle position
        double yawRad = Math.toRadians(player.getYRot() + 90.0f);
        Vec3 right = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 up = right.cross(look).normalize();
        Vec3 muzzle = eyePos.add(look.scale(0.85)).add(right.scale(0.24)).add(up.scale(-0.18));

        double range = 18.0;

        AABB searchBox = player.getBoundingBox().inflate(range);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive() && !e.isSpectator());

        List<LivingEntity> hitTargets = new ArrayList<>();
        for (LivingEntity e : potentialTargets) {
            Vec3 toEntity = e.getEyePosition().subtract(eyePos);
            double dist = toEntity.length();
            if (dist <= range) {
                double dot = toEntity.normalize().dot(look);
                if (dot > 0.5) {
                    hitTargets.add(e);
                }
            }
        }
        hitTargets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));

        int chained = 0;
        Vec3 currentArcOrigin = muzzle;

        for (LivingEntity target : hitTargets) {
            if (chained >= 3) break;

            if (level instanceof ServerLevel serverLevel) {
                target.hurtServer(serverLevel, level.damageSources().playerAttack(player), 9.0f);
                target.igniteForSeconds(2.0f);

                // Spawn horizontal real lightning arc entity from origin to target!
                Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
                long seed = level.random.nextLong();
                TeslaArcEntity arc = new TeslaArcEntity(ModEntities.TESLA_ARC, serverLevel, currentArcOrigin, targetPos, seed);
                serverLevel.addFreshEntity(arc);
                currentArcOrigin = targetPos;
            } else {
                target.hurt(level.damageSources().playerAttack(player), 9.0f);
            }

            chained++;
        }

        // If no targets were in range, fire horizontal lightning beam forward into the air
        if (chained == 0 && level instanceof ServerLevel serverLevel) {
            Vec3 beamEnd = eyePos.add(look.scale(range));
            long seed = level.random.nextLong();
            TeslaArcEntity arc = new TeslaArcEntity(ModEntities.TESLA_ARC, serverLevel, currentArcOrigin, beamEnd, seed);
            serverLevel.addFreshEntity(arc);
        }
    }

    private int countInventoryCells(Player player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.TESLA_CELL)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void consumeInventoryCell(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.TESLA_CELL)) {
                stack.shrink(1);
                return;
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.tesla.damage"));
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.tesla.ammo", getLoadedCharges(stack)));
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.tesla.controls"));
    }
}
