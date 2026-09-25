// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.pile.BlockPileDevice;
import com.hbm.client.model.PileDeviceModel;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.pile.BlockEntityPileVent;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class PileVentVisual extends HbmDynamicBlockEntityVisual<BlockEntityPileVent>
        implements ShaderLightVisual {
    private static final int FAN = ResourceManager.pile_vent.partId(PileDeviceModel.VENT_FAN);
    private static final MeshPart FAN_PART =
            MeshPart.obj(
                    ResourceManager.pile_vent.groups[FAN],
                    ResourceManager.pile_vent.smoothing(),
                    MeshPart.litCutout(ResourceManager.pile_vent_tex));
    private final TransformedInstance fan;
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private final AABB rawBodyBounds;
    private final Matrix4f basePose = new Matrix4f();
    private float lastSpin = Float.NaN;

    public PileVentVisual(
            VisualizationContext context, BlockEntityPileVent blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(blockState.getValue(BlockPileDevice.FACING), 90)
                                * Mth.DEG_TO_RAD);
        rawBodyBounds =
                new AABB(pos)
                        .minmax(LightBounds.of(ResourceManager.pile_vent, "Pipe", basePose, pos));
        fan =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, FAN_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float spin = blockEntity.lastFan + (blockEntity.fan - blockEntity.lastFan) * partialTick;
        if (spin == lastSpin) return;
        lastSpin = spin;
        pose.set(basePose).rotateY(spin * Mth.DEG_TO_RAD);
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        fan.setTransform(world).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(new AABB(pos).inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(fan);
    }

    @Override
    protected void _delete() {
        fan.delete();
    }
}
