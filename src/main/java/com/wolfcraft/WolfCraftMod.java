package com.wolfcraft;

import com.wolfcraft.entity.ModEntities;
import com.wolfcraft.item.MauserItem;
import com.wolfcraft.item.PanzerfaustItem;
import com.wolfcraft.item.StenItem;
import com.wolfcraft.item.VenomItem;
import com.wolfcraft.item.MP40Item;
import com.wolfcraft.item.ThompsonItem;
import com.wolfcraft.item.ColtItem;
import com.wolfcraft.item.LugerItem;
import com.wolfcraft.item.FlamethrowerItem;
import com.wolfcraft.item.ModItemGroups;
import com.wolfcraft.item.ModItems;
import com.wolfcraft.item.TeslaGunItem;
import com.wolfcraft.network.ShootPayload;
import com.wolfcraft.sound.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WolfCraftMod implements ModInitializer {
    public static final String MOD_ID = "wolfcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean isGun(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(ModItems.MP40) || stack.is(ModItems.TESLA_GUN) ||
               stack.is(ModItems.STEN) || stack.is(ModItems.VENOM) ||
               stack.is(ModItems.PANZERFAUST) || stack.is(ModItems.MAUSER) ||
               stack.is(ModItems.THOMPSON) || stack.is(ModItems.COLT) ||
               stack.is(ModItems.LUGER) || stack.is(ModItems.FLAMETHROWER);
    }

    public static void spawnMuzzleFlashAndTracer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.Vec3 endPos) {
        net.minecraft.world.phys.Vec3 eyePos = player.getEyePosition(1.0f);
        net.minecraft.world.phys.Vec3 look = player.getViewVector(1.0f);
        double yawRad = Math.toRadians(player.getYRot() + 90.0f);
        net.minecraft.world.phys.Vec3 right = new net.minecraft.world.phys.Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        net.minecraft.world.phys.Vec3 up = right.cross(look).normalize();

        // First person offset: barrel tip on right side, slightly lower than eye line
        net.minecraft.world.phys.Vec3 muzzle = eyePos.add(look.scale(0.85)).add(right.scale(0.24)).add(up.scale(-0.18));

        // 1. Muzzle Flash: bright flame spark + smoke puff right at the barrel
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME, muzzle.x, muzzle.y, muzzle.z, 2, 0.02, 0.02, 0.02, 0.02);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 1, 0.01, 0.01, 0.01, 0.01);

        // 2. Straight tracer line from muzzle to hit position
        net.minecraft.world.phys.Vec3 traj = endPos.subtract(muzzle);
        double dist = traj.length();
        if (dist > 1.2) {
            net.minecraft.world.phys.Vec3 dir = traj.normalize();
            int tracerSteps = Math.min(8, (int) (dist / 3.0));
            for (int i = 1; i <= tracerSteps; i++) {
                double d = i * (dist / (tracerSteps + 1));
                net.minecraft.world.phys.Vec3 p = muzzle.add(dir.scale(d));
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Starting WolfCraft - Return to Castle Wolfenstein for Minecraft 1.21!");
        ModSounds.initialize();
        ModItems.initialize();
        ModEntities.initialize();
        ModItemGroups.initialize();

        // Register LMB shoot packet payload and server handler
        PayloadTypeRegistry.playC2S().register(ShootPayload.TYPE, ShootPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ShootPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() instanceof MP40Item mp40) {
                    mp40.shoot(player.level(), player, InteractionHand.MAIN_HAND);
                } else if (stack.getItem() instanceof TeslaGunItem tesla) {
                    tesla.shoot(player.level(), player, InteractionHand.MAIN_HAND);
                } else if (stack.getItem() instanceof StenItem sten) {
                    sten.shoot(player.level(), player, InteractionHand.MAIN_HAND);
                } else if (stack.getItem() instanceof VenomItem venom) {
                    venom.shoot(player.level(), player, InteractionHand.MAIN_HAND);
                } else if (stack.getItem() instanceof PanzerfaustItem panzerfaust) {
                    panzerfaust.shoot(player.level(), player, InteractionHand.MAIN_HAND);
                } else if (stack.getItem() instanceof MauserItem mauser) {
                    mauser.shoot(player.level(), player, InteractionHand.MAIN_HAND);
                } else if (stack.getItem() instanceof ThompsonItem thompson) {
                    thompson.shoot(player.level(), player, InteractionHand.MAIN_HAND);
                } else if (stack.getItem() instanceof ColtItem colt) {
                    colt.shoot(player.level(), player, InteractionHand.MAIN_HAND);
                } else if (stack.getItem() instanceof LugerItem luger) {
                    luger.shoot(player.level(), player, InteractionHand.MAIN_HAND);
                } else if (stack.getItem() instanceof FlamethrowerItem flame) {
                    flame.shoot(player.level(), player, InteractionHand.MAIN_HAND);
                }
            });
        });

        // Prevent guns from breaking blocks or punching mobs on LMB
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (isGun(stack)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (isGun(stack)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        LOGGER.info("WolfCraft successfully initialized!");
    }
}
