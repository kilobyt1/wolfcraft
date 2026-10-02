package com.wolfcraft.client.mixin;

import com.wolfcraft.item.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin<T extends HumanoidRenderState> {
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart head;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void wolfcraft$shoulderPanzerfaust(T state, CallbackInfo ci) {
        ItemStack main = state.getMainHandItemStack();
        if (main != null && !main.isEmpty() && main.is(ModItems.PANZERFAUST)) {
            boolean rightHanded = state.mainArm == HumanoidArm.RIGHT;
            ModelPart firingArm = rightHanded ? rightArm : leftArm;
            ModelPart supportArm = rightHanded ? leftArm : rightArm;

            float ySign = rightHanded ? 1.0f : -1.0f;

            // Firing arm: raised straight forward aiming with head pitch/yaw
            firingArm.xRot = -1.5707964f + head.xRot;
            firingArm.yRot = -0.1f * ySign + head.yRot;
            firingArm.zRot = 0.0f;

            // Support arm: reaches across under the launcher body
            supportArm.xRot = -1.35f + head.xRot;
            supportArm.yRot = 0.65f * ySign + head.yRot;
            supportArm.zRot = 0.0f;
        }
    }
}
