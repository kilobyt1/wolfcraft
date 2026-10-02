package com.wolfcraft.client;

import com.wolfcraft.WolfCraftMod;
import com.wolfcraft.client.model.ObjItemModel;
import com.wolfcraft.client.render.TeslaArcRenderer;
import com.wolfcraft.entity.ModEntities;
import com.wolfcraft.item.ModItems;
import com.wolfcraft.item.VenomItem;
import com.wolfcraft.network.ShootPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SpriteGetter;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class WolfCraftClient implements ClientModInitializer {
    private static int attackHoldCooldown = 0;

    private static float venomSpinAngle = 0.0f;
    private static float prevVenomSpinAngle = 0.0f;
    private static float venomSpinSpeed = 0.0f;

    private static float teslaRingAngle = 0.0f;
    private static float prevTeslaRingAngle = 0.0f;
    private static float teslaRingSpeed = 2.5f;

    private static ItemTransforms teslaTransforms = null;
    private static ItemTransforms venomTransforms = null;

    public static ItemTransforms getTeslaTransforms() {
        return teslaTransforms;
    }

    public static ItemTransforms getVenomTransforms() {
        return venomTransforms;
    }

    public static float getVenomSpinAngle(float partialTick) {
        return Mth.rotLerp(partialTick, prevVenomSpinAngle, venomSpinAngle);
    }

    public static float getTeslaRingAngle(float partialTick) {
        return Mth.rotLerp(partialTick, prevTeslaRingAngle, teslaRingAngle);
    }

    private static int getGunCooldown(ItemStack stack) {
        if (stack.is(ModItems.VENOM)) return 1;
        if (stack.is(ModItems.FLAMETHROWER)) return 1;
        if (stack.is(ModItems.MP40)) return 2;
        if (stack.is(ModItems.THOMPSON)) return 2;
        if (stack.is(ModItems.STEN)) return 3;
        if (stack.is(ModItems.LUGER)) return 3;
        if (stack.is(ModItems.COLT)) return 4;
        if (stack.is(ModItems.TESLA_GUN)) return 3;
        if (stack.is(ModItems.MAUSER)) return 26;
        if (stack.is(ModItems.PANZERFAUST)) return 30;
        return 2;
    }

    public static boolean isSemiAuto(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(ModItems.COLT) || stack.is(ModItems.LUGER) || stack.is(ModItems.PANZERFAUST);
    }

    @Override
    public void onInitializeClient() {
        // Register TeslaArc horizontal lightning entity renderer
        EntityRendererRegistry.register(ModEntities.TESLA_ARC, TeslaArcRenderer::new);

        // Register Panzerfaust rocket projectile renderer
        EntityRendererRegistry.register(ModEntities.PANZERFAUST_ROCKET, context -> new ThrownItemRenderer<>(context, 1.2f, true));

        // Register Grenade projectile renderer
        EntityRendererRegistry.register(ModEntities.GRENADE, context -> new ThrownItemRenderer<>(context, 1.0f, true));

        // Register ModelLoadingPlugin to substitute authentic RTCW 3D polygonal meshes
        Map<String, String[]> modelConfigs = new HashMap<>();
        // format: [objResourcePath, texturePath]
        modelConfigs.put("knife", new String[]{"/assets/wolfcraft/models/raw_obj/knife.obj", "item/knife"});
        modelConfigs.put("mp40", new String[]{"/assets/wolfcraft/models/raw_obj/mp40.obj", "item/mp40"});
        modelConfigs.put("tesla_gun", new String[]{"/assets/wolfcraft/models/raw_obj/tesla_gun.obj", "item/tesla"});
        modelConfigs.put("sten", new String[]{"/assets/wolfcraft/models/raw_obj/sten.obj", "item/sten"});
        modelConfigs.put("venom", new String[]{"/assets/wolfcraft/models/raw_obj/venom.obj", "item/venom"});
        modelConfigs.put("panzerfaust", new String[]{"/assets/wolfcraft/models/raw_obj/panzerfaust.obj", "item/panzerfaust"});
        modelConfigs.put("mauser", new String[]{"/assets/wolfcraft/models/raw_obj/mauser.obj", "item/mauser"});
        modelConfigs.put("thompson", new String[]{"/assets/wolfcraft/models/raw_obj/thompson.obj", "item/thompson"});
        modelConfigs.put("colt", new String[]{"/assets/wolfcraft/models/raw_obj/colt.obj", "item/colt"});
        modelConfigs.put("luger", new String[]{"/assets/wolfcraft/models/raw_obj/luger.obj", "item/luger"});
        modelConfigs.put("flamethrower", new String[]{"/assets/wolfcraft/models/raw_obj/flamethrower.obj", "item/flamethrower"});
        modelConfigs.put("grenade", new String[]{"/assets/wolfcraft/models/raw_obj/grenade.obj", "item/grenade"});
        modelConfigs.put("medkit_small", new String[]{"/assets/wolfcraft/models/raw_obj/medkit_small.obj", "item/medkit_small"});
        modelConfigs.put("medkit_large", new String[]{"/assets/wolfcraft/models/raw_obj/medkit_large.obj", "item/medkit_large"});
        modelConfigs.put("ammo_9mm", new String[]{"/assets/wolfcraft/models/raw_obj/ammo_9mm.obj", "item/mp40"});
        modelConfigs.put("ammo_792mm", new String[]{"/assets/wolfcraft/models/raw_obj/ammo_792mm.obj", "item/ammo_792mm"});
        modelConfigs.put("ammo_127mm", new String[]{"/assets/wolfcraft/models/raw_obj/ammo_127mm.obj", "item/ammo_127mm"});
        modelConfigs.put("ammo_45cal", new String[]{"/assets/wolfcraft/models/raw_obj/ammo_45cal.obj", "item/ammo_45cal"});
        modelConfigs.put("fuel_tank", new String[]{"/assets/wolfcraft/models/raw_obj/fuel_tank.obj", "item/fuel_tank"});
        modelConfigs.put("panzerfaust_rocket", new String[]{"/assets/wolfcraft/models/raw_obj/panzerfaust_rocket.obj", "item/panzerfaust"});
        modelConfigs.put("tesla_cell", new String[]{"/assets/wolfcraft/models/raw_obj/tesla_cell.obj", "item/tesla_cell"});

        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.modifyItemModelAfterBake().register((originalModel, context) -> {
                Identifier itemId = context.itemId();
                if (itemId != null && itemId.getNamespace().equals(WolfCraftMod.MOD_ID)) {
                    String path = itemId.getPath();
                    if (modelConfigs.containsKey(path)) {
                        String[] config = modelConfigs.get(path);
                        String objPath = config[0];
                        String texPath = config[1];

                        try {
                            Identifier modelId = Identifier.fromNamespaceAndPath(WolfCraftMod.MOD_ID, "item/" + path);
                            ResolvedModel resolved = context.bakeContext().blockModelBaker().getModel(modelId);
                            ItemTransforms transforms = resolved.getTopTransforms();

                            SpriteGetter sprites = context.bakeContext().blockModelBaker().sprites();
                            Material mat = new Material(TextureAtlas.LOCATION_ITEMS, Identifier.fromNamespaceAndPath(WolfCraftMod.MOD_ID, texPath));
                            TextureAtlasSprite sprite = sprites.get(mat, () -> "wolfcraft:" + path);

                            WolfCraftMod.LOGGER.info("Loaded authentic RTCW 3D model for: {}, sprite: {}, atlas: {}", itemId, sprite.contents().name(), sprite.atlasLocation());

                            if ("venom".equals(path)) {
                                venomTransforms = transforms;
                                return new ObjItemModel(
                                        "/assets/wolfcraft/models/raw_obj/venom.obj",
                                        "/assets/wolfcraft/models/raw_obj/venom_barrel.obj",
                                        new Vector3f(0.5860f, 0.4983f, 0.8f),
                                        () -> getVenomSpinAngle(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)),
                                        sprite, transforms
                                );
                            } else if ("tesla_gun".equals(path)) {
                                teslaTransforms = transforms;
                                return new ObjItemModel(
                                        "/assets/wolfcraft/models/raw_obj/tesla_gun.obj",
                                        "/assets/wolfcraft/models/raw_obj/tesla_ring.obj",
                                        new Vector3f(0.5000f, 0.6586f, 1.2481f),
                                        () -> getTeslaRingAngle(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)),
                                        sprite, transforms
                                );
                            }

                            return new ObjItemModel(objPath, sprite, transforms);
                        } catch (Exception e) {
                            WolfCraftMod.LOGGER.error("Failed to load RTCW 3D model for: " + itemId, e);
                        }
                    }
                }
                return originalModel;
            });
        });

        // Intercept LMB (Left Click) attack to shoot guns!
        ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
            if (player == null || client.gameMode == null) return false;
            ItemStack mainHand = player.getMainHandItem();
            if (WolfCraftMod.isGun(mainHand)) {
                if (attackHoldCooldown <= 0) {
                    ClientPlayNetworking.send(ShootPayload.INSTANCE);
                    RecoilHandler.onShoot();
                    attackHoldCooldown = getGunCooldown(mainHand);
                }
                return true; // Cancel standard Minecraft melee/block breaking
            }
            return false;
        });

        // Continuous firing when HOLDING LMB (Left Click) + recoil and weapon animation ticking!
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            RecoilHandler.clientTick();

            // Venom Gatling barrel spin ticking
            prevVenomSpinAngle = venomSpinAngle;
            if (client.player != null) {
                ItemStack main = client.player.getMainHandItem();
                if (main.is(ModItems.VENOM)) {
                    int spin = VenomItem.getSpin(main);
                    if (client.options.keyAttack.isDown() || spin > 0) {
                        venomSpinSpeed = Math.min(36.0f, venomSpinSpeed + 6.0f);
                    } else {
                        venomSpinSpeed = Math.max(0.0f, venomSpinSpeed - 3.0f);
                    }
                } else {
                    venomSpinSpeed = Math.max(0.0f, venomSpinSpeed - 4.0f);
                }
            } else {
                venomSpinSpeed = 0.0f;
            }
            venomSpinAngle = (venomSpinAngle + venomSpinSpeed) % 360.0f;

            // Tesla Gun front ring rotation ticking
            prevTeslaRingAngle = teslaRingAngle;
            if (client.player != null && client.player.getMainHandItem().is(ModItems.TESLA_GUN)) {
                if (client.options.keyAttack.isDown()) {
                    teslaRingSpeed = Math.min(22.0f, teslaRingSpeed + 3.0f);
                } else {
                    teslaRingSpeed = Math.max(2.5f, teslaRingSpeed - 1.5f);
                }
            } else {
                teslaRingSpeed = 2.0f;
            }
            teslaRingAngle = (teslaRingAngle + teslaRingSpeed) % 360.0f;

            // Handle continuous shooting cadence
            if (client.player == null || client.gameMode == null || client.level == null) return;
            ItemStack mainHand = client.player.getMainHandItem();
            if (WolfCraftMod.isGun(mainHand)) {
                // Suppress vanilla attack swing state so the arm never chops like breaking blocks
                client.player.swingTime = 0;
                client.player.swinging = false;
                client.player.attackAnim = 0.0f;
                client.player.oAttackAnim = 0.0f;

                if (client.options.keyAttack.isDown()) {
                    if (!isSemiAuto(mainHand)) {
                        if (attackHoldCooldown <= 0) {
                            ClientPlayNetworking.send(ShootPayload.INSTANCE);
                            RecoilHandler.onShoot();
                            attackHoldCooldown = getGunCooldown(mainHand);
                        } else {
                            attackHoldCooldown--;
                        }
                    } else if (attackHoldCooldown > 0) {
                        attackHoldCooldown--;
                    }
                } else if (attackHoldCooldown > 0) {
                    attackHoldCooldown--;
                }
            }
        });
    }
}
