package com.wolfcraft.client;

import net.minecraft.util.Mth;

public class RecoilHandler {
    private static float currentRecoil = 0.0f;
    private static float previousRecoil = 0.0f;

    public static void onShoot() {
        currentRecoil = 1.0f;
    }

    public static void clientTick() {
        previousRecoil = currentRecoil;
        if (currentRecoil > 0.0f) {
            // Smooth snappy recoil decay
            currentRecoil = Math.max(0.0f, currentRecoil - 0.35f);
        }
    }

    public static float getRecoil(float partialTick) {
        return Mth.lerp(partialTick, previousRecoil, currentRecoil);
    }

    public static float getThirdPersonRecoil() {
        return currentRecoil;
    }
}
