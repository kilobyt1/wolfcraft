package com.wolfcraft.item;

import com.wolfcraft.entity.GrenadeEntity;
import com.wolfcraft.entity.ModEntities;
import com.wolfcraft.sound.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public class GrenadeItem extends Item {
    public GrenadeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.GRENADE_THROW, SoundSource.PLAYERS, 1.0f, 0.95f + (level.random.nextFloat() * 0.1f));

        if (!level.isClientSide()) {
            Vec3 eyePos = player.getEyePosition(1.0f);
            Vec3 lookVec = player.getViewVector(1.0f);
            Vec3 spawnPos = eyePos.add(lookVec.scale(0.5));
            Vec3 velocity = lookVec.scale(1.15).add(0, 0.18, 0);

            GrenadeEntity grenade = new GrenadeEntity(ModEntities.GRENADE, level, player, spawnPos, velocity);
            level.addFreshEntity(grenade);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        player.getCooldowns().addCooldown(stack, 20);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.grenade.type"));
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.grenade.effect"));
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.grenade.controls"));
    }
}
