package com.wolfcraft.client.model;

import com.google.common.base.Suppliers;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ObjItemModel implements ItemModel {
    private final List<BakedQuad> baseQuads;
    private final List<BakedQuad> rotatingQuads;
    private final Vector3f rotPivot;
    private final Supplier<Float> angleSupplier;
    private final Supplier<Vector3fc[]> extents;
    private final ModelRenderProperties properties;
    private final ItemTransforms transforms;

    public ObjItemModel(String objResourcePath, TextureAtlasSprite sprite, ItemTransforms transforms) {
        this(objResourcePath, null, null, null, sprite, transforms);
    }

    public ObjItemModel(String baseObjPath, String rotObjPath, Vector3f rotPivot, Supplier<Float> angleSupplier,
                        TextureAtlasSprite sprite, ItemTransforms transforms) {
        this.properties = new ModelRenderProperties(false, sprite, transforms);
        this.transforms = transforms;
        this.baseQuads = loadQuads(baseObjPath, sprite);
        if (rotObjPath != null) {
            this.rotatingQuads = loadQuads(rotObjPath, sprite);
            this.rotPivot = rotPivot != null ? rotPivot : new Vector3f(0.5f, 0.5f, 0.5f);
            this.angleSupplier = angleSupplier;
        } else {
            this.rotatingQuads = null;
            this.rotPivot = null;
            this.angleSupplier = null;
        }

        List<BakedQuad> all = new ArrayList<>(this.baseQuads);
        if (this.rotatingQuads != null) {
            all.addAll(this.rotatingQuads);
        }
        this.extents = Suppliers.memoize(() -> BlockModelWrapper.computeExtents(all));
    }

    public ItemTransforms getTransforms() {
        return this.transforms;
    }

    private static float wrap(float val) {
        if (val >= 0.0f && val <= 1.0f) {
            return val;
        }
        float wrapped = val % 1.0f;
        if (wrapped < 0.0f) wrapped += 1.0f;
        if (wrapped == 0.0f && val > 0.0f) wrapped = 1.0f;
        return wrapped;
    }

    private static long packUV(TextureAtlasSprite sprite, float rawU, float rawV) {
        float u = wrap(rawU);
        float v = wrap(rawV);
        return UVPair.pack(sprite.getU(u), sprite.getV(1.0f - v));
    }

    private static List<BakedQuad> loadQuads(String path, TextureAtlasSprite sprite) {
        List<BakedQuad> result = new ArrayList<>();
        List<Vector3f> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();

        try (InputStream in = ObjItemModel.class.getResourceAsStream(path)) {
            if (in == null) {
                System.err.println("[WolfCraft] Could not find OBJ resource: " + path);
                return result;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    String[] tokens = line.split("\\s+");
                    if (tokens[0].equals("v") && tokens.length >= 4) {
                        float x = Float.parseFloat(tokens[1]);
                        float y = Float.parseFloat(tokens[2]);
                        float z = Float.parseFloat(tokens[3]);
                        positions.add(new Vector3f(x, y, z));
                    } else if (tokens[0].equals("vt") && tokens.length >= 3) {
                        float u = Float.parseFloat(tokens[1]);
                        float v = Float.parseFloat(tokens[2]);
                        uvs.add(new float[]{u, v});
                    } else if (tokens[0].equals("f") && tokens.length >= 4) {
                        int count = tokens.length - 1;
                        int[][] vertData = new int[count][2];
                        for (int i = 0; i < count; i++) {
                            String[] parts = tokens[i + 1].split("/");
                            vertData[i][0] = Integer.parseInt(parts[0]) - 1;
                            vertData[i][1] = Integer.parseInt(parts[1]) - 1;
                        }

                        if (count == 3) {
                            addTriangle(result, positions, uvs, vertData[0], vertData[1], vertData[2], sprite);
                            addTriangle(result, positions, uvs, vertData[0], vertData[2], vertData[1], sprite);
                        } else if (count == 4) {
                            addQuad(result, positions, uvs, vertData[0], vertData[1], vertData[2], vertData[3], sprite);
                            addQuad(result, positions, uvs, vertData[0], vertData[3], vertData[2], vertData[1], sprite);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[WolfCraft] Error parsing OBJ model: " + path);
            e.printStackTrace();
        }

        return result;
    }

    private static void addTriangle(List<BakedQuad> out, List<Vector3f> pos, List<float[]> uvs,
                                    int[] v0, int[] v1, int[] v2, TextureAtlasSprite sprite) {
        Vector3f p0 = pos.get(v0[0]);
        Vector3f p1 = pos.get(v1[0]);
        Vector3f p2 = pos.get(v2[0]);

        long uvPack0 = packUV(sprite, uvs.get(v0[1])[0], uvs.get(v0[1])[1]);
        long uvPack1 = packUV(sprite, uvs.get(v1[1])[0], uvs.get(v1[1])[1]);
        long uvPack2 = packUV(sprite, uvs.get(v2[1])[0], uvs.get(v2[1])[1]);

        out.add(new BakedQuad(
                p0, p1, p2, p2,
                uvPack0, uvPack1, uvPack2, uvPack2,
                -1, Direction.UP, sprite, true, 0
        ));
    }

    private static void addQuad(List<BakedQuad> out, List<Vector3f> pos, List<float[]> uvs,
                                int[] v0, int[] v1, int[] v2, int[] v3, TextureAtlasSprite sprite) {
        Vector3f p0 = pos.get(v0[0]);
        Vector3f p1 = pos.get(v1[0]);
        Vector3f p2 = pos.get(v2[0]);
        Vector3f p3 = pos.get(v3[0]);

        long uvPack0 = packUV(sprite, uvs.get(v0[1])[0], uvs.get(v0[1])[1]);
        long uvPack1 = packUV(sprite, uvs.get(v1[1])[0], uvs.get(v1[1])[1]);
        long uvPack2 = packUV(sprite, uvs.get(v2[1])[0], uvs.get(v2[1])[1]);
        long uvPack3 = packUV(sprite, uvs.get(v3[1])[0], uvs.get(v3[1])[1]);

        out.add(new BakedQuad(
                p0, p1, p2, p3,
                uvPack0, uvPack1, uvPack2, uvPack3,
                -1, Direction.UP, sprite, true, 0
        ));
        out.add(new BakedQuad(
                p0, p3, p2, p1,
                uvPack0, uvPack3, uvPack2, uvPack1,
                -1, Direction.UP, sprite, true, 0
        ));
    }

    private static void rotateAndAddQuads(List<BakedQuad> out, List<BakedQuad> source, Vector3f pivot, float angleDeg) {
        float rad = (float) Math.toRadians(angleDeg);
        float cos = Mth.cos(rad);
        float sin = Mth.sin(rad);
        float px = pivot.x();
        float py = pivot.y();

        for (BakedQuad q : source) {
            Vector3fc p0 = q.position0();
            Vector3fc p1 = q.position1();
            Vector3fc p2 = q.position2();
            Vector3fc p3 = q.position3();

            Vector3f rp0 = rotatePoint(p0, px, py, cos, sin);
            Vector3f rp1 = rotatePoint(p1, px, py, cos, sin);
            Vector3f rp2 = rotatePoint(p2, px, py, cos, sin);
            Vector3f rp3 = rotatePoint(p3, px, py, cos, sin);

            out.add(new BakedQuad(
                    rp0, rp1, rp2, rp3,
                    q.packedUV0(), q.packedUV1(), q.packedUV2(), q.packedUV3(),
                    q.tintIndex(), q.direction(), q.sprite(), q.shade(), q.lightEmission()
            ));
        }
    }

    private static Vector3f rotatePoint(Vector3fc p, float px, float py, float cos, float sin) {
        float dx = p.x() - px;
        float dy = p.y() - py;
        return new Vector3f(px + dx * cos - dy * sin, py + dx * sin + dy * cos, p.z());
    }

    @Override
    public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, ClientLevel level, ItemOwner owner, int seed) {
        state.appendModelIdentityElement(this);
        ItemStackRenderState.LayerRenderState layer = state.newLayer();

        if (stack.hasFoil()) {
            layer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
            state.setAnimated();
            state.appendModelIdentityElement(ItemStackRenderState.FoilType.STANDARD);
        }

        layer.prepareTintLayers(0);
        layer.setExtents(this.extents);
        layer.setRenderType(Sheets.translucentItemSheet());
        this.properties.applyToLayer(layer, displayContext);

        List<BakedQuad> quads = layer.prepareQuadList();
        quads.addAll(this.baseQuads);

        if (this.rotatingQuads != null && !this.rotatingQuads.isEmpty() && this.angleSupplier != null) {
            state.setAnimated();
            float angle = this.angleSupplier.get();
            rotateAndAddQuads(quads, this.rotatingQuads, this.rotPivot, angle);
        }
    }
}
