package com.wolfcraft.client.mixin;

import com.wolfcraft.client.RecoilHandler;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnimationUtils.class)
public class AnimationUtilsMixin {
    @Inject(method = "animateCrossbowHold", at = @At("TAIL"))
    private static void wolfcraft$recoilCrossbowHold(ModelPart rightArm, ModelPart leftArm, ModelPart head, boolean rightHanded, CallbackInfo ci) {
        float r = RecoilHandler.getThirdPersonRecoil();
        if (r > 0.001f) {
            // Subtle kick back of both arms toward the body
            rightArm.z += r * 1.2f;
            rightArm.xRot -= r * 0.06f;
            leftArm.z += r * 1.2f;
            leftArm.xRot -= r * 0.06f;
        }
    }
}
