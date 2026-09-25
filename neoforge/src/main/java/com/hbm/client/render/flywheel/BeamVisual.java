// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.IndexSequence;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.FogShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.material.StandardMaterialShaders;
import dev.engine_room.flywheel.lib.model.LineModelBuilder;
import dev.engine_room.flywheel.lib.model.QuadIndexSequence;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import dev.engine_room.flywheel.lib.visual.AbstractVisual;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public final class BeamVisual extends AbstractVisual {
    private static final Map<Style, Model> MODELS = new ConcurrentHashMap<>();
    private final BlockPos anchor;
    private final boolean line;
    private final int layers;
    private final float width;
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f lastBase = new Matrix4f();
    private BeamInstance instance;
    private Model model;
    private int modelCount = -1;
    private boolean live, poseValid;
    private int lastCount;
    private float beamLength;
    private double lastX, lastY, lastZ;
    private float lastSize, lastThickness;
    private int lastPhase, lastOuter, lastInner, lastOriginX, lastOriginY, lastOriginZ;
    private EnumWaveType lastWave;

    public BeamVisual(
            VisualizationContext context,
            Level level,
            BlockPos anchor,
            boolean line,
            int layers,
            float width) {
        super(context, level, 0);
        this.anchor = anchor.immutable();
        this.line = line;
        this.layers = layers;
        this.width = width;
    }

    private static Model model(Style style) {
        Material material =
                SimpleMaterial.builder()
                        .texture(ResourceManager.white_tex)
                        .shaders(
                                style.line
                                        ? StandardMaterialShaders.LINE
                                        : StandardMaterialShaders.DEFAULT)
                        .cutout(CutoutShaders.ONE_TENTH)
                        .light(LightShaders.NONE)
                        .useLight(false)
                        .useOverlay(false)
                        .ambientOcclusion(false)
                        .cardinalLightingMode(CardinalLightingMode.OFF)
                        .backfaceCulling(false)
                        .transparency(
                                style.line
                                        ? Transparency.OPAQUE
                                        : Transparency.ORDER_INDEPENDENT_ADDITIVE)
                        .writeMask(WriteMask.COLOR)
                        .fog(style.line ? FogShaders.LINEAR : EffectVisuals.FADE)
                        .build();
        if (style.line) return new SingleMeshModel(lineMesh(style.count, style.width), material);
        assert style.layers > 0;
        return new SingleMeshModel(new SolidMesh(style.count, style.layers), material);
    }

    private static Mesh lineMesh(int count, float width) {
        var builder = new LineModelBuilder(count + 1);
        for (int i = 0; i <= count; i++)
            builder.line(i, 0, i == count ? 1 : 0, i, 1, i == count ? 1 : 0);
        return MeshPart.lines(builder, width);
    }

    public void update(
            Matrix4fc base,
            Vec3 skeleton,
            EnumWaveType wave,
            int phase,
            int count,
            float size,
            float thickness,
            int outer,
            int inner) {
        update(
                base,
                skeleton.x,
                skeleton.y,
                skeleton.z,
                wave,
                phase,
                count,
                size,
                thickness,
                outer,
                inner);
    }

    public void update(
            Matrix4fc base,
            double x,
            double y,
            double z,
            EnumWaveType wave,
            int phase,
            int count,
            float size,
            float thickness,
            int outer,
            int inner) {
        double lengthSquared = x * x + y * y + z * z;
        if (count <= 0 || lengthSquared == 0) {
            hide();
            return;
        }
        var origin = renderOrigin();
        boolean poseChanged =
                !poseValid
                        || !lastBase.equals(base)
                        || lastX != x
                        || lastY != y
                        || lastZ != z
                        || lastOriginX != origin.getX()
                        || lastOriginY != origin.getY()
                        || lastOriginZ != origin.getZ();
        if (live
                && lastCount == count
                && !poseChanged
                && lastWave == wave
                && lastPhase == phase
                && lastSize == size
                && lastThickness == thickness
                && lastOuter == outer
                && lastInner == inner) return;
        if (model == null || count > modelCount || count <= modelCount / 4) {
            int capacity = Math.max(1, Integer.highestOneBit(count - 1) << 1);
            Model wanted =
                    MODELS.computeIfAbsent(
                            new Style(line, layers, width, capacity), BeamVisual::model);
            if (wanted != model) {
                if (instance != null) instance.delete();
                model = wanted;
                instance =
                        instancerProvider()
                                .instancer(line ? BeamInstance.LINE : BeamInstance.SOLID, wanted)
                                .createInstance();
            }
            modelCount = capacity;
        }
        if (poseChanged) {
            float yaw = (float) Math.atan2(x, z);
            float horizontal = (float) Math.sqrt(x * x + z * z);
            float pitch = (float) Math.atan2(y, horizontal);
            pose.translation(
                            anchor.getX() - origin.getX(),
                            anchor.getY() - origin.getY(),
                            anchor.getZ() - origin.getZ())
                    .mul(base)
                    .rotateY((float) Math.PI + yaw)
                    .rotateX(pitch - (float) Math.PI * .5F);
            beamLength = (float) Math.sqrt(lengthSquared);
            poseValid = true;
        }
        instance.update(
                pose,
                beamLength,
                size,
                thickness,
                phase,
                wave == EnumWaveType.SPIRAL ? 1 : 0,
                count,
                outer,
                inner);
        lastCount = count;
        lastBase.set(base);
        lastX = x;
        lastY = y;
        lastZ = z;
        lastWave = wave;
        lastPhase = phase;
        lastSize = size;
        lastThickness = thickness;
        lastOuter = outer;
        lastInner = inner;
        lastOriginX = origin.getX();
        lastOriginY = origin.getY();
        lastOriginZ = origin.getZ();
        live = true;
    }

    public void hide() {
        if (live) instance.setVisible(false);
        live = false;
    }

    @Override
    protected void _delete() {
        if (instance != null) instance.delete();
        instance = null;
        live = false;
    }

    private static final class SolidMesh implements Mesh {
        private static final int[] X = {1, 1, 1, 1, -1, -1, -1, -1, 1, -1, -1, 1, 1, -1, -1, 1};
        private static final int[] Y = {0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1};
        private static final int[] Z = {1, -1, -1, 1, 1, -1, -1, 1, 1, 1, 1, 1, -1, -1, -1, -1};
        private final int count, layers;
        private final Vector4f sphere;

        SolidMesh(int count, int layers) {
            this.count = count;
            this.layers = layers;
            sphere = new Vector4f(0, count * 0.5F, 0, (float) Math.sqrt(count * count * 0.25F + 2));
        }

        @Override
        public int vertexCount() {
            return count * layers * 16;
        }

        @Override
        public int indexCount() {
            return vertexCount() / 4 * 6;
        }

        @Override
        public IndexSequence indexSequence() {
            return QuadIndexSequence.INSTANCE;
        }

        @Override
        public Vector4fc boundingSphere() {
            return sphere;
        }

        @Override
        public void write(MutableVertexList vertices) {
            int at = 0;
            for (int segment = 0; segment < count; segment++)
                for (int layer = 1; layer <= layers; layer++) {
                    float radius = (float) layer / layers;
                    float fraction = layers == 1 ? 0 : (float) (layer - 1) / (layers - 1);
                    for (int corner = 0; corner < 16; corner++, at++) {
                        vertices.x(at, X[corner] * radius);
                        vertices.y(at, segment + Y[corner]);
                        vertices.z(at, Z[corner] * radius);
                        vertices.r(at, 1);
                        vertices.g(at, 1);
                        vertices.b(at, 1);
                        vertices.a(at, 1);
                        vertices.u(at, fraction);
                        vertices.v(at, segment);
                        vertices.overlay(at, OverlayTexture.NO_OVERLAY);
                        vertices.light(at, 0);
                        vertices.normalX(at, 0);
                        vertices.normalY(at, 1);
                        vertices.normalZ(at, 0);
                    }
                }
        }
    }

    private record Style(boolean line, int layers, float width, int count) {}
}
