package com.wolfcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolfcraft.entity.TeslaArcEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class TeslaArcRenderer extends EntityRenderer<TeslaArcEntity, TeslaArcRenderState> {

    public TeslaArcRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public TeslaArcRenderState createRenderState() {
        return new TeslaArcRenderState();
    }

    @Override
    public void extractRenderState(TeslaArcEntity entity, TeslaArcRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        Vec3 start = entity.position();
        Vec3 target = entity.getTargetPos();
        state.targetOffset = target.subtract(start);
        state.seed = entity.getSeed();
    }

    @Override
    public void submit(TeslaArcRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, consumer) -> {
            renderLightningArc(pose.pose(), consumer, state.targetOffset, state.seed);
        });
    }

    private void renderLightningArc(Matrix4f matrix, VertexConsumer consumer, Vec3 targetOffset, long seed) {
        double dist = targetOffset.length();
        if (dist < 0.1) return;

        RandomSource random = RandomSource.create(seed);
        int segments = Math.max(4, (int) (dist * 2.5));
        double step = 1.0 / segments;

        // Compute local coordinate frame along beam direction
        Vec3 dir = targetOffset.normalize();
        Vec3 up = Math.abs(dir.y) < 0.95 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = dir.cross(up).normalize();
        Vec3 normal = right.cross(dir).normalize();

        Vec3[] points = new Vec3[segments + 1];
        points[0] = Vec3.ZERO;
        points[segments] = targetOffset;

        for (int i = 1; i < segments; i++) {
            double t = i * step;
            Vec3 base = targetOffset.scale(t);
            // Perpendicular jagged jitter
            double jitterR = (random.nextFloat() - 0.5f) * 0.45f;
            double jitterN = (random.nextFloat() - 0.5f) * 0.45f;
            points[i] = base.add(right.scale(jitterR)).add(normal.scale(jitterN));
        }

        // Draw outer electric blue glow (thicker, translucent cyan)
        drawRibbon(matrix, consumer, points, right, normal, 0.18f, 0.25f, 0.65f, 1.0f, 0.45f);
        // Draw inner white-hot core (thin, brilliant pure white/cyan)
        drawRibbon(matrix, consumer, points, right, normal, 0.05f, 0.95f, 0.98f, 1.0f, 0.95f);

        // Fork branches
        int forks = random.nextInt(3) + 1;
        for (int f = 0; f < forks; f++) {
            int branchIdx = 1 + random.nextInt(Math.max(1, segments - 2));
            Vec3 branchStart = points[branchIdx];
            Vec3 branchDir = dir.add(right.scale((random.nextFloat() - 0.5f) * 1.5))
                               .add(normal.scale((random.nextFloat() - 0.5f) * 1.5)).normalize();
            int subSegs = 2 + random.nextInt(2);
            Vec3[] branchPoints = new Vec3[subSegs + 1];
            branchPoints[0] = branchStart;
            Vec3 curr = branchStart;
            for (int s = 1; s <= subSegs; s++) {
                curr = curr.add(branchDir.scale(0.35 + random.nextFloat() * 0.25))
                           .add(right.scale((random.nextFloat() - 0.5f) * 0.2));
                branchPoints[s] = curr;
            }
            drawRibbon(matrix, consumer, branchPoints, right, normal, 0.04f, 0.4f, 0.8f, 1.0f, 0.6f);
        }
    }

    private void drawRibbon(Matrix4f matrix, VertexConsumer consumer, Vec3[] pts, Vec3 r, Vec3 n,
                            float width, float red, float green, float blue, float alpha) {
        for (int i = 0; i < pts.length - 1; i++) {
            Vec3 p0 = pts[i];
            Vec3 p1 = pts[i + 1];

            // Plane 1 (horizontal ribbon)
            float r0x = (float) (p0.x - r.x * width);
            float r0y = (float) (p0.y - r.y * width);
            float r0z = (float) (p0.z - r.z * width);

            float r1x = (float) (p0.x + r.x * width);
            float r1y = (float) (p0.y + r.y * width);
            float r1z = (float) (p0.z + r.z * width);

            float r2x = (float) (p1.x + r.x * width);
            float r2y = (float) (p1.y + r.y * width);
            float r2z = (float) (p1.z + r.z * width);

            float r3x = (float) (p1.x - r.x * width);
            float r3y = (float) (p1.y - r.y * width);
            float r3z = (float) (p1.z - r.z * width);

            consumer.addVertex(matrix, r0x, r0y, r0z).setColor(red, green, blue, alpha);
            consumer.addVertex(matrix, r1x, r1y, r1z).setColor(red, green, blue, alpha);
            consumer.addVertex(matrix, r2x, r2y, r2z).setColor(red, green, blue, alpha);
            consumer.addVertex(matrix, r3x, r3y, r3z).setColor(red, green, blue, alpha);

            // Plane 2 (vertical ribbon)
            float n0x = (float) (p0.x - n.x * width);
            float n0y = (float) (p0.y - n.y * width);
            float n0z = (float) (p0.z - n.z * width);

            float n1x = (float) (p0.x + n.x * width);
            float n1y = (float) (p0.y + n.y * width);
            float n1z = (float) (p0.z + n.z * width);

            float n2x = (float) (p1.x + n.x * width);
            float n2y = (float) (p1.y + n.y * width);
            float n2z = (float) (p1.z + n.z * width);

            float n3x = (float) (p1.x - n.x * width);
            float n3y = (float) (p1.y - n.y * width);
            float n3z = (float) (p1.z - n.z * width);

            consumer.addVertex(matrix, n0x, n0y, n0z).setColor(red, green, blue, alpha);
            consumer.addVertex(matrix, n1x, n1y, n1z).setColor(red, green, blue, alpha);
            consumer.addVertex(matrix, n2x, n2y, n2z).setColor(red, green, blue, alpha);
            consumer.addVertex(matrix, n3x, n3y, n3z).setColor(red, green, blue, alpha);
        }
    }
}
