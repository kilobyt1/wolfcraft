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

public class VenomItem extends Item {
    public static final int MAGAZINE_CAPACITY = 500;
    public static final int MAX_HEAT = 100;
    public static final int HEAT_PER_SHOT = 1;
    public static final int SPIN_THRESHOLD = 5;

    public VenomItem(Properties properties) {
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

    public static int getSpin(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            return data.copyTag().getInt("wolfcraft_spin").orElse(0);
        }
        return 0;
    }

    public static void setSpin(ItemStack stack, int spin) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("wolfcraft_spin", Math.clamp(spin, 0, 10)));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (entity instanceof Player player) {
            int spin = getSpin(stack);
            if (spin > 0) {
                setSpin(stack, spin - 1);
                if (spin == 1) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModSounds.VENOM_SPINDOWN, SoundSource.PLAYERS, 0.75f, 1.0f);
                }
            } else {
                // Barrels have stopped spinning -> gun cools down
                int heat = getHeat(stack);
                if (heat > 0) {
                    setHeat(stack, Math.max(0, heat - 1));
                }
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
            return 0xFF1100;
        } else if (heat > 35) {
            return 0xFF7700;
        }
        return 0x55FF55; // High-capacity green bar
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
                player.displayClientMessage(Component.translatable("message.wolfcraft.mag_full", "Venom", "500", "500"), true);
            }
            return InteractionResult.PASS;
        }

        if (player.getAbilities().instabuild) {
            setLoadedAmmo(stack, MAGAZINE_CAPACITY);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.VENOM_SPINDOWN, SoundSource.PLAYERS, 0.85f, 1.2f);
            player.getCooldowns().addCooldown(stack, 20);
            player.displayClientMessage(Component.translatable("message.wolfcraft.reloaded_creative", "Venom", "500", "500"), true);
            return InteractionResult.SUCCESS;
        }

        int available = countInventoryAmmo(player);
        if (available <= 0) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.wolfcraft.no_ammo", "Venom", "12.7mm"), true);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.VENOM_OVERHEAT, SoundSource.PLAYERS, 0.6f, 2.0f);
            return InteractionResult.FAIL;
        }

        // Each 12.7mm box provides 100 rounds
        int needed = MAGAZINE_CAPACITY - current;
        int boxesNeeded = (int) Math.ceil((double) needed / 100.0);
        int boxesAvailable = available;
        int boxesToUse = Math.min(boxesNeeded, boxesAvailable);

        consumeInventoryAmmo(player, boxesToUse);
        int newAmmo = Math.min(MAGAZINE_CAPACITY, current + boxesToUse * 100);
        setLoadedAmmo(stack, newAmmo);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.VENOM_SPINDOWN, SoundSource.PLAYERS, 0.85f, 1.2f);
        player.getCooldowns().addCooldown(stack, 35);
        player.displayClientMessage(Component.translatable("message.wolfcraft.reloaded", "Venom", newAmmo, "500"), true);
        return InteractionResult.SUCCESS;
    }

    public void shoot(Level level, Player player, InteractionHand hand) {
        ItemStack gunStack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(gunStack)) {
            return;
        }

        int heat = getHeat(gunStack);
        if (heat >= MAX_HEAT) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.VENOM_OVERHEAT, SoundSource.PLAYERS, 1.0f, 1.0f);
            player.displayClientMessage(Component.translatable("message.wolfcraft.overheat_barrels", "Venom"), true);
            player.getCooldowns().addCooldown(gunStack, 60);
            setSpin(gunStack, 0);
            return;
        }

        // Spin-up mechanic
        int spin = getSpin(gunStack);
        if (spin < SPIN_THRESHOLD) {
            setSpin(gunStack, spin + 2);
            if (spin == 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.VENOM_SPINUP, SoundSource.PLAYERS, 0.9f, 1.0f);
                player.displayClientMessage(Component.translatable("message.wolfcraft.spinup", "Venom"), true);
            }
            return; // Still spinning up, not firing yet!
        }
        setSpin(gunStack, 8); // Maintain full spin speed while firing

        int ammo = getLoadedAmmo(gunStack);
        if (!player.getAbilities().instabuild) {
            if (ammo <= 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.VENOM_SPINDOWN, SoundSource.PLAYERS, 0.8f, 1.5f);
                player.displayClientMessage(Component.translatable("message.wolfcraft.mag_empty", "Venom"), true);
                player.getCooldowns().addCooldown(gunStack, 10);
                setSpin(gunStack, 0);
                return;
            }
            int newAmmo = ammo - 1;
            setLoadedAmmo(gunStack, newAmmo);
            int newHeat = heat + HEAT_PER_SHOT;
            setHeat(gunStack, newHeat);

            if (newHeat >= MAX_HEAT) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.VENOM_OVERHEAT, SoundSource.PLAYERS, 1.0f, 1.0f);
                player.getCooldowns().addCooldown(gunStack, 70);
                setSpin(gunStack, 0);
                player.displayClientMessage(Component.translatable("message.wolfcraft.overheat_barrels", "Venom"), true);
                if (level instanceof ServerLevel serverLevel) {
                    Vec3 eye = player.getEyePosition();
                    serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, eye.x, eye.y, eye.z, 15, 0.3, 0.3, 0.3, 0.05);
                }
            } else {
                player.displayClientMessage(Component.translatable("message.wolfcraft.venom_ammo", newAmmo, (newHeat > 75 ? "§c" : (newHeat > 40 ? "§6" : "§a")), newHeat), true);
            }
        } else {
            int newHeat = heat + HEAT_PER_SHOT;
            setHeat(gunStack, newHeat);
            if (newHeat >= MAX_HEAT) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.VENOM_OVERHEAT, SoundSource.PLAYERS, 1.0f, 1.0f);
                player.getCooldowns().addCooldown(gunStack, 70);
                setSpin(gunStack, 0);
                player.displayClientMessage(Component.translatable("message.wolfcraft.overheat_barrels", "Venom"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.wolfcraft.infinite", "§2[Venom Gatling]"), true);
            }
        }

        // Blistering heavy Gatling sound
        float pitch = 0.95f + player.getRandom().nextFloat() * 0.12f;
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.VENOM_FIRE, SoundSource.PLAYERS, 0.85f, pitch);

        // Recoil kickback + heavy vibration + movement slowdown
        player.setXRot(player.getXRot() - (0.6f + player.getRandom().nextFloat() * 0.4f));
        player.setDeltaMovement(player.getDeltaMovement().multiply(0.85, 1.0, 0.85));
        gunStack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);

        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);


        Vec3 spread = new Vec3(
                (player.getRandom().nextDouble() - 0.5) * 0.06,
                (player.getRandom().nextDouble() - 0.5) * 0.06,
                (player.getRandom().nextDouble() - 0.5) * 0.06
        );
        Vec3 shootDir = look.add(spread).normalize();
        double range = 60.0;
        Vec3 reach = eyePos.add(shootDir.scale(range));

        BlockHitResult blockHit = level.clip(new ClipContext(
                eyePos, reach, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDist = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation().distanceTo(eyePos) : range;

        AABB aabb = player.getBoundingBox().expandTowards(shootDir.scale(maxDist)).inflate(1.5);
        LivingEntity hitTarget = null;
        double minDistance = maxDist;

        for (Entity entity : level.getEntities(player, aabb, e -> !e.isSpectator() && e.isAlive() && e != player)) {
            if (entity instanceof LivingEntity living) {
                AABB box = living.getBoundingBox().inflate(0.35);
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

        Vec3 targetPoint = hitTarget != null ? hitTarget.position().add(0, hitTarget.getEyeHeight() * 0.75, 0)
                : (blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : reach);

        if (level instanceof ServerLevel serverLevel) {
            com.wolfcraft.WolfCraftMod.spawnMuzzleFlashAndTracer(serverLevel, player, targetPoint);
        }

        if (hitTarget != null) {
            if (level instanceof ServerLevel serverLevel) {
                hitTarget.hurtServer(serverLevel, level.damageSources().playerAttack(player), 9.0f);
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        hitTarget.getX(), hitTarget.getEyeY(), hitTarget.getZ(),
                        8, 0.25, 0.25, 0.25, 0.15);
            } else {
                hitTarget.hurt(level.damageSources().playerAttack(player), 9.0f);
            }
        } else if (blockHit.getType() == HitResult.Type.BLOCK && level instanceof ServerLevel serverLevel) {
            Vec3 hitPos = blockHit.getLocation();
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    hitPos.x, hitPos.y, hitPos.z,
                    4, 0.08, 0.08, 0.08, 0.04);
        }
    }

    private int countInventoryAmmo(Player player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.AMMO_127MM)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void consumeInventoryAmmo(Player player, int amount) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.AMMO_127MM)) {
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
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.venom.damage"));
        int ammo = getLoadedAmmo(stack);
        int heat = getHeat(stack);
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.venom.ammo", ammo, heat));
        tooltipAdder.accept(Component.translatable("tooltip.wolfcraft.venom.controls"));
    }
}
