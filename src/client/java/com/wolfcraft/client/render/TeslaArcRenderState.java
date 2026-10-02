package com.wolfcraft.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;

public class TeslaArcRenderState extends EntityRenderState {
    public Vec3 targetOffset = Vec3.ZERO;
    public long seed;
}
