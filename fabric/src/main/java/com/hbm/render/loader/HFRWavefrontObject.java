// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.loader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;

public class HFRWavefrontObject {

    public final String source;
    public final GroupObject[] groups;
    private boolean smoothing = true;

    public HFRWavefrontObject(ResourceManager resources, Identifier id) {
        this.source = id.toString();
        this.groups = ObjMeshCache.load(resources, id);
    }

    public HFRWavefrontObject(String sourceName, InputStream inputStream) {
        this.source = sourceName;
        try {
            this.groups = ObjLoader.parse(inputStream, sourceName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load OBJ " + sourceName, e);
        }
    }

    public HFRWavefrontObject(String source, List<GroupObject> groups) {
        this.source = source;
        this.groups = groups.toArray(new GroupObject[0]);
    }

    private static float[] empty() {
        return new float[] {
            Float.MAX_VALUE,
            Float.MAX_VALUE,
            Float.MAX_VALUE,
            -Float.MAX_VALUE,
            -Float.MAX_VALUE,
            -Float.MAX_VALUE
        };
    }

    private static void encapsulate(float[] box, float[] other) {
        for (int i = 0; i < 3; i++) {
            box[i] = Math.min(box[i], other[i]);
            box[i + 3] = Math.max(box[i + 3], other[i + 3]);
        }
    }

    public HFRWavefrontObject noSmooth() {
        smoothing = false;
        return this;
    }

    public HFRWavefrontObject insetUv(float epsilon) {
        List<GroupObject> inset = new ArrayList<>(groups.length);
        for (GroupObject group : groups) {
            float[] quads = group.quads(true).clone();
            for (int face = 0; face < quads.length; face += GroupObject.QUAD) {

                int corners = degenerate(quads, face) ? 3 : 4;
                float meanU = 0F;
                float meanV = 0F;
                for (int c = 0; c < corners; c++) {
                    meanU += quads[face + c * GroupObject.STRIDE + 3];
                    meanV += quads[face + c * GroupObject.STRIDE + 4];
                }
                meanU /= corners;
                meanV /= corners;
                for (int c = 0; c < 4; c++) {
                    int v = face + c * GroupObject.STRIDE;
                    quads[v + 3] += quads[v + 3] > meanU ? -epsilon : epsilon;
                    quads[v + 4] += quads[v + 4] > meanV ? -epsilon : epsilon;
                }
            }
            inset.add(new GroupObject(group.name, quads, group.faceNormals().clone()));
        }
        HFRWavefrontObject mesh = new HFRWavefrontObject(source, inset);
        mesh.smoothing = smoothing;
        return mesh;
    }

    private static boolean degenerate(float[] quads, int face) {
        int third = face + 2 * GroupObject.STRIDE;
        int fourth = face + 3 * GroupObject.STRIDE;
        for (int i = 0; i < 5; i++) if (quads[third + i] != quads[fourth + i]) return false;
        return true;
    }

    public boolean smoothing() {
        return smoothing;
    }

    public int partId(String name) {
        int found = -1;
        for (int i = 0; i < groups.length; i++) {
            if (!groups[i].name.equalsIgnoreCase(name)) continue;
            if (found >= 0) {
                throw new IllegalArgumentException(
                        "OBJ "
                                + source
                                + " declares group '"
                                + name
                                + "' more than once, so it has no single part id; it has "
                                + getGroupNames());
            }
            found = i;
        }
        if (found < 0) {
            throw new IllegalArgumentException(
                    "OBJ " + source + " has no group '" + name + "'; it has " + getGroupNames());
        }
        return found;
    }

    public int[] partIds(String... names) {
        int[] ids = new int[names.length];
        for (int i = 0; i < names.length; i++) ids[i] = partId(names[i]);
        return ids;
    }

    public int[] partIds(List<String> names) {
        int[] ids = new int[names.size()];
        for (int i = 0; i < ids.length; i++) ids[i] = partId(names.get(i));
        return ids;
    }

    public void render(PoseStack.Pose pose, VertexConsumer buffer, int light, int color) {
        for (GroupObject group : groups) group.emit(pose, buffer, light, color, smoothing);
    }

    public void render(
            PoseStack.Pose pose, VertexConsumer buffer, int light, int color, int overlay) {
        for (GroupObject group : groups) group.emit(pose, buffer, light, color, overlay, smoothing);
    }

    public void render(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int light,
            int color,
            float uScale,
            float vScale,
            float uOff,
            float vOff) {
        for (GroupObject group : groups) {
            group.emit(pose, buffer, light, color, uScale, vScale, uOff, vOff, smoothing);
        }
    }

    public void renderPart(
            PoseStack.Pose pose, VertexConsumer buffer, int light, int color, int part) {
        groups[part].emit(pose, buffer, light, color, smoothing);
    }

    public void renderPart(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int light,
            int color,
            int overlay,
            int part) {
        groups[part].emit(pose, buffer, light, color, overlay, smoothing);
    }

    public void renderPart(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int light,
            int color,
            int part,
            float uScale,
            float vScale,
            float uOff,
            float vOff) {
        groups[part].emit(pose, buffer, light, color, uScale, vScale, uOff, vOff, smoothing);
    }

    public void renderPartClipped(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int light,
            int color,
            int part,
            float nx,
            float ny,
            float nz,
            float d) {
        groups[part].emitClipped(pose, buffer, light, color, nx, ny, nz, d, smoothing);
    }

    public void renderPartClipped(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int light,
            int color,
            int part,
            float[][] planes) {
        groups[part].emitClipped(pose, buffer, light, color, smoothing, planes);
    }

    public List<String> getGroupNames() {
        List<String> names = new ArrayList<>(groups.length);
        for (GroupObject g : groups) names.add(g.name);
        return names;
    }

    public float[] getExtents() {
        float[] box = empty();
        for (GroupObject group : groups) encapsulate(box, group.bounds());
        if (box[0] > box[3]) throw new IllegalStateException("OBJ " + source + " has no faces");
        return box;
    }

    public float @Nullable [] boundsOfParts(Collection<String> parts) {
        float[] box = empty();
        for (GroupObject group : groups) {
            for (String part : parts) {
                if (part.equalsIgnoreCase(group.name)) {
                    encapsulate(box, group.bounds());
                    break;
                }
            }
        }
        return box[0] > box[3] ? null : box;
    }

    public float @Nullable [] boundsOfParts(int... ids) {
        float[] box = empty();
        for (int id : ids) encapsulate(box, groups[id].bounds());
        return box[0] > box[3] ? null : box;
    }

    public float @Nullable [] boundsOfParts(String... parts) {
        return boundsOfParts(List.of(parts));
    }

    public float[] boundsExcluding(Set<String> hidden) {
        float[] box = empty();
        for (GroupObject group : groups) {
            if (!hidden.contains(group.name)) encapsulate(box, group.bounds());
        }
        if (box[0] > box[3])
            throw new IllegalStateException("OBJ has no visible faces after exclusions");
        return box;
    }
}
