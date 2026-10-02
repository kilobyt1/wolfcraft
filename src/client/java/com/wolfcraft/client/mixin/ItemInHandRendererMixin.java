package com.wolfcraft.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wolfcraft.WolfCraftMod;
import com.wolfcraft.client.RecoilHandler;
import com.wolfcraft.client.WeaponAnimationHandler;
import com.wolfcraft.client.WolfCraftClient;
import com.wolfcraft.client.render.TeslaCoronaRenderer;
import com.wolfcraft.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    // Prevent re-equip drop animation when shooting or changing ammo/heat data on guns
    @Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("HEAD"), cancellable = true)
    private void wolfcraft$preventReequipDrop(ItemStack current, ItemStack next, CallbackInfoReturnable<Boolean> cir) {
        if (!current.isEmpty() && !next.isEmpty() && current.is(next.getItem()) && WolfCraftMod.isGun(current)) {
            cir.setReturnValue(true);
        }
    }

    // Completely cancel the vanilla attack swing (pickaxe/block-breaking swing) for guns
    @Inject(method = "applyItemArmAttackTransform", at = @At("HEAD"), cancellable = true)
    private void wolfcraft$cancelWeaponAttackSwing(PoseStack poseStack, HumanoidArm arm, float swingProgress, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && WolfCraftMod.isGun(mc.player.getMainHandItem())) {
            ci.cancel();
        }
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
    private void wolfcraft$applyRecoilAndReload(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
                                               float swingProgress, ItemStack stack, float equipProgress, PoseStack poseStack,
                                               SubmitNodeCollector collector, int packedLight, CallbackInfo ci) {
        if (!stack.isEmpty() && WolfCraftMod.isGun(stack)) {
            // 1. Snappy dynamic shooting recoil
            float r = RecoilHandler.getRecoil(partialTick);
            if (r > 0.001f) {
                poseStack.translate(0.0, r * 0.015, r * 0.05);
                poseStack.mulPose(Axis.XP.rotationDegrees(r * 2.5f));
                poseStack.mulPose(Axis.ZP.rotationDegrees(r * 0.8f));
            }

            // 2. Procedural first-person reload / bolt-cycling animation
            if (player.getCooldowns().isOnCooldown(stack)) {
                float cd = player.getCooldowns().getCooldownPercent(stack, partialTick);
                WeaponAnimationHandler.applyReloadAnimation(poseStack, stack, cd, player);
            }
        }
    }

    @Inject(method = "renderItem", at = @At("TAIL"))
    private void wolfcraft$renderTeslaCorona(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext,
                                             PoseStack poseStack, SubmitNodeCollector collector, int packedLight, CallbackInfo ci) {
        if (!stack.isEmpty() && stack.is(ModItems.TESLA_GUN)) {
            if (displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                    || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
                ItemTransforms transforms = WolfCraftClient.getTeslaTransforms();
                if (transforms != null) {
                    poseStack.pushPose();
                    transforms.getTransform(displayContext).apply(displayContext.leftHand(), poseStack.last());
                    float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
                    float ringAngle = WolfCraftClient.getTeslaRingAngle(partialTick);
                    TeslaCoronaRenderer.renderCorona(poseStack, collector, ringAngle);
                    poseStack.popPose();
                }
            }
        }
    }
}
