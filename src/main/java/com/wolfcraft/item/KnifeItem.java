package com.wolfcraft.item;

import com.wolfcraft.sound.ModSounds;
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
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Consumer;

public class KnifeItem extends Item {
    public KnifeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = attacker.level();
        float pitch = 0.95f + attacker.getRandom().nextFloat() * 0.1f;

        if (level.isClientSide()) {
            attacker.playSound(ModSounds.KNIFE_HIT, 0.85f, pitch);
        } else {
            level.playSound(attacker instanceof Player p ? p : null, target.getX(), target.getY(), target.getZ(),
                    ModSounds.KNIFE_HIT, SoundSource.PLAYERS, 0.85f, pitch);
        }

        if (attacker instanceof Player player) {
            if (level instanceof ServerLevel serverLevel) {
                target.hurtServer(serverLevel, level.damageSources().playerAttack(player), 7.0f);
            } else {
                target.hurt(level.damageSources().playerAttack(player), 7.0f);
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getEyeY(), target.getZ(),
                    8, 0.2, 0.2, 0.2, 0.15);
        }
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack knife = player.getItemInHand(hand);
        float pitch = 0.95f + player.getRandom().nextFloat() * 0.1f;

        if (level.isClientSide()) {
            player.playSound(ModSounds.KNIFE_SLASH, 0.85f, pitch);
        } else {
            level.playSound(player, player.getX(), player.getY(), player.getZ(),
                    ModSounds.KNIFE_SLASH, SoundSource.PLAYERS, 0.85f, pitch);
        }

        player.swing(hand);
        player.getCooldowns().addCooldown(knife, 5);

        // Swift slash hit check in front (3 block range)
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);
        Vec3 reach = eyePos.add(look.scale(3.0));
        AABB aabb = player.getBoundingBox().expandTowards(look.scale(3.0)).inflate(1.0);

        LivingEntity target = null;
        double closest = 3.0;

        for (Entity ent : level.getEntities(player, aabb, e -> !e.isSpectator() && e.isAlive() && e != player)) {
            if (ent instanceof LivingEntity living) {
                AABB box = living.getBoundingBox().inflate(0.3);
                Optional<Vec3> clip = box.clip(eyePos, reach);
                if (clip.isPresent()) {
                    double dist = eyePos.distanceTo(clip.get());
                    if (dist < closest) {
                        closest = dist;
                        target = living;
                    }
                }
            }
        }

        if (target != null) {
            if (level instanceof ServerLevel serverLevel) {
                target.hurtServer(serverLevel, level.damageSources().playerAttack(player), 7.0f);
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getEyeY(), target.getZ(),
                        8, 0.2, 0.2, 0.2, 0.15);
            } else {
                target.hurt(level.damageSources().playerAttack(player), 7.0f);
            }

            if (level.isClientSide()) {
                player.playSound(ModSounds.KNIFE_HIT, 0.85f, 1.0f);
            } else {
                level.playSound(player, target.getX(), target.getY(), target.getZ(),
                        ModSounds.KNIFE_HIT, SoundSource.PLAYERS, 0.85f, 1.0f);
            }
            knife.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.knife.damage"));
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.knife.desc"));
    }
}
