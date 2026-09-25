// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityCrucible.PourStream;
import com.hbm.tileentity.machine.BlockEntityMachineRotaryFurnace;
import com.hbm.util.BobMathUtil;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class RotaryFurnaceVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineRotaryFurnace>
        implements ShaderLightVisual {
    private static final int PISTON = ResourceManager.rotary_furnace.partId("Piston");
    private static final float MAX_AGE = 20F;
    private static final MeshPart PISTON_PART =
            MeshPart.obj(
                    ResourceManager.rotary_furnace.groups[PISTON],
                    ResourceManager.rotary_furnace.smoothing(),
                    MeshPart.litCutout(ResourceManager.rotary_furnace_tex));
    private final TransformedInstance piston;
    private final List<PourVisual> pours = new ArrayList<>();
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final AABB rawBodyBounds;
    private final Matrix4f basePose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastLift = Float.NaN;
    private boolean initialized;

    public RotaryFurnaceVisual(
            VisualizationContext context,
            BlockEntityMachineRotaryFurnace blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        var rawBodyLocal =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(
                                                BlockMultiblockCore.coreFacing(
                                                        blockEntity.getBlockState()),
                                                90)
                                        * Mth.DEG_TO_RAD);
        basePose.set(rawBodyLocal);
        rawBodyBounds =
                new AABB(pos)
                        .minmax(
                                LightBounds.of(
                                        ResourceManager.rotary_furnace,
                                        "Furnace",
                                        rawBodyLocal,
                                        pos));
        piston =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, PISTON_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float anim = Mth.lerp(partialTick, blockEntity.lastAnim, blockEntity.anim);
        float lift = (float) (BobMathUtil.sps((anim * .75F) * .125F) * .5D - .5D);
        if (!initialized || lift != lastLift) {
            lastLift = lift;
            localPose.set(basePose).translate(0F, lift, 0F);
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(localPose);
            piston.setTransform(instancePose).light(0).setChanged();
            LightBounds.resetBounds(lightBoundsAccumulator, rawBodyBounds);
            LightBounds.includeLightBounds(
                    lightBoundsAccumulator, PISTON_PART.model(), localPose, pos);
            lastLightBounds =
                    LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
        }
        Direction direction =
                BlockMultiblockCore.coreFacing(blockEntity.getBlockState()).getCounterClockWise();
        long now = level.getGameTime();
        int slot = 0;
        for (PourStream stream : blockEntity.streams) {
            while (pours.size() <= slot)
                pours.add(new PourVisual(visualizationContext, level, pos));
            float age = (now - stream.birth()) + partialTick;
            pours.get(slot++)
                    .update(
                            stream.color(),
                            direction,
                            stream.len(),
                            age,
                            .625F,
                            .625F,
                            (float) (.5D + direction.getStepX() * 2.875D),
                            .75F,
                            (float) (.5D + direction.getStepZ() * 2.875D));
        }
        while (slot < pours.size()) {
            pours.get(slot++)
                    .update(
                            0,
                            direction,
                            0F,
                            MAX_AGE,
                            .625F,
                            .625F,
                            (float) (.5D + direction.getStepX() * 2.875D),
                            .75F,
                            (float) (.5D + direction.getStepZ() * 2.875D));
        }
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds;
    }

    @Override
    protected AABB visibleBounds() {
        return rawBodyBounds.minmax(
                new AABB(
                        pos.getX() - 3,
                        pos.getY() - 6,
                        pos.getZ() - 3,
                        pos.getX() + 4,
                        pos.getY() + 6,
                        pos.getZ() + 4));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(piston);
    }

    @Override
    protected void _delete() {
        piston.delete();
        for (PourVisual pour : pours) pour.delete();
    }
}
