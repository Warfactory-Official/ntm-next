// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.render.loader.GroupObject;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.platform.Transparency;
import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import java.util.*;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class ObjUnbakedGeometry implements UnbakedGeometry {

    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;
    public static final String SINGLE_SLOT = "all";
    public static final String OVERLAY_SLOT = "overlay";
    public static final float RAW_OFFSET_Y = 0.5F;
    private static final int FULL_BRIGHT = 15;

    private static final float UV_EPS = 1.0e-4F;

    private static final int MAX_TILES = 64;
    private static final int CLIP_MAX = 12;
    private final @Nullable Identifier meshId;
    private final @Nullable HFRWavefrontObject model;
    private final @Nullable List<Loaded> groups;
    private final List<BoxSpec> boxes;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private final boolean overlay;
    private final int overlayTintIndex;
    private final boolean translucentBase;
    private final Map<String, String[]> skins;
    private final String[] hidden;
    private final Overrides overrides;
    private final boolean smooth;

    private final Map<String, float[]> uvScales;

    public ObjUnbakedGeometry(
            Identifier objId,
            Map<String, String[]> skins,
            String[] hidden,
            float offsetX,
            float offsetY,
            float offsetZ,
            boolean overlay,
            int overlayTintIndex,
            boolean translucentBase,
            Overrides overrides,
            boolean smooth,
            Map<String, float[]> uvScales) {
        this.meshId = objId;
        this.model = Meshes.load(objId);
        this.groups = null;
        this.boxes = List.of();
        this.skins = skins;
        this.hidden = hidden;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.overlay = overlay;
        this.overlayTintIndex = overlayTintIndex;
        this.translucentBase = translucentBase;
        this.overrides = overrides;
        this.smooth = smooth;
        this.uvScales = uvScales;
    }

    public ObjUnbakedGeometry(
            @Nullable Identifier ownMesh,
            List<GroupSpec> groups,
            List<BoxSpec> boxes,
            String[] hidden,
            float offsetX,
            float offsetY,
            float offsetZ,
            Map<String, float[]> uvScales,
            Overrides overrides) {
        Map<Identifier, HFRWavefrontObject> meshes = new LinkedHashMap<>();
        List<Loaded> loaded = new ArrayList<>(groups.size());
        for (GroupSpec spec : groups) {
            HFRWavefrontObject mesh = meshes.computeIfAbsent(spec.mesh(), Meshes::load);
            for (String part : spec.parts()) {
                if (!hasGroup(mesh, part)) {
                    throw new IllegalStateException(
                            spec.mesh()
                                    + " has no group '"
                                    + part
                                    + "'; it has "
                                    + mesh.getGroupNames());
                }
            }
            loaded.add(
                    new Loaded(
                            mesh,
                            spec.parts(),
                            spec.slot(),
                            spec.transform(),
                            spec.smooth(),
                            spec.color()));
        }
        this.meshId = ownMesh;
        this.model = ownMesh == null ? null : meshes.computeIfAbsent(ownMesh, Meshes::load);
        this.groups = List.copyOf(loaded);
        this.boxes = List.copyOf(boxes);
        this.skins = Map.of();
        this.hidden = hidden;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.overlay = false;
        this.overlayTintIndex = -1;
        this.translucentBase = false;
        this.overrides = overrides;
        this.smooth = true;
        this.uvScales = uvScales;
    }

    private static boolean hasGroup(HFRWavefrontObject mesh, String name) {
        for (GroupObject group : mesh.groups) if (group.name.equals(name)) return true;
        return false;
    }

    static QuadCollection bakeGroups(
            HFRWavefrontObject model,
            String[] parts,
            Material.Baked material,
            ModelState state,
            float offsetX,
            float offsetY,
            float offsetZ,
            boolean translucentBase) {
        return bakeGroups(
                model,
                parts,
                material,
                state,
                offsetX,
                offsetY,
                offsetZ,
                translucentBase,
                true,
                Overrides.NONE);
    }

    static QuadCollection bakeGroups(
            HFRWavefrontObject model,
            String[] parts,
            Material.Baked material,
            ModelState state,
            float offsetX,
            float offsetY,
            float offsetZ,
            boolean translucentBase,
            boolean smoothing) {
        return bakeGroups(
                model,
                parts,
                material,
                state,
                offsetX,
                offsetY,
                offsetZ,
                translucentBase,
                smoothing,
                Overrides.NONE);
    }

    static QuadCollection bakeGroups(
            HFRWavefrontObject model,
            String[] parts,
            Material.Baked material,
            ModelState state,
            float offsetX,
            float offsetY,
            float offsetZ,
            boolean translucentBase,
            boolean smoothing,
            Overrides overrides) {
        return bakeGroups(
                model,
                parts,
                material,
                state,
                offsetX,
                offsetY,
                offsetZ,
                translucentBase,
                smoothing,
                overrides,
                spriteScale(material.sprite()));
    }

    static QuadCollection bakeGroups(
            HFRWavefrontObject model,
            String[] parts,
            Material.Baked material,
            ModelState state,
            float offsetX,
            float offsetY,
            float offsetZ,
            boolean translucentBase,
            boolean smoothing,
            Overrides overrides,
            float[] uv) {
        TextureAtlasSprite sprite = material.sprite();
        Transparency baseTransparency =
                translucentBase ? Transparency.TRANSLUCENT : spriteTransparency(material);
        BakedQuad.MaterialInfo info = meshInfo(material, baseTransparency, -1, 0);
        BakedQuad.MaterialInfo solidInfo =
                overrides.blackout() == null ? info : meshInfo(material, Transparency.NONE, -1, 0);
        return bakeGroups(
                model, parts, sprite, info, solidInfo, null, null, state, offsetX, offsetY, offsetZ,
                smoothing, overrides, uv, uv);
    }

    private static BakedQuad.MaterialInfo meshInfo(
            Material.Baked material, Transparency transparency, int tintIndex, int lightEmission) {

        return QuadLighting.ownBlock(
                BakedQuad.MaterialInfo.of(
                        material, transparency, tintIndex, true, lightEmission, false));
    }

    private static BakedQuad.MaterialInfo fullBright(BakedQuad.MaterialInfo info) {

        var result =
                new BakedQuad.MaterialInfo(
                        info.sprite(),
                        info.layer(),
                        info.itemRenderType(),
                        info.tintIndex(),
                        info.shade(),
                        FULL_BRIGHT,
                        info.ambientOcclusion());

        result.hbm$setLightOrigin(info.hbm$lightOrigin());
        return result;
    }

    private static Transparency spriteTransparency(Material.Baked material) {
        return material.sprite().transparency().hasTransparent()
                ? Transparency.TRANSPARENT
                : Transparency.NONE;
    }

    public static QuadCollection bakeGroups(
            List<Group> groups, float offsetX, float offsetY, float offsetZ) {
        return bakeGroups(groups, List.of(), offsetX, offsetY, offsetZ);
    }

    public static QuadCollection bakeGroups(
            List<Group> groups,
            List<BoxQuad> boxQuads,
            float offsetX,
            float offsetY,
            float offsetZ) {
        return bakeGroups(groups, boxQuads, offsetX, offsetY, offsetZ, Overrides.NONE);
    }

    static QuadCollection bakeGroups(
            List<Group> groups,
            List<BoxQuad> boxQuads,
            float offsetX,
            float offsetY,
            float offsetZ,
            Overrides overrides) {

        QuadCollection.Builder builder = new QuadCollection.Builder();

        QuadScratch scratch = new QuadScratch();
        for (Group g : groups) {
            Matrix4fc matrix = g.state().transformation().getMatrix();
            Matrix3f normalMatrix = matrix.normal(new Matrix3f());
            TextureAtlasSprite sprite = g.material().sprite();
            BakedQuad.MaterialInfo lit = meshInfo(g.material(), g.transparency(), -1, 0);
            for (GroupObject group : g.model().groups) {
                if (g.parts() != null && !contains(g.parts(), group.name)) continue;
                BakedQuad.MaterialInfo info =
                        overrides.isEmissive(group.name) ? fullBright(lit) : lit;
                boolean tiled = overrides.isTiled(group.name);
                boolean twoSided = overrides.isDoubleSided(group.name);
                float[] q = g.smooth() ? group.quads(true) : group.flatShadedQuads();
                for (int base = 0; base < q.length; base += GroupObject.QUAD) {
                    float[][] cut = tiled ? tileFace(q, base) : null;
                    int faces = cut == null ? 1 : cut.length;
                    for (int f = 0; f < faces; f++) {
                        float[] src = cut == null ? q : cut[f];
                        int at = cut == null ? base : 0;
                        float[] back = twoSided ? reversed(src, at) : null;

                        builder.addUnculledFace(
                                makeQuad(
                                        scratch,
                                        src,
                                        at,
                                        sprite,
                                        info,
                                        matrix,
                                        normalMatrix,
                                        offsetX,
                                        offsetY,
                                        offsetZ,
                                        g.uScale(),
                                        g.vScale(),
                                        g.color()));
                        if (back != null)
                            builder.addUnculledFace(
                                    makeQuad(
                                            scratch,
                                            back,
                                            0,
                                            sprite,
                                            info,
                                            matrix,
                                            normalMatrix,
                                            offsetX,
                                            offsetY,
                                            offsetZ,
                                            g.uScale(),
                                            g.vScale(),
                                            g.color()));
                    }
                }
            }
        }
        for (BoxQuad box : boxQuads) {

            BakedQuad q = box.quad();
            BakedQuad tinted =
                    box.color() == WHITE && box.origin() == QuadLighting.VANILLA
                            ? q
                            : new BakedQuad(
                                    q.position0(),
                                    q.position1(),
                                    q.position2(),
                                    q.position3(),
                                    q.packedUV0(),
                                    q.packedUV1(),
                                    q.packedUV2(),
                                    q.packedUV3(),
                                    q.direction(),
                                    box.origin() == QuadLighting.VANILLA
                                            ? q.materialInfo()
                                            : QuadLighting.copy(
                                                    q.materialInfo(),
                                                    box.origin(),
                                                    box.ambientOcclusion()),
                                    q.bakedNormals(),
                                    box.color() == WHITE
                                            ? q.bakedColors()
                                            : BakedColors.of(box.color()));
            if (box.cull() == null) builder.addUnculledFace(tinted);
            else builder.addCulledFace(box.cull(), tinted);
        }

        return builder.build();
    }

    public static QuadCollection bakeGroupsWithOverlay(
            HFRWavefrontObject model,
            String[] parts,
            Material.Baked material,
            Material.Baked overlayMaterial,
            ModelState state,
            float offsetX,
            float offsetY,
            float offsetZ,
            int overlayTintIndex) {
        return bakeGroupsWithOverlay(
                model,
                parts,
                material,
                overlayMaterial,
                state,
                offsetX,
                offsetY,
                offsetZ,
                overlayTintIndex,
                Overrides.NONE,
                spriteScale(material.sprite()),
                spriteScale(overlayMaterial.sprite()));
    }

    static QuadCollection bakeGroupsWithOverlay(
            HFRWavefrontObject model,
            String[] parts,
            Material.Baked material,
            Material.Baked overlayMaterial,
            ModelState state,
            float offsetX,
            float offsetY,
            float offsetZ,
            int overlayTintIndex,
            Overrides overrides,
            float[] uv,
            float[] overlayUv) {
        TextureAtlasSprite sprite = material.sprite();
        BakedQuad.MaterialInfo info = meshInfo(material, Transparency.NONE, -1, 0);
        TextureAtlasSprite overlaySprite = overlayMaterial.sprite();
        BakedQuad.MaterialInfo overlayInfo =
                meshInfo(overlayMaterial, Transparency.TRANSPARENT, overlayTintIndex, 0);
        return bakeGroups(
                model,
                parts,
                sprite,
                info,
                info,
                overlaySprite,
                overlayInfo,
                state,
                offsetX,
                offsetY,
                offsetZ,
                true,
                overrides,
                uv,
                overlayUv);
    }

    private static QuadCollection bakeGroups(
            HFRWavefrontObject model,
            String[] parts,
            TextureAtlasSprite sprite,
            BakedQuad.MaterialInfo info,
            BakedQuad.MaterialInfo solidInfo,
            TextureAtlasSprite overlaySprite,
            BakedQuad.MaterialInfo overlayInfo,
            ModelState state,
            float offsetX,
            float offsetY,
            float offsetZ,
            boolean smoothing,
            Overrides overrides,
            float[] uv,
            float[] overlayUv) {
        Matrix4fc matrix = state.transformation().getMatrix();
        Matrix3f normalMatrix = matrix.normal(new Matrix3f());
        BakedQuad.MaterialInfo emissiveInfo =
                overrides.emissive() == null ? info : fullBright(info);

        QuadCollection.Builder builder = new QuadCollection.Builder();

        QuadScratch scratch = new QuadScratch();
        for (GroupObject group : model.groups) {
            if (parts != null && !contains(parts, group.name)) continue;
            boolean blackout = overrides.isBlackout(group.name);
            BakedQuad.MaterialInfo groupInfo =
                    blackout ? solidInfo : overrides.isEmissive(group.name) ? emissiveInfo : info;
            int groupColor = blackout ? BLACK : WHITE;
            boolean tiled = overrides.isTiled(group.name);
            boolean twoSided = overrides.isDoubleSided(group.name);
            float[] q = smoothing ? group.quads(true) : group.flatShadedQuads();
            for (int base = 0; base < q.length; base += GroupObject.QUAD) {
                float[][] cut = tiled ? tileFace(q, base) : null;
                int faces = cut == null ? 1 : cut.length;
                for (int f = 0; f < faces; f++) {
                    float[] src = cut == null ? q : cut[f];
                    int at = cut == null ? base : 0;
                    float[] back = twoSided ? reversed(src, at) : null;

                    builder.addUnculledFace(
                            makeQuad(
                                    scratch,
                                    src,
                                    at,
                                    sprite,
                                    groupInfo,
                                    matrix,
                                    normalMatrix,
                                    offsetX,
                                    offsetY,
                                    offsetZ,
                                    uv[0],
                                    uv[1],
                                    groupColor));
                    if (back != null)
                        builder.addUnculledFace(
                                makeQuad(
                                        scratch,
                                        back,
                                        0,
                                        sprite,
                                        groupInfo,
                                        matrix,
                                        normalMatrix,
                                        offsetX,
                                        offsetY,
                                        offsetZ,
                                        uv[0],
                                        uv[1],
                                        groupColor));

                    if (overlaySprite != null) {

                        builder.addUnculledFace(
                                makeQuad(
                                        scratch,
                                        src,
                                        at,
                                        overlaySprite,
                                        overlayInfo,
                                        matrix,
                                        normalMatrix,
                                        offsetX,
                                        offsetY,
                                        offsetZ,
                                        overlayUv[0],
                                        overlayUv[1],
                                        WHITE));
                    }
                }
            }
        }

        return builder.build();
    }

    private static boolean contains(String[] names, String name) {
        for (String n : names) if (n.equals(name)) return true;
        return false;
    }

    private static float @Nullable [][] tileFace(float[] q, int base) {
        float minU = Float.MAX_VALUE, maxU = -Float.MAX_VALUE;
        float minV = Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
        for (int c = 0; c < 4; c++) {
            int v = base + c * GroupObject.STRIDE;
            minU = Math.min(minU, q[v + 3]);
            maxU = Math.max(maxU, q[v + 3]);
            minV = Math.min(minV, q[v + 4]);
            maxV = Math.max(maxV, q[v + 4]);
        }
        int u0 = Mth.floor(minU + UV_EPS), u1 = Math.max(Mth.ceil(maxU - UV_EPS), u0 + 1);
        int v0 = Mth.floor(minV + UV_EPS), v1 = Math.max(Mth.ceil(maxV - UV_EPS), v0 + 1);
        if (u0 == 0 && u1 == 1 && v0 == 0 && v1 == 1) return null;
        int cells = (u1 - u0) * (v1 - v0);
        if (cells > MAX_TILES) {
            throw new IllegalStateException(
                    "a tiled face repeats its sheet "
                            + cells
                            + " times, past the "
                            + MAX_TILES
                            + " a group may cut into; u ["
                            + minU
                            + ", "
                            + maxU
                            + "] v ["
                            + minV
                            + ", "
                            + maxV
                            + "]");
        }
        List<float[]> out = new ArrayList<>(cells);
        float[] poly = new float[CLIP_MAX * GroupObject.STRIDE];
        float[] scratch = new float[CLIP_MAX * GroupObject.STRIDE];
        for (int cu = u0; cu < u1; cu++) {
            for (int cv = v0; cv < v1; cv++) {
                System.arraycopy(q, base, poly, 0, GroupObject.QUAD);
                int n = clipUv(poly, 4, scratch, 3, cu, true);
                if (n >= 3) n = clipUv(poly, n, scratch, 3, cu + 1F, false);
                if (n >= 3) n = clipUv(poly, n, scratch, 4, cv, true);
                if (n >= 3) n = clipUv(poly, n, scratch, 4, cv + 1F, false);
                if (n < 3) continue;
                for (int i = 0; i < n; i++) {
                    poly[i * GroupObject.STRIDE + 3] -= cu;
                    poly[i * GroupObject.STRIDE + 4] -= cv;
                }
                for (int i = 1; i + 1 < n; i += 2) {
                    float[] face = new float[GroupObject.QUAD];
                    corner(face, 0, poly, 0);
                    corner(face, 1, poly, i);
                    corner(face, 2, poly, i + 1);
                    corner(face, 3, poly, i + 2 < n ? i + 2 : i + 1);
                    out.add(face);
                }
            }
        }
        return out.toArray(float[][]::new);
    }

    private static int clipUv(
            float[] poly, int n, float[] scratch, int axis, float bound, boolean keepAbove) {
        int m = 0;
        for (int i = 0; i < n; i++) {
            int a = i * GroupObject.STRIDE, b = (i + 1) % n * GroupObject.STRIDE;
            float da = keepAbove ? poly[a + axis] - bound : bound - poly[a + axis];
            float db = keepAbove ? poly[b + axis] - bound : bound - poly[b + axis];
            if (da >= 0) {
                System.arraycopy(poly, a, scratch, m * GroupObject.STRIDE, GroupObject.STRIDE);
                m++;
            }
            if (da >= 0 != db >= 0) {
                float t = da / (da - db);
                for (int k = 0; k < GroupObject.STRIDE; k++) {
                    scratch[m * GroupObject.STRIDE + k] =
                            poly[a + k] + (poly[b + k] - poly[a + k]) * t;
                }
                m++;
            }
        }
        System.arraycopy(scratch, 0, poly, 0, m * GroupObject.STRIDE);
        return m;
    }

    private static void corner(float[] face, int to, float[] poly, int from) {
        System.arraycopy(
                poly, from * GroupObject.STRIDE, face, to * GroupObject.STRIDE, GroupObject.STRIDE);
    }

    private static float[] reversed(float[] q, int base) {
        float[] face = new float[GroupObject.QUAD];
        for (int i = 0; i < 4; i++) {
            System.arraycopy(
                    q,
                    base + (i == 3 ? 3 : 2 - i) * GroupObject.STRIDE,
                    face,
                    i * GroupObject.STRIDE,
                    GroupObject.STRIDE);
        }
        return face;
    }

    private static Vector3f[] vertices() {
        return new Vector3f[] {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
    }

    private static void transformPositions(
            float[] q,
            int base,
            Matrix4fc matrix,
            float offsetX,
            float offsetY,
            float offsetZ,
            Vector3f[] pos) {
        for (int i = 0; i < 4; i++) {
            int v = base + i * GroupObject.STRIDE;
            Vector3f p =
                    pos[i].set(
                            q[v] + offsetX + 0.5F, q[v + 1] + offsetY, q[v + 2] + offsetZ + 0.5F);
            p.sub(0.5F, 0.5F, 0.5F);
            matrix.transformPosition(p);
            p.add(0.5F, 0.5F, 0.5F);
        }
    }

    private static void transformNormals(
            float[] q, int base, Matrix3f normalMatrix, Direction direction, Vector3f[] normals) {
        for (int i = 0; i < 4; i++) {
            int v = base + i * GroupObject.STRIDE;
            Vector3f transformed =
                    normalMatrix.transform(normals[i].set(q[v + 5], q[v + 6], q[v + 7]));

            if (transformed.lengthSquared() < 1e-12F) {
                normalMatrix.transform(transformed.set(direction.getUnitVec3f()));
            }
            transformed.normalize();
        }
    }

    private static long bakeUv(
            float[] q, int vertex, TextureAtlasSprite sprite, float uScale, float vScale) {
        return UVPair.pack(
                sprite.getU(q[vertex + 3] * uScale), sprite.getV(q[vertex + 4] * vScale));
    }

    private static float[] spriteScale(TextureAtlasSprite sprite) {
        return new float[] {uScale(sprite), vScale(sprite)};
    }

    private static float uScale(TextureAtlasSprite sprite) {
        return sprite.contents() instanceof PaddedSpriteContents padded ? padded.uScale() : 1F;
    }

    private static float vScale(TextureAtlasSprite sprite) {
        return sprite.contents() instanceof PaddedSpriteContents padded ? padded.vScale() : 1F;
    }

    private static Direction direction(Vector3f[] pos, Vector3f normal, Vector3f edge) {
        normal.set(pos[1]).sub(pos[0]).cross(edge.set(pos[2]).sub(pos[0]));
        return Direction.getApproximateNearest(normal.x(), normal.y(), normal.z());
    }

    private static final class QuadScratch {
        final Vector3f[] positions = vertices();
        final Vector3f[] normals = vertices();
        final Vector3f faceNormal = new Vector3f();
        final Vector3f edge = new Vector3f();
    }

    private static BakedQuad makeQuad(
            QuadScratch scratch,
            float[] q,
            int base,
            TextureAtlasSprite sprite,
            BakedQuad.MaterialInfo info,
            Matrix4fc matrix,
            Matrix3f normalMatrix,
            float offsetX,
            float offsetY,
            float offsetZ,
            float uScale,
            float vScale,
            int color) {
        Vector3f[] pos = scratch.positions;
        transformPositions(q, base, matrix, offsetX, offsetY, offsetZ, pos);
        Direction direction = direction(pos, scratch.faceNormal, scratch.edge);
        Vector3f[] normals = scratch.normals;
        transformNormals(q, base, normalMatrix, direction, normals);
        BakedNormals bakedNormals =
                BakedNormals.of(
                        BakedNormals.pack(normals[0]), BakedNormals.pack(normals[1]),
                        BakedNormals.pack(normals[2]), BakedNormals.pack(normals[3]));

        return new BakedQuad(
                new Vector3f(pos[0]),
                new Vector3f(pos[1]),
                new Vector3f(pos[2]),
                new Vector3f(pos[3]),
                bakeUv(q, base, sprite, uScale, vScale),
                bakeUv(q, base + GroupObject.STRIDE, sprite, uScale, vScale),
                bakeUv(q, base + 2 * GroupObject.STRIDE, sprite, uScale, vScale),
                bakeUv(q, base + 3 * GroupObject.STRIDE, sprite, uScale, vScale),
                direction,
                info,
                bakedNormals,
                BakedColors.of(color));
    }

    private void checkCensus(ModelDebugName name) {
        if (model == null) return;
        Map<String, String> claimedBy = new LinkedHashMap<>();
        List<String> twice = new ArrayList<>();
        if (groups != null) {
            for (Loaded group : groups) {
                if (group.model() != model) continue;
                for (String part : group.parts()) claimedBy.put(part, group.slot());
            }
        } else {
            skins.forEach(
                    (slot, parts) -> {
                        for (String part : parts) {
                            String previous = claimedBy.put(part, slot);
                            if (previous != null)
                                twice.add(part + " (" + previous + ", " + slot + ")");
                        }
                    });
        }
        List<String> unknown = new ArrayList<>();
        for (String part : claimedBy.keySet()) if (!hasGroup(model, part)) unknown.add(part);
        for (String part : hidden) {
            if (!hasGroup(model, part)) unknown.add(part);
            if (claimedBy.containsKey(part))
                twice.add(part + " (hidden and " + claimedBy.get(part) + ")");
        }
        List<String> unclaimed = new ArrayList<>();
        for (String group : new LinkedHashSet<>(model.getGroupNames())) {
            if (!claimedBy.containsKey(group) && !contains(hidden, group)) unclaimed.add(group);
        }
        if (unknown.isEmpty() && twice.isEmpty() && unclaimed.isEmpty()) return;
        throw new IllegalStateException(
                name.debugName()
                        + " does not account for its mesh "
                        + meshId
                        + ": "
                        + unclaimed.size()
                        + " name(s) neither drawn nor hidden "
                        + unclaimed
                        + ", "
                        + unknown.size()
                        + " name(s) the mesh does not have "
                        + unknown
                        + ", "
                        + twice.size()
                        + " claimed twice "
                        + twice
                        + ". The mesh has "
                        + new LinkedHashSet<>(model.getGroupNames()));
    }

    @Override
    public QuadCollection bake(
            TextureSlots slots, ModelBaker baker, ModelState state, ModelDebugName name) {
        checkCensus(name);
        if (groups != null) return bakeSpecs(slots, baker, state, name);
        if (skins.size() == 1) {
            Map.Entry<String, String[]> only = skins.entrySet().iterator().next();
            Material.Baked material = baker.materials().resolveSlot(slots, only.getKey(), name);
            float[] uv = scaleFor(only.getKey(), material, name);
            if (overlay) {
                Material.Baked overlayMaterial =
                        baker.materials().resolveSlot(slots, OVERLAY_SLOT, name);
                return bakeGroupsWithOverlay(
                        model,
                        only.getValue(),
                        material,
                        overlayMaterial,
                        state,
                        offsetX,
                        offsetY,
                        offsetZ,
                        overlayTintIndex,
                        overrides,
                        uv,
                        scaleFor(OVERLAY_SLOT, overlayMaterial, name));
            }
            return bakeGroups(
                    model,
                    only.getValue(),
                    material,
                    state,
                    offsetX,
                    offsetY,
                    offsetZ,
                    translucentBase,
                    smooth,
                    overrides,
                    uv);
        }
        List<Group> slotted = new ArrayList<>();
        for (Map.Entry<String, String[]> skin : skins.entrySet()) {
            Material.Baked material = baker.materials().resolveSlot(slots, skin.getKey(), name);
            float[] uv = scaleFor(skin.getKey(), material, name);
            slotted.add(
                    new Group(
                            model,
                            skin.getValue(),
                            material,
                            state,
                            spriteTransparency(material),
                            smooth,
                            uv[0],
                            uv[1],
                            WHITE));
        }
        return bakeGroups(slotted, List.of(), offsetX, offsetY, offsetZ, overrides);
    }

    private float[] scaleFor(String slot, Material.Baked material, ModelDebugName name) {
        float[] declared = uvScales.get(slot);
        TextureAtlasSprite sprite = material.sprite();
        if (declared == null) return new float[] {uScale(sprite), vScale(sprite)};
        if (sprite.contents() instanceof PaddedSpriteContents) {
            throw new IllegalStateException(
                    name.debugName()
                            + " declares a uv_scale for slot '"
                            + slot
                            + "', but its sprite "
                            + sprite.contents().name()
                            + " is padded at RUNTIME and already "
                            + "carries the ratio. Only a sprite nothing pads at runtime - an animated one - may declare "
                            + "it; drop the declaration or stop padding the sprite");
        }
        return declared;
    }

    private QuadCollection bakeSpecs(
            TextureSlots slots, ModelBaker baker, ModelState state, ModelDebugName name) {
        Matrix4fc outer = state.transformation().getMatrix();
        float dx = offsetX;
        float dy = offsetY - 0.5F;
        float dz = offsetZ;
        List<Group> baked = new ArrayList<>(groups.size());
        for (Loaded group : groups) {
            Material.Baked material = baker.materials().resolveSlot(slots, group.slot(), name);
            ModelState groupState =
                    group.transform() == null
                            ? state
                            : Boxes.pose(
                                    new Transformation(
                                            new Matrix4f(outer)
                                                    .translate(dx, dy, dz)
                                                    .mul(group.transform())
                                                    .translate(-dx, -dy, -dz)));
            float[] uv = scaleFor(group.slot(), material, name);
            baked.add(
                    new Group(
                            group.model(),
                            group.parts(),
                            material,
                            groupState,
                            spriteTransparency(material),
                            group.smooth(),
                            uv[0],
                            uv[1],
                            group.color()));
        }
        return bakeGroups(
                baked, bakeBoxes(slots, baker, state, name), offsetX, offsetY, offsetZ, overrides);
    }

    private List<BoxQuad> bakeBoxes(
            TextureSlots slots, ModelBaker baker, ModelState state, ModelDebugName name) {
        if (boxes.isEmpty()) return List.of();
        List<BoxQuad> quads = new ArrayList<>(boxes.size() * 6);
        for (BoxSpec box : boxes) {
            Vector3f from = box.from();
            Vector3f to = box.to();
            for (Map.Entry<Direction, String> face : box.faces().entrySet()) {
                Direction dir = face.getKey();
                Material.Baked material =
                        baker.materials().resolveSlot(slots, face.getValue(), name);
                CuboidFace.UVs uvs = box.uvs().get(dir);
                quads.add(
                        new BoxQuad(
                                Boxes.face(
                                        baker,
                                        from,
                                        to,
                                        dir,
                                        material,
                                        uvs,
                                        Quadrant.R0,
                                        CuboidFace.NO_TINT,
                                        state),
                                box.color()));
            }
        }
        return quads;
    }

    public record Overrides(
            String @Nullable [] emissive,
            String @Nullable [] tiled,
            String @Nullable [] blackout,
            String @Nullable [] doubleSided) {

        public static final Overrides NONE = new Overrides(null, null, null, null);

        public boolean isEmissive(String group) {
            return emissive != null && contains(emissive, group);
        }

        public boolean isDoubleSided(String group) {
            return doubleSided != null && contains(doubleSided, group);
        }

        public boolean isTiled(String group) {
            return tiled != null && contains(tiled, group);
        }

        public boolean isBlackout(String group) {
            return blackout != null && contains(blackout, group);
        }
    }

    public record GroupSpec(
            Identifier mesh,
            String[] parts,
            String slot,
            @Nullable Matrix4fc transform,
            boolean smooth,
            int color) {}

    private record Loaded(
            HFRWavefrontObject model,
            String[] parts,
            String slot,
            @Nullable Matrix4fc transform,
            boolean smooth,
            int color) {}

    public record BoxSpec(
            Vector3f from,
            Vector3f to,
            Map<Direction, String> faces,
            Map<Direction, CuboidFace.UVs> uvs,
            int color) {}

    public record BoxQuad(BakedQuad quad, int color, @Nullable Direction cull, int origin) {

        public BoxQuad(BakedQuad quad, int color) {
            this(quad, color, null, QuadLighting.VANILLA);
        }

        public BoxQuad(BakedQuad quad, int color, @Nullable Direction cull) {
            this(quad, color, cull, QuadLighting.VANILLA);
        }

        boolean ambientOcclusion() {
            return origin != QuadLighting.OWN_BLOCK;
        }
    }

    public record Group(
            HFRWavefrontObject model,
            String[] parts,
            Material.Baked material,
            ModelState state,
            Transparency transparency,
            boolean smooth,
            float uScale,
            float vScale,
            int color) {

        public Group(
                HFRWavefrontObject model,
                String[] parts,
                Material.Baked material,
                ModelState state) {
            this(model, parts, material, state, spriteTransparency(material), true);
        }

        public Group(
                HFRWavefrontObject model,
                String[] parts,
                Material.Baked material,
                ModelState state,
                Transparency transparency,
                boolean smooth) {
            this(
                    model,
                    parts,
                    material,
                    state,
                    transparency,
                    smooth,
                    ObjUnbakedGeometry.uScale(material.sprite()),
                    ObjUnbakedGeometry.vScale(material.sprite()),
                    WHITE);
        }

        public static Group opaque(
                HFRWavefrontObject model,
                String[] parts,
                Material.Baked material,
                ModelState state) {
            return new Group(
                    model,
                    parts,
                    material,
                    state,
                    Transparency.NONE,
                    true,
                    ObjUnbakedGeometry.uScale(material.sprite()),
                    ObjUnbakedGeometry.vScale(material.sprite()),
                    WHITE);
        }

        public static Group raw(
                HFRWavefrontObject model,
                String @Nullable [] parts,
                Material.Baked material,
                Matrix4fc blockSpace) {
            return new Group(
                    model, parts, material, blockSpacePose(blockSpace), Transparency.NONE, false);
        }

        public static Group rawCutout(
                HFRWavefrontObject model,
                String @Nullable [] parts,
                Material.Baked material,
                Matrix4fc blockSpace) {
            return new Group(
                    model,
                    parts,
                    material,
                    blockSpacePose(blockSpace),
                    spriteTransparency(material),
                    false);
        }

        public static Group rawSmooth(
                HFRWavefrontObject model,
                String @Nullable [] parts,
                Material.Baked material,
                Matrix4fc blockSpace) {
            return new Group(model, parts, material, blockSpacePose(blockSpace));
        }

        private static ModelState blockSpacePose(Matrix4fc blockSpace) {
            return Boxes.pose(
                    new Transformation(
                            new Matrix4f().translation(-0.5F, -0.5F, -0.5F).mul(blockSpace)));
        }
    }
}
