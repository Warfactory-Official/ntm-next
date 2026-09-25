// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import com.hbm.client.render.RenderTextures;
import com.hbm.handler.radiation.RadVisGeometry.Marker;
import com.hbm.handler.radiation.RadVisGeometry.Mesh;
import com.hbm.handler.radiation.RadVisGeometry.Packed;
import com.hbm.handler.radiation.RadVisGeometry.Piece;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.Effect;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.FogShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.QuadMesh;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

final class RadVisVisual implements EffectVisual<RadVisVisual.RadVisEffect>, SimpleDynamicVisual {

    private static final Material DEPTH = material(DepthTest.LEQUAL);

    private static final Material THROUGH = material(DepthTest.OFF);
    private static final Map<Marker, float[]> MARKERS = new EnumMap<>(Marker.class);

    static {
        for (Marker m : Marker.values()) MARKERS.put(m, RadVisMeshes.marker(m));
    }

    private static @Nullable RadVisEffect effect;
    private static @Nullable VisualizationManager manager;

    private final VisualizationContext context;
    private final Map<PieceKey, Entry> entries = new HashMap<>();
    private Map<ModelKey, Model> models = new HashMap<>();
    private final Matrix4f pose = new Matrix4f();
    private RadVisOverlay.@Nullable Frame last;

    private RadVisVisual(VisualizationContext context) {
        this.context = context;
    }

    private static Material material(DepthTest depthTest) {
        return SimpleMaterial.builder()
                .texture(RenderTextures.WHITE)
                .mipmap(false)
                .useLight(false)
                .useOverlay(false)
                .cardinalLightingMode(CardinalLightingMode.OFF)
                .fog(FogShaders.NONE)
                .transparency(Transparency.ORDER_INDEPENDENT)
                .writeMask(WriteMask.COLOR)
                .backfaceCulling(false)
                .depthTest(depthTest)
                .build();
    }

    static void sync(@Nullable Level level) {
        VisualizationManager next = level == null ? null : VisualizationManager.get(level);
        if (next == manager) return;
        if (manager != null && effect != null) manager.effects().queueRemove(effect);
        manager = next;
        effect = next == null ? null : new RadVisEffect(level);
        if (next != null) next.effects().queueAdd(effect);
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        RadVisOverlay.Frame frame = RadVisOverlay.frame();
        if (frame == last) return;
        last = frame;
        if (frame == null) {
            wipe();
            return;
        }
        Material material = frame.xray() ? THROUGH : DEPTH;
        Vec3i origin = context.renderOrigin();
        Map<ModelKey, Model> used = new HashMap<>();
        for (Entry e : entries.values()) e.seen = false;
        for (Piece piece : frame.pieces()) {
            ModelKey modelKey = new ModelKey(piece.mesh(), material);
            Model model =
                    used.computeIfAbsent(
                            modelKey,
                            k -> {
                                Model known = models.get(k);
                                return known != null
                                        ? known
                                        : new SingleMeshModel(quadMesh(k.mesh()), k.material());
                            });
            PieceKey key = new PieceKey(piece.section(), piece.id());
            Entry entry = entries.get(key);
            if (entry == null || entry.model != model) {
                if (entry != null) entry.instance.delete();
                entry =
                        new Entry(
                                model,
                                context.instancerProvider()
                                        .instancer(InstanceTypes.TRANSFORMED, model)
                                        .createInstance());
                entries.put(key, entry);
            }
            entry.seen = true;
            boolean changed = false;
            int x = piece.x() - origin.getX(),
                    y = piece.y() - origin.getY(),
                    z = piece.z() - origin.getZ();
            if (entry.fresh || entry.x != x || entry.y != y || entry.z != z) {
                entry.instance.setTransform(pose.translation(x, y, z));
                entry.x = x;
                entry.y = y;
                entry.z = z;
                changed = true;
            }
            if (entry.fresh || entry.argb != piece.argb()) {
                entry.instance.colorArgb(piece.argb());
                entry.argb = piece.argb();
                changed = true;
            }
            entry.fresh = false;
            if (changed) entry.instance.setChanged();
        }
        for (Iterator<Entry> it = entries.values().iterator(); it.hasNext(); ) {
            Entry e = it.next();
            if (!e.seen) {
                e.instance.delete();
                it.remove();
            }
        }
        models = used;
    }

    private static QuadMesh quadMesh(Mesh mesh) {
        return mesh instanceof Packed p
                ? new PackedMesh(p.quads(), p.from(), p.to())
                : new CornerMesh(MARKERS.get((Marker) mesh));
    }

    private void wipe() {
        for (Entry e : entries.values()) e.instance.delete();
        entries.clear();
        models = new HashMap<>();
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        wipe();
    }

    private record PieceKey(long section, int id) {}

    private record ModelKey(Mesh mesh, Material material) {}

    private static final class Entry {
        final Model model;
        final TransformedInstance instance;
        boolean seen, fresh = true;
        int x, y, z, argb;

        Entry(Model model, TransformedInstance instance) {
            this.model = model;
            this.instance = instance;
        }
    }

    private record PackedMesh(long[] quads, int from, int to) implements QuadMesh {
        private static final Vector4fc BOUNDS = new Vector4f(8f, 8f, 8f, 14f);

        @Override
        public int vertexCount() {
            return (to - from) * 4;
        }

        @Override
        public void write(MutableVertexList dst) {
            float[] c = new float[12];
            for (int q = from; q < to; q++) {
                RadVisMeshes.quadCorners(quads[q], RadVisMeshes.INSET, c);
                for (int k = 0; k < 4; k++) vertex(dst, (q - from) * 4 + k, c, k * 3);
            }
        }

        @Override
        public Vector4fc boundingSphere() {
            return BOUNDS;
        }
    }

    private record CornerMesh(float[] corners) implements QuadMesh {
        private static final Vector4fc BOUNDS = new Vector4f(0.5f, 0.5f, 0.5f, 0.87f);

        @Override
        public int vertexCount() {
            return corners.length / 3;
        }

        @Override
        public void write(MutableVertexList dst) {
            for (int i = 0; i < corners.length / 3; i++) vertex(dst, i, corners, i * 3);
        }

        @Override
        public Vector4fc boundingSphere() {
            return BOUNDS;
        }
    }

    private static void vertex(MutableVertexList dst, int i, float[] c, int at) {
        dst.x(i, c[at]);
        dst.y(i, c[at + 1]);
        dst.z(i, c[at + 2]);
        dst.r(i, 1f);
        dst.g(i, 1f);
        dst.b(i, 1f);
        dst.a(i, 1f);
        dst.u(i, 0f);
        dst.v(i, 0f);
        dst.normalX(i, 0f);
        dst.normalY(i, 1f);
        dst.normalZ(i, 0f);
    }

    static final class RadVisEffect implements Effect {
        private final Level level;

        RadVisEffect(Level level) {
            this.level = level;
        }

        @Override
        public Level level() {
            return level;
        }

        @Override
        public EffectVisual<?> visualize(VisualizationContext ctx, float partialTick) {
            return new RadVisVisual(ctx);
        }
    }
}
