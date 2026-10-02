package com.wolfcraft.item;

import com.wolfcraft.entity.ModEntities;
import com.wolfcraft.entity.PanzerfaustRocketEntity;
import com.wolfcraft.sound.ModSounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;

public class PanzerfaustItem extends Item {
    public PanzerfaustItem(Properties properties) {
        super(properties);
    }

    public static boolean isLoaded(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            return data.copyTag().getInt("wolfcraft_ammo").orElse(1) > 0;
        }
        return true;
    }

    public static void setLoaded(ItemStack stack, boolean loaded) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("wolfcraft_ammo", loaded ? 1 : 0));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return isLoaded(stack) ? 13 : 0;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFF3300; // Explosive warhead orange/red
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return use(context.getLevel(), context.getPlayer(), context.getHand());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isLoaded(stack)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.wolfcraft.panzerfaust.already_loaded"), true);
            }
            return InteractionResult.PASS;
        }

        if (player.getAbilities().instabuild) {
            setLoaded(stack, true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.MP40_RELOAD, SoundSource.PLAYERS, 0.9f, 0.7f);
            player.getCooldowns().addCooldown(stack, 25);
            player.displayClientMessage(Component.translatable("message.wolfcraft.panzerfaust.loaded"), true);
            return InteractionResult.SUCCESS;
        }

        int available = countInventoryAmmo(player);
        if (available <= 0) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.wolfcraft.panzerfaust.no_ammo"), true);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.MP40_RELOAD, SoundSource.PLAYERS, 0.8f, 2.0f);
            return InteractionResult.FAIL;
        }

        consumeInventoryAmmo(player, 1);
        setLoaded(stack, true);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.MP40_RELOAD, SoundSource.PLAYERS, 0.9f, 0.7f);
        player.getCooldowns().addCooldown(stack, 40); // 2s reload
        player.displayClientMessage(Component.translatable("message.wolfcraft.panzerfaust.loaded"), true);
        return InteractionResult.SUCCESS;
    }

    public void shoot(Level level, Player player, InteractionHand hand) {
        ItemStack gunStack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(gunStack)) {
            return;
        }

        if (!player.getAbilities().instabuild && !isLoaded(gunStack)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.MP40_RELOAD, SoundSource.PLAYERS, 0.9f, 2.0f);
            player.displayClientMessage(Component.translatable("message.wolfcraft.panzerfaust.empty"), true);
            player.getCooldowns().addCooldown(gunStack, 10);
            return;
        }

        if (!player.getAbilities().instabuild) {
            setLoaded(gunStack, false);
            player.displayClientMessage(Component.translatable("message.wolfcraft.panzerfaust.fired"), true);
        }

        // Authentic rocket firing sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.PANZERFAUST_FIRE, SoundSource.PLAYERS, 1.2f, 1.0f);

        // Heavy backblast recoil
        player.setXRot(player.getXRot() - 2.5f);
        Vec3 look = player.getViewVector(1.0f);
        player.setDeltaMovement(player.getDeltaMovement().add(-look.x * 0.15, 0.05, -look.z * 0.15));
        gunStack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);

        Vec3 eyePos = player.getEyePosition();
        double yawRad = Math.toRadians(player.getYRot() + 90.0f);
        Vec3 right = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 up = right.cross(look).normalize();
        Vec3 muzzle = eyePos.add(look.scale(0.85)).add(right.scale(0.22)).add(up.scale(-0.16));

        // Spawn rocket entity
        if (!level.isClientSide()) {
            Vec3 vel = look.scale(1.85); // High-velocity rocket
            PanzerfaustRocketEntity rocket = new PanzerfaustRocketEntity(ModEntities.PANZERFAUST_ROCKET, level, player, muzzle, vel);
            level.addFreshEntity(rocket);
        }

        player.getCooldowns().addCooldown(gunStack, 30);
    }

    private int countInventoryAmmo(Player player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.PANZERFAUST_ROCKET)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void consumeInventoryAmmo(Player player, int amount) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.PANZERFAUST_ROCKET)) {
                stack.shrink(amount);
                break;
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.panzerfaust.damage"));
        boolean loaded = isLoaded(stack);
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.panzerfaust.ammo", (loaded ? "§61 / 1" : "§c0 / 1")));
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.panzerfaust.controls"));
    }
}
