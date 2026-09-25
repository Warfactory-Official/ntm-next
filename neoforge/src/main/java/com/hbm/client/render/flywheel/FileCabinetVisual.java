// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.ModBlocks;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.storage.BlockEntityFileCabinet;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class FileCabinetVisual extends HbmDynamicBlockEntityVisual<BlockEntityFileCabinet>
        implements ShaderLightVisual {
    private static final float DRAWER_SLIDE = .6875F;
    private static final HFRWavefrontObject MODEL = ResourceManager.file_cabinet;
    private static final MeshPart[][] PARTS = {
        parts(MeshPart.litCutout(ResourceManager.file_cabinet_tex)),
        parts(MeshPart.litCutout(ResourceManager.file_cabinet_steel_tex))
    };
    private final MeshPart[] parts;
    private final AABB bodyBounds;
    private final TransformedInstance lower;
    private final TransformedInstance upper;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastLowerExtent = Float.NaN;
    private float lastUpperExtent = Float.NaN;

    public FileCabinetVisual(
            VisualizationContext context, BlockEntityFileCabinet blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        parts = PARTS[blockState.is(ModBlocks.FILING_CABINET_STEEL.get()) ? 1 : 0];
        basePose.translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(blockState.getValue(HorizontalDirectionalBlock.FACING), 180)
                                * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(MODEL, "Cabinet", basePose, pos);
        lower =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, parts[0].model())
                        .createInstance();
        upper =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, parts[1].model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    private static MeshPart[] parts(Material material) {
        return new MeshPart[] {
            MeshPart.obj(MODEL.groups[MODEL.partId("LowerDrawer")], MODEL.smoothing(), material),
            MeshPart.obj(MODEL.groups[MODEL.partId("UpperDrawer")], MODEL.smoothing(), material)
        };
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float lowerExtent =
                blockEntity.prevLowerExtent
                        + (blockEntity.lowerExtent - blockEntity.prevLowerExtent) * partialTick;
        float upperExtent =
                blockEntity.prevUpperExtent
                        + (blockEntity.upperExtent - blockEntity.prevUpperExtent) * partialTick;
        if (lowerExtent == lastLowerExtent && upperExtent == lastUpperExtent) return;
        lastLowerExtent = lowerExtent;
        lastUpperExtent = upperExtent;

        localPose.set(basePose).translate(0F, 0F, DRAWER_SLIDE * lowerExtent);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPose);
        lower.setTransform(instancePose).light(0).setChanged();
        LightBounds.resetBounds(lightBoundsAccumulator, bodyBounds);
        LightBounds.includeLightBounds(lightBoundsAccumulator, parts[0].model(), localPose, pos);

        localPose.set(basePose).translate(0F, 0F, DRAWER_SLIDE * upperExtent);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPose);
        upper.setTransform(instancePose).light(0).setChanged();
        LightBounds.includeLightBounds(lightBoundsAccumulator, parts[1].model(), localPose, pos);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(new AABB(pos).inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(lower);
        consumer.accept(upper);
    }

    @Override
    protected void _delete() {
        lower.delete();
        upper.delete();
    }
}
