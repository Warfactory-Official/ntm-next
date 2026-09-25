// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.pile.BlockPileDevice;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.pile.BlockEntityPileControl;
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

public final class PileControlVisual extends HbmDynamicBlockEntityVisual<BlockEntityPileControl>
        implements ShaderLightVisual {
    private static final int ROD = ResourceManager.pile_control.partId("Rod");
    private static final MeshPart ROD_PART =
            MeshPart.obj(
                    ResourceManager.pile_control.groups[ROD],
                    ResourceManager.pile_control.smoothing(),
                    MeshPart.litCutout(ResourceManager.pile_control_tex));

    private final TransformedInstance rod;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private float lastExtension = Float.NaN;

    public PileControlVisual(
            VisualizationContext context, BlockEntityPileControl blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(blockState.getValue(BlockPileDevice.FACING), 90)
                                * Mth.DEG_TO_RAD);
        rod =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, ROD_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    private void updateMovingParts(float partialTick) {
        float extension =
                (float)
                        (blockEntity.lastExtension
                                + (blockEntity.extension - blockEntity.lastExtension)
                                        * partialTick);
        if (extension == lastExtension) return;
        lastExtension = extension;
        pose.set(basePose).translate(0F, extension * .75F, 0F);
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        rod.setTransform(world).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(2);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(rod);
    }

    @Override
    protected void _delete() {
        rod.delete();
    }
}
