package com.wolfcraft.item;

import com.wolfcraft.sound.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class MedkitItem extends Item {
    private final boolean isLarge;

    public MedkitItem(Properties properties, boolean isLarge) {
        super(properties);
        this.isLarge = isLarge;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getHealth() >= player.getMaxHealth()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.wolfcraft.health_full"), true);
            }
            return InteractionResult.PASS;
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.MEDKIT_USE, SoundSource.PLAYERS, 1.0f, isLarge ? 0.95f : 1.15f);

        if (!level.isClientSide()) {
            if (isLarge) {
                player.heal(20.0f); // 10 hearts
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1)); // Regen II for 10s
                player.displayClientMessage(Component.translatable("message.wolfcraft.medkit_large_used"), true);
            } else {
                player.heal(8.0f); // 4 hearts
                player.displayClientMessage(Component.translatable("message.wolfcraft.medkit_small_used"), true);
            }
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        player.getCooldowns().addCooldown(stack, isLarge ? 25 : 15);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        if (isLarge) {
            tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.medkit_large.type"));
            tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.medkit_large.effect"));
        } else {
            tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.medkit_small.type"));
            tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.medkit_small.effect"));
        }
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.medkit.controls"));
    }
}
