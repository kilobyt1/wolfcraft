package com.wolfcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wolfcraft.item.MauserItem;
import com.wolfcraft.item.ModItems;
import com.wolfcraft.item.PanzerfaustItem;
import com.wolfcraft.item.VenomItem;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class WeaponAnimationHandler {

    public static void applyReloadAnimation(PoseStack poseStack, ItemStack stack, float cdPercent, Player player) {
        if (cdPercent <= 0.0001f || cdPercent >= 0.9999f) return;

        // cdPercent runs from 1.0 (just started cooldown) down to 0.0 (finished cooldown).
        // progress p runs from 0.0 (start) to 1.0 (finish).
        float p = 1.0f - cdPercent;

        if (stack.is(ModItems.MP40) || stack.is(ModItems.STEN)) {
            applySmgReload(poseStack, p);
        } else if (stack.is(ModItems.THOMPSON)) {
            applyThompsonReload(poseStack, p);
        } else if (stack.is(ModItems.COLT) || stack.is(ModItems.LUGER)) {
            applyPistolReload(poseStack, p);
        } else if (stack.is(ModItems.FLAMETHROWER)) {
            applyFlamethrowerReload(poseStack, p);
        } else if (stack.is(ModItems.MAUSER)) {
            applyMauserReload(poseStack, stack, p);
        } else if (stack.is(ModItems.PANZERFAUST)) {
            applyPanzerfaustReload(poseStack, p);
        } else if (stack.is(ModItems.TESLA_GUN)) {
            applyTeslaReload(poseStack, p);
        } else if (stack.is(ModItems.VENOM)) {
            applyVenomReload(poseStack, stack, p);
        }
    }

    private static void applyThompsonReload(PoseStack poseStack, float p) {
        float f = p < 0.25f ? Mth.sin(p / 0.25f * (float) (Math.PI / 2.0))
                : (p > 0.85f ? Mth.cos((p - 0.85f) / 0.15f * (float) (Math.PI / 2.0)) : 1.0f);

        poseStack.translate(-0.06 * f, -0.20 * f, 0.08 * f);
        poseStack.mulPose(Axis.XP.rotationDegrees(14.0f * f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-20.0f * f));
        poseStack.mulPose(Axis.YP.rotationDegrees(10.0f * f));

        // Heavy magazine slap bump
        if (p >= 0.46f && p <= 0.54f) {
            float bump = Mth.sin((p - 0.46f) / 0.08f * (float) Math.PI) * 0.035f;
            poseStack.translate(0, bump, -bump * 0.5);
            poseStack.mulPose(Axis.XP.rotationDegrees(-bump * 100.0f));
        }

        // Top actuator cocking pull
        if (p >= 0.70f && p <= 0.82f) {
            float pull = Mth.sin((p - 0.70f) / 0.12f * (float) Math.PI) * 0.025f;
            poseStack.translate(0, 0, pull);
        }
    }

    private static void applyPistolReload(PoseStack poseStack, float p) {
        float f = p < 0.25f ? Mth.sin(p / 0.25f * (float) (Math.PI / 2.0))
                : (p > 0.80f ? Mth.cos((p - 0.80f) / 0.20f * (float) (Math.PI / 2.0)) : 1.0f);

        // Tilt pistol up and roll left to expose grip magazine well
        poseStack.translate(-0.04 * f, -0.16 * f, 0.05 * f);
        poseStack.mulPose(Axis.XP.rotationDegrees(-18.0f * f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-25.0f * f));
        poseStack.mulPose(Axis.YP.rotationDegrees(8.0f * f));

        // Mag insert slap at 0.48 - 0.56
        if (p >= 0.48f && p <= 0.56f) {
            float bump = Mth.sin((p - 0.48f) / 0.08f * (float) Math.PI) * 0.03f;
            poseStack.translate(0, bump, 0);
        }

        // Slide release catch snap at 0.72 - 0.82
        if (p >= 0.72f && p <= 0.82f) {
            float snap = Mth.sin((p - 0.72f) / 0.10f * (float) Math.PI) * 0.02f;
            poseStack.translate(0, 0, -snap);
            poseStack.mulPose(Axis.XP.rotationDegrees(snap * 60.0f));
        }
    }

    private static void applyFlamethrowerReload(PoseStack poseStack, float p) {
        float f = p < 0.30f ? Mth.sin(p / 0.30f * (float) (Math.PI / 2.0))
                : (p > 0.75f ? Mth.cos((p - 0.75f) / 0.25f * (float) (Math.PI / 2.0)) : 1.0f);

        poseStack.translate(0.04 * f, -0.28 * f, 0.10 * f);
        poseStack.mulPose(Axis.XP.rotationDegrees(16.0f * f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-14.0f * f));

        // Canister lock clamp turn at 0.48 - 0.60
        if (p >= 0.48f && p <= 0.60f) {
            float bump = Mth.sin((p - 0.48f) / 0.12f * (float) Math.PI) * 0.035f;
            poseStack.translate(0, bump, -bump * 0.4);
            poseStack.mulPose(Axis.ZP.rotationDegrees(bump * 50.0f));
        }
    }

    private static void applySmgReload(PoseStack poseStack, float p) {
        float f;
        if (p < 0.25f) {
            f = Mth.sin(p / 0.25f * (float) (Math.PI / 2.0));
        } else if (p > 0.85f) {
            f = Mth.cos((p - 0.85f) / 0.15f * (float) (Math.PI / 2.0));
        } else {
            f = 1.0f;
        }

        // Lower weapon down and roll inward towards chest to open mag well
        poseStack.translate(-0.07 * f, -0.22 * f, 0.08 * f);
        poseStack.mulPose(Axis.XP.rotationDegrees(16.0f * f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-24.0f * f));
        poseStack.mulPose(Axis.YP.rotationDegrees(12.0f * f));

        // Magazine slap bump at p in [0.46, 0.54]
        if (p >= 0.46f && p <= 0.54f) {
            float bump = Mth.sin((p - 0.46f) / 0.08f * (float) Math.PI) * 0.035f;
            poseStack.translate(0, bump, -bump * 0.6);
            poseStack.mulPose(Axis.XP.rotationDegrees(-bump * 110.0f));
        }

        // Bolt cocking pull at p in [0.70, 0.85]
        if (p >= 0.70f && p <= 0.85f) {
            float pull = Mth.sin((p - 0.70f) / 0.15f * (float) Math.PI) * 0.025f;
            poseStack.translate(0, 0, pull);
        }
    }

    private static void applyMauserReload(PoseStack poseStack, ItemStack stack, float p) {
        int ammo = MauserItem.getLoadedAmmo(stack);

        if (ammo > 0) {
            // Bolt action cycling after a shot (26 ticks)
            if (p < 0.35f) {
                // Hand twists bolt handle up and pulls backward
                float f = Mth.sin(p / 0.35f * (float) (Math.PI / 2.0));
                poseStack.translate(0, 0.02 * f, 0.08 * f);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-9.0f * f));
                poseStack.mulPose(Axis.XP.rotationDegrees(-6.0f * f));
            } else if (p < 0.45f) {
                // Cartridge casing ejection click
                poseStack.translate(0.015, 0.02, 0.08);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-9.0f));
                poseStack.mulPose(Axis.XP.rotationDegrees(-6.0f));
            } else if (p < 0.80f) {
                // Bolt pushed forward into chamber
                float f = (0.80f - p) / 0.35f;
                poseStack.translate(0, 0.02 * f, 0.08 * f);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-9.0f * f));
                poseStack.mulPose(Axis.XP.rotationDegrees(-6.0f * f));
            } else {
                // Bolt lock down, snaps back to aim
                float f = (1.0f - p) / 0.20f;
                poseStack.mulPose(Axis.ZP.rotationDegrees(4.0f * f));
            }
        } else {
            // Stripper clip reload (loading 10 rounds from empty)
            float f = p < 0.25f ? Mth.sin(p / 0.25f * (float) (Math.PI / 2.0))
                    : (p > 0.80f ? Mth.cos((p - 0.80f) / 0.20f * (float) (Math.PI / 2.0)) : 1.0f);
            poseStack.translate(-0.05 * f, -0.25 * f, 0.06 * f);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-18.0f * f));
            poseStack.mulPose(Axis.XP.rotationDegrees(12.0f * f));

            // Stripper clip thumb press bumps
            if ((p >= 0.40f && p <= 0.48f) || (p >= 0.54f && p <= 0.62f)) {
                float subP = p < 0.50f ? (p - 0.40f) / 0.08f : (p - 0.54f) / 0.08f;
                float bump = Mth.sin(subP * (float) Math.PI) * 0.025f;
                poseStack.translate(0, -bump, 0);
            }
        }
    }

    private static void applyPanzerfaustReload(PoseStack poseStack, float p) {
        float f;
        if (p < 0.30f) {
            f = Mth.sin(p / 0.30f * (float) (Math.PI / 2.0));
        } else if (p > 0.75f) {
            f = Mth.cos((p - 0.75f) / 0.25f * (float) (Math.PI / 2.0));
        } else {
            f = 1.0f;
        }

        // Lower heavy launcher from shoulder to waist level
        poseStack.translate(0.06 * f, -0.32 * f, 0.14 * f);
        poseStack.mulPose(Axis.XP.rotationDegrees(-16.0f * f));
        poseStack.mulPose(Axis.YP.rotationDegrees(-8.0f * f));

        // Shoving new rocket into the front muzzle at p in [0.48, 0.60]
        if (p >= 0.48f && p <= 0.60f) {
            float bump = Mth.sin((p - 0.48f) / 0.12f * (float) Math.PI) * 0.06f;
            poseStack.translate(0, 0, -bump);
            poseStack.mulPose(Axis.XP.rotationDegrees(bump * 50.0f));
        }
    }

    private static void applyTeslaReload(PoseStack poseStack, float p) {
        float f;
        if (p < 0.28f) {
            f = Mth.sin(p / 0.28f * (float) (Math.PI / 2.0));
        } else if (p > 0.75f) {
            f = Mth.cos((p - 0.75f) / 0.25f * (float) (Math.PI / 2.0));
        } else {
            f = 1.0f;
        }

        // Lower and tilt side battery housing
        poseStack.translate(-0.08 * f, -0.18 * f, 0.06 * f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-20.0f * f));
        poseStack.mulPose(Axis.XP.rotationDegrees(8.0f * f));

        // Battery cell clicks into chamber at p in [0.46, 0.56]
        if (p >= 0.46f && p <= 0.56f) {
            float bump = Mth.sin((p - 0.46f) / 0.10f * (float) Math.PI) * 0.025f;
            poseStack.translate(0, bump, -bump * 0.5);
        }
    }

    private static void applyVenomReload(PoseStack poseStack, ItemStack stack, float p) {
        int heat = VenomItem.getHeat(stack);
        if (heat >= 95) {
            // Overheat lock: gun is smoking and lowered, with high-temperature vibration
            float shake = Mth.sin(System.currentTimeMillis() * 0.05f) * 0.004f;
            poseStack.translate(shake, -0.14, shake);
            poseStack.mulPose(Axis.XP.rotationDegrees(8.0f));
            return;
        }

        // Heavy 500-round ammo drum reload
        float f;
        if (p < 0.25f) {
            f = Mth.sin(p / 0.25f * (float) (Math.PI / 2.0));
        } else if (p > 0.80f) {
            f = Mth.cos((p - 0.80f) / 0.20f * (float) (Math.PI / 2.0));
        } else {
            f = 1.0f;
        }

        poseStack.translate(0.04 * f, -0.26 * f, 0.10 * f);
        poseStack.mulPose(Axis.XP.rotationDegrees(12.0f * f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-15.0f * f));

        // Heavy steel ammo drum lock clamp at p in [0.48, 0.58]
        if (p >= 0.48f && p <= 0.58f) {
            float bump = Mth.sin((p - 0.48f) / 0.10f * (float) Math.PI) * 0.04f;
            poseStack.translate(0, bump, -bump * 0.6);
            poseStack.mulPose(Axis.XP.rotationDegrees(-bump * 80.0f));
        }
    }
}
