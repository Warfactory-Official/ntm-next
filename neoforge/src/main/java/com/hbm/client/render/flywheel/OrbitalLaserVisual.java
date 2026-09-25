// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.OrbitalLaserGeometry;
import com.hbm.entity.logic.EntityOrbitalLaser;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.IndexSequence;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.material.StandardMaterialShaders;
import dev.engine_room.flywheel.lib.model.QuadIndexSequence;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public final class OrbitalLaserVisual extends HbmDynamicEntityVisual<EntityOrbitalLaser> {

    private static final Material MATERIAL =
            SimpleMaterial.builder()
                    .texture(ResourceManager.white_tex)
                    .shaders(StandardMaterialShaders.DEFAULT)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.NONE)
                    .useLight(false)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .backfaceCulling(true)
                    .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                    .writeMask(WriteMask.COLOR)
                    .fog(EffectVisuals.FADE)
                    .build();
    private static final Model MODEL = new SingleMeshModel(new LaserMesh(), MATERIAL);

    private final TransformedInstance beam;
    private final Matrix4f pose = new Matrix4f();
    private final Vector3f position = new Vector3f();

    public OrbitalLaserVisual(
            VisualizationContext context, EntityOrbitalLaser entity, float partialTick) {
        super(context, entity, partialTick);
        beam = instancerProvider().instancer(InstanceTypes.TRANSFORMED, MODEL).createInstance();
        updateBeam(partialTick);
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        return sphereVisible(frustum, 125F, 125.5F);
    }

    @Override
    protected void frame(DynamicVisual.Context context) {
        updateBeam(context.partialTick());
    }

    private void updateBeam(float partialTick) {
        getVisualPosition(partialTick, position);
        beam.setTransform(pose.translation(position)).light(0).setChanged();
    }

    @Override
    protected void _delete() {
        beam.delete();
    }

    private static final class LaserMesh implements Mesh {
        private static final Vector4f BOUNDS = new Vector4f(0F, 125F, 0F, 125.5F);

        @Override
        public int vertexCount() {
            return OrbitalLaserGeometry.VERTEX_COUNT;
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
            return BOUNDS;
        }

        @Override
        public void write(MutableVertexList vertices) {
            for (int i = 0; i < OrbitalLaserGeometry.VERTEX_COUNT; i++) {
                int xyz = i * 3;
                int color = OrbitalLaserGeometry.ARGB[i];
                vertices.x(i, OrbitalLaserGeometry.XYZ[xyz]);
                vertices.y(i, OrbitalLaserGeometry.XYZ[xyz + 1]);
                vertices.z(i, OrbitalLaserGeometry.XYZ[xyz + 2]);
                vertices.r(i, ((color >>> 16) & 255) / 255F);
                vertices.g(i, ((color >>> 8) & 255) / 255F);
                vertices.b(i, (color & 255) / 255F);
                vertices.a(i, ((color >>> 24) & 255) / 255F);
                vertices.u(i, 0F);
                vertices.v(i, 0F);
                vertices.overlay(i, OverlayTexture.NO_OVERLAY);
                vertices.light(i, 0);
                vertices.normalX(i, 0);
                vertices.normalY(i, 1);
                vertices.normalZ(i, 0);
            }
        }
    }
}
