// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.MachineMicrowave;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityMicrowave;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class MicrowaveVisual extends HbmDynamicBlockEntityVisual<BlockEntityMicrowave>
        implements ShaderLightVisual {
    private static final float OFF_X = -0.5F, OFF_Y = -0.785F, OFF_Z = 0.65F;
    private static final MeshPart PLATE_PART =
            MeshPart.obj(
                    ResourceManager.microwave
                            .groups[ResourceManager.microwave.partId("plate_Cylinder")],
                    ResourceManager.microwave.smoothing(),
                    MeshPart.litCutout(ResourceManager.microwave_tex));
    private final AABB bodyBounds;
    private final TransformedInstance plate;
    private final Matrix4f platePose = new Matrix4f();
    private final Matrix4f plateBasePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private float lastSpin = Float.NaN;
    private boolean lastActive;
    private boolean initialized;

    public MicrowaveVisual(
            VisualizationContext context, BlockEntityMicrowave blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        plateBasePose
                .translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(blockState.getValue(MachineMicrowave.FACING), 0)
                                * Mth.DEG_TO_RAD)
                .translate(OFF_X, OFF_Y, OFF_Z);
        var mesh = ResourceManager.microwave;
        bodyBounds =
                LightBounds.of(mesh, "mainbody_Cube.001", plateBasePose, pos)
                        .minmax(LightBounds.of(mesh, "window_Cube.002", plateBasePose, pos));
        plate =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, PLATE_PART.model())
                        .createInstance();
        writeFrame(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        boolean active = blockEntity.time > 0;
        float spin =
                active
                        ? (float)
                                ((GameTime.millis(blockEntity.getLevel()) + partialTick * 50D)
                                        * blockEntity.speed
                                        / 10D
                                        % 360D)
                        : 0F;
        if (initialized && active == lastActive && (!active || spin == lastSpin)) return;
        platePose.set(plateBasePose);
        if (active)
            platePose
                    .translate(.575F, 0F, -.45F)
                    .rotateY(spin * Mth.DEG_TO_RAD)
                    .translate(-.575F, 0F, .45F);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(platePose);
        plate.setTransform(instancePose).light(0).setChanged();
        lastActive = active;
        lastSpin = spin;
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(new AABB(pos).inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(plate);
    }

    @Override
    protected void _delete() {
        plate.delete();
    }
}
