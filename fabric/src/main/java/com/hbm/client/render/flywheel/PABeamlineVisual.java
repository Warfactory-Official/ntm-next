// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.albion.BlockPABeamline;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.render.RenderTextures;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.albion.BlockEntityPABeamline;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.function.Consumer;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class PABeamlineVisual extends HbmDynamicBlockEntityVisual<BlockEntityPABeamline> {
    private static final HFRWavefrontObject MODEL = ResourceManager.pa_beamline;
    private static final Material MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(RenderTextures.WHITE)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .useLight(false)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .backfaceCulling(false)
                    .build();
    private static final Model GLASS_MODEL =
            new SingleMeshModel(
                    PackedQuadMesh.of(
                            MODEL.groups[MODEL.partId("BeamlineGlass")], MODEL.smoothing()),
                    MATERIAL);
    private final AABB bodyBounds;
    private final @Nullable TransformedInstance glass;
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f glassBasePose = new Matrix4f();
    private float lastFlash = Float.NaN;

    public PABeamlineVisual(
            VisualizationContext context, BlockEntityPABeamline blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        boolean window = blockState.getValue(BlockPABeamline.WINDOW);
        glassBasePose
                .translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 90)
                                * Mth.DEG_TO_RAD);
        bodyBounds =
                LightBounds.of(MODEL, window ? "BeamlineWindow" : "Beamline", glassBasePose, pos);
        glass =
                window
                        ? instancerProvider()
                                .instancer(InstanceTypes.TRANSFORMED, GLASS_MODEL)
                                .createInstance()
                        : null;
        if (glass != null) {
            pose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(glassBasePose);
            glass.setTransform(pose).light(LightCoordsUtil.FULL_BRIGHT);
        }
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        if (glass == null) return;
        float flash = blockEntity.flash(partialTick);
        if (flash == lastFlash) return;
        lastFlash = flash;
        float redGreen = Math.min(.9F * flash, 1F);
        glass.colorArgb(ARGB.colorFromFloat(1F, redGreen, redGreen, Math.min(flash, 1F)))
                .setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(new AABB(pos).inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        if (glass != null) glass.delete();
    }
}
