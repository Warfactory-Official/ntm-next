// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityConveyorPress;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class ConveyorPressVisual extends HbmDynamicBlockEntityVisual<BlockEntityConveyorPress>
        implements ShaderLightVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.conveyor_press;
    private static final MeshPart PISTON_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("Piston")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.conveyor_press_tex));
    private static final MeshPart BELT_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("Belt")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.conveyor_press_belt_tex));
    private final AABB bodyBounds;
    private final TransformedInstance piston;
    private final UvTransformedInstance belt;
    private final Matrix4f base = new Matrix4f();
    private final Matrix4f pistonPose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastPress = Float.NaN;
    private float lastBeltOffset = Float.NaN;
    private boolean lastStamped;
    private boolean initialized;

    public ConveyorPressVisual(
            VisualizationContext context, BlockEntityConveyorPress blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        base.translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 90)
                                * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(MODEL, "Press", base, pos);
        piston =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, PISTON_PART.model())
                        .createInstance();
        belt =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, BELT_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float press =
                (float)
                        (blockEntity.lastPress
                                + (blockEntity.renderPress - blockEntity.lastPress) * partialTick);
        float beltOffset = ((GameTime.now() % 16L) - 2L) / 16F;
        boolean stamped = !blockEntity.syncStack.isEmpty();
        boolean poseChanged = !initialized || press != lastPress;
        boolean stampedChanged = !initialized || stamped != lastStamped;
        boolean beltChanged = !initialized || beltOffset != lastBeltOffset;
        if (!poseChanged && !stampedChanged && !beltChanged) return;
        if (poseChanged || stampedChanged) {
            lastPress = press;
            lastStamped = stamped;
            pistonPose.set(base).translate(0F, -press * .75F, 0F);
            piston.setVisible(stamped);
            if (stamped) {
                world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(pistonPose);
                piston.setTransform(world).light(0).setChanged();
            }
            LightBounds.resetBounds(lightBoundsAccumulator, bodyBounds);
            if (stamped)
                LightBounds.includeLightBounds(
                        lightBoundsAccumulator, PISTON_PART.model(), pistonPose, pos);
            lastLightBounds =
                    LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
        }
        if (beltChanged) {
            lastBeltOffset = beltOffset;
            world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(base);
            belt.setTransform(world).light(0);
            belt.uvRegion(0F, beltOffset, 1F, 1F).setChanged();
        }
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 1,
                                pos.getY(),
                                pos.getZ() - 1,
                                pos.getX() + 2,
                                pos.getY() + 3,
                                pos.getZ() + 2)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(piston);
        consumer.accept(belt);
    }

    @Override
    protected void _delete() {
        piston.delete();
        belt.delete();
    }
}
