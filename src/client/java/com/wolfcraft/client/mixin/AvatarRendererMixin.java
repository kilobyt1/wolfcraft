package com.wolfcraft.client.mixin;

import com.wolfcraft.WolfCraftMod;
import com.wolfcraft.item.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Inject(method = "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/entity/HumanoidArm;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;", at = @At("HEAD"), cancellable = true)
    private static void wolfcraft$getGunArmPose(Avatar avatar, HumanoidArm arm,
                                               CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        ItemStack mainHand = avatar.getItemInHand(InteractionHand.MAIN_HAND);
        if (!mainHand.isEmpty() && WolfCraftMod.isGun(mainHand)) {
            cir.setReturnValue(HumanoidModel.ArmPose.CROSSBOW_HOLD);
        }
    }
}
