// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.storage.BlockEntityMachineFENSU;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import java.util.function.Consumer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class FENSUVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachineFENSU>
        implements ShaderLightVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.fensu;
    private static final MeshPart DISC_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("Disc")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.fensu_tex));
    private static final Material LIGHT_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT)
                    .texture(ResourceManager.fensu_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.NONE)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .ambientOcclusion(false)
                    .backfaceCulling(false)
                    .build();
    private static final MeshPart LIGHT_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Lights")], MODEL.smoothing(), LIGHT_MATERIAL);
    private final AABB bodyBounds;
    private final TransformedInstance disc;
    private final TransformedInstance lights;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastRotation = Float.NaN;

    public FENSUVisual(
            VisualizationContext context, BlockEntityMachineFENSU blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 90)
                                * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(MODEL, "Base", basePose, pos);
        disc =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, DISC_PART.model())
                        .createInstance();
        lights =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, LIGHT_PART.model())
                        .createInstance();
        lights.light(LightCoordsUtil.FULL_BRIGHT);
        lights.overlay(OverlayTexture.NO_OVERLAY);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float rotation =
                blockEntity.prevRotation
                        + (blockEntity.rotation - blockEntity.prevRotation) * partialTick;
        if (rotation == lastRotation) return;
        localPose
                .set(basePose)
                .translate(0F, 2.5F, 0F)
                .rotateX((rotation) * Mth.DEG_TO_RAD)
                .translate(0F, -2.5F, 0F);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPose);
        disc.setTransform(instancePose).light(0).setChanged();
        lights.setTransform(instancePose).setChanged();
        LightBounds.resetBounds(lightBounds, bodyBounds);
        LightBounds.includeLightBounds(lightBounds, DISC_PART.model(), localPose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastRotation = rotation;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(new AABB(pos).expandTowards(0, 3, 0).inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(disc);
    }

    @Override
    protected void _delete() {
        disc.delete();
        lights.delete();
    }
}
