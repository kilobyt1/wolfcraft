package com.wolfcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class TeslaCoronaRenderer {
    // Model-space coordinates for the 3 rotating electrodes on the front rotor
    private static final float CENTER_X = 0.5000f;
    private static final float CENTER_Y = 0.6586f;
    private static final float CENTER_Z = 1.2481f;
    private static final float RADIUS = 0.198f;

    public static void renderCorona(PoseStack poseStack, SubmitNodeCollector collector, float ringAngle) {
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, consumer) -> {
            drawCorona(pose.pose(), consumer, ringAngle);
        });
    }

    private static void drawCorona(Matrix4f matrix, VertexConsumer consumer, float ringAngle) {
        float baseAngle = ringAngle - 140.0f;
        Vec3[] electrodes = new Vec3[3];
        for (int i = 0; i < 3; i++) {
            float ang = (float) Math.toRadians(baseAngle + i * 120.0f);
            float ex = CENTER_X + RADIUS * Mth.cos(ang);
            float ey = CENTER_Y + RADIUS * Mth.sin(ang);
            float ez = CENTER_Z;
            electrodes[i] = new Vec3(ex, ey, ez);
        }

        long timeSeed = System.currentTimeMillis() / 50L;
        RandomSource random = RandomSource.create(timeSeed);

        // Draw 3 dancing high-voltage lightning arcs forming the authentic RTCW electric ring!
        drawArc(matrix, consumer, electrodes[0], electrodes[1], random.nextLong());
        drawArc(matrix, consumer, electrodes[1], electrodes[2], random.nextLong());
        drawArc(matrix, consumer, electrodes[2], electrodes[0], random.nextLong());
    }

    private static void drawArc(Matrix4f matrix, VertexConsumer consumer, Vec3 start, Vec3 end, long seed) {
        Vec3 delta = end.subtract(start);
        double dist = delta.length();
        if (dist < 0.01) return;

        RandomSource random = RandomSource.create(seed);
        int segments = 5;
        double step = 1.0 / segments;

        Vec3 dir = delta.normalize();
        Vec3 up = Math.abs(dir.y) < 0.95 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = dir.cross(up).normalize();
        Vec3 normal = right.cross(dir).normalize();

        Vec3[] points = new Vec3[segments + 1];
        points[0] = start;
        points[segments] = end;

        for (int i = 1; i < segments; i++) {
            double t = i * step;
            Vec3 base = start.add(delta.scale(t));
            double jitterR = (random.nextFloat() - 0.5f) * 0.035;
            double jitterN = (random.nextFloat() - 0.5f) * 0.035;
            points[i] = base.add(right.scale(jitterR)).add(normal.scale(jitterN));
        }

        // 1. Outer glowing electric-cyan corona ribbon
        drawRibbon(matrix, consumer, points, right, normal, 0.024f, 0.15f, 0.65f, 1.0f, 0.70f);
        // 2. Inner brilliant white-hot core
        drawRibbon(matrix, consumer, points, right, normal, 0.009f, 0.95f, 0.98f, 1.0f, 0.98f);
    }

    private static void drawRibbon(Matrix4f matrix, VertexConsumer consumer, Vec3[] pts, Vec3 r, Vec3 n,
                                  float width, float red, float green, float blue, float alpha) {
        for (int i = 0; i < pts.length - 1; i++) {
            Vec3 p0 = pts[i];
            Vec3 p1 = pts[i + 1];

            float x0a = (float) (p0.x - r.x * width);
            float y0a = (float) (p0.y - r.y * width);
            float z0a = (float) (p0.z - r.z * width);

            float x0b = (float) (p0.x + r.x * width);
            float y0b = (float) (p0.y + r.y * width);
            float z0b = (float) (p0.z + r.z * width);

            float x1a = (float) (p1.x - r.x * width);
            float y1a = (float) (p1.y - r.y * width);
            float z1a = (float) (p1.z - r.z * width);

            float x1b = (float) (p1.x + r.x * width);
            float y1b = (float) (p1.y + r.y * width);
            float z1b = (float) (p1.z + r.z * width);

            consumer.addVertex(matrix, x0a, y0a, z0a).setColor(red, green, blue, alpha);
            consumer.addVertex(matrix, x0b, y0b, z0b).setColor(red, green, blue, alpha);
            consumer.addVertex(matrix, x1b, y1b, z1b).setColor(red, green, blue, alpha);
            consumer.addVertex(matrix, x1a, y1a, z1a).setColor(red, green, blue, alpha);

            // Cross plane (normal)
            float x0c = (float) (p0.x - n.x * width);
            float y0c = (float) (p0.y - n.y * width);
            float z0c = (float) (p0.z - n.z * width);

            float x0d = (float) (p0.x + n.x * width);
            float y0d = (float) (p0.y + n.y * width);
            float z0d = (float) (p0.z + n.z * width);

            float x1c = (float) (p1.x - n.x * width);
            float y1c = (float) (p1.y - n.y * width);
            float z1c = (float) (p1.z - n.z * width);

            float x1d = (float) (p1.x + n.x * width);
            float y1d = (float) (p1.y + n.y * width);
            float z1d = (float) (p1.z + n.z * width);

            consumer.addVertex(matrix, x0c, y0c, z0c).setColor(red, green, blue, alpha);
            consumer.addVertex(matrix, x0d, y0d, z0d).setColor(red, green, blue, alpha);
            consumer.addVertex(matrix, x1d, y1d, z1d).setColor(red, green, blue, alpha);
            consumer.addVertex(matrix, x1c, y1c, z1c).setColor(red, green, blue, alpha);
        }
    }
}
