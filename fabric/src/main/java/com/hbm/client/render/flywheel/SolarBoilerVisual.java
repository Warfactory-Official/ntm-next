// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntitySolarBoiler;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import it.unimi.dsi.fastutil.HashCommon;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class SolarBoilerVisual extends HbmDynamicBlockEntityVisual<BlockEntitySolarBoiler>
        implements ShaderLightVisual {
    private static final int BEAM_LIMIT = 250;
    private static final Material MATERIAL =
            SimpleMaterial.builder()
                    .texture(ResourceManager.white_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.TINY)
                    .light(LightShaders.SMOOTH)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                    .writeMask(WriteMask.COLOR)
                    .backfaceCulling(false)
                    .fog(EffectVisuals.FADE)
                    .build();
    private static final Model BEAM_MODEL = new SingleMeshModel(beamMesh(), MATERIAL);
    private final ArrayList<TransformedInstance> beams = new ArrayList<>();
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private @Nullable AABB lastLightBounds;
    private long trackedTick = Long.MIN_VALUE;
    private int lastMirrorCount = -1;
    private long lastMirrorHash;
    private boolean mirrorsDirty;
    private int lastUsed;
    private boolean lastFast;

    public SolarBoilerVisual(
            VisualizationContext context, BlockEntitySolarBoiler blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        trackExtent();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static PackedQuadMesh beamMesh() {
        PackedQuadMesh.Builder mesh = PackedQuadMesh.builder(4);
        int near = 0x02FFFFFF;
        int far = 0x01FFFFFF;
        wall(mesh, 1F, 0F, 0F, .5F, .5F, .5F, -.5F, near, far);
        wall(mesh, -1F, 0F, 0F, -.5F, .5F, -.5F, -.5F, near, far);
        wall(mesh, 0F, 0F, 1F, .5F, .5F, -.5F, .5F, near, far);
        wall(mesh, 0F, 0F, -1F, .5F, -.5F, -.5F, -.5F, near, far);
        return mesh.build();
    }

    private static void wall(
            PackedQuadMesh.Builder mesh,
            float nx,
            float ny,
            float nz,
            float x0,
            float z0,
            float x1,
            float z1,
            int near,
            int far) {
        mesh.normal(nx, ny, nz)
                .vertex(x0, 0F, z0, 0F, 0F, near)
                .vertex(x1, 0F, z1, 0F, 0F, near)
                .vertex(x1, 1F, z1, 0F, 0F, far)
                .vertex(x0, 1F, z0, 0F, 0F, far);
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    @Override
    protected void trackExtent() {
        long tick = level.getGameTime();
        if (tick == trackedTick) return;
        trackedTick = tick;
        int mirrorCount = blockEntity.secondary.size();
        long mirrorHash = 0L;
        for (BlockPos mirror : blockEntity.secondary) mirrorHash += HashCommon.mix(mirror.asLong());
        if (mirrorCount == lastMirrorCount && mirrorHash == lastMirrorHash) return;
        lastMirrorCount = mirrorCount;
        lastMirrorHash = mirrorHash;
        mirrorsDirty = true;
        lastLightBounds =
                LightBounds.sections(lightSections, getRenderBoundingBox(), lastLightBounds);
        refreshVisibleBounds();
    }

    public void updateMovingParts(float partialTick) {
        boolean fast =
                Minecraft.getInstance().options.graphicsPreset().get() == GraphicsPreset.FAST;
        if (!mirrorsDirty && fast == lastFast) return;
        mirrorsDirty = false;
        lastFast = fast;
        int used = 0;
        if (!fast)
            for (BlockPos mirror : blockEntity.secondary) {
                if (used >= BEAM_LIMIT) break;
                int rx = mirror.getX() - pos.getX();
                int ry = mirror.getY() - pos.getY();
                int rz = mirror.getZ() - pos.getZ();
                double dx = -rx, dy = -ry, dz = -rz;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (distance < 1.0e-4D) continue;
                double pitch =
                        Math.toDegrees(
                                        -Math.asin(
                                                Math.max(-1D, Math.min(1D, (dy + .5D) / distance))))
                                + 90D;
                double yaw = Math.toDegrees(-Math.atan2(dz, dx)) + 180D;
                localPose
                        .identity()
                        .translate(.5F, 0F, .5F)
                        .translate(rx, ry, rz)
                        .translate(0F, 1F, 0F)
                        .rotateY(((float) yaw) * Mth.DEG_TO_RAD)
                        .rotateZ(((float) pitch) * Mth.DEG_TO_RAD)
                        .translate(0F, -1F, 0F)
                        .translate(0F, 1.0625F, 0F)
                        .scale(1F, (float) distance - 1.0625F, 1F);
                instancePose
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(localPose);
                if (used == beams.size())
                    beams.add(
                            instancerProvider()
                                    .instancer(InstanceTypes.TRANSFORMED, BEAM_MODEL)
                                    .createInstance());
                TransformedInstance beam = beams.get(used);
                beam.setVisible(true);
                beam.setTransform(instancePose).light(0).setChanged();
                used++;
            }
        for (int i = used; i < lastUsed; i++) beams.get(i).setVisible(false);
        lastUsed = used;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        AABB bounds = new AABB(pos);
        for (BlockPos mirror : blockEntity.secondary) bounds = bounds.minmax(new AABB(mirror));
        return bounds.inflate(3);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        for (var beam : beams) beam.delete();
    }
}
