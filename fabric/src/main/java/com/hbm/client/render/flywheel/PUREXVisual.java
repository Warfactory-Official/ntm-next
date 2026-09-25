// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityMachinePUREX;
import com.hbm.util.BobMathUtil;
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

public final class PUREXVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachinePUREX>
        implements ShaderLightVisual {
    private static final int FRAME = ResourceManager.purex.partId("Frame");
    private static final int FAN = ResourceManager.purex.partId("Fan");
    private static final int PUMP = ResourceManager.purex.partId("Pump");
    private static final MeshPart[] STATIC_PARTS = {
        MeshPart.obj(
                ResourceManager.purex.groups[FRAME],
                ResourceManager.purex.smoothing(),
                MeshPart.litCutout(ResourceManager.purex_tex)),
        MeshPart.obj(
                ResourceManager.purex.groups[FAN],
                ResourceManager.purex.smoothing(),
                MeshPart.litCutout(ResourceManager.purex_tex)),
        MeshPart.obj(
                ResourceManager.purex.groups[PUMP],
                ResourceManager.purex.smoothing(),
                MeshPart.litCutout(ResourceManager.purex_tex))
    };
    private final AABB bodyBounds;
    private final TransformedInstance[] instances;
    private final Matrix4f[] local = {new Matrix4f(), new Matrix4f(), new Matrix4f()};
    private final Matrix4f world = new Matrix4f();
    private double lastAnimation = Double.NaN;
    private boolean lastFrame;
    private boolean initialized;

    public PUREXVisual(
            VisualizationContext context, BlockEntityMachinePUREX blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        local[0].translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 90)
                                * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(ResourceManager.purex, "Base", local[0], pos);
        instances = new TransformedInstance[STATIC_PARTS.length];
        for (int i = 0; i < STATIC_PARTS.length; i++)
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, STATIC_PARTS[i].model())
                            .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        double anim = Mth.lerp(partialTick, blockEntity.prevAnim, blockEntity.anim);
        boolean frame = blockEntity.frame;
        if (!initialized || frame != lastFrame) {
            instances[0].setVisible(frame);
            if (frame) write(0, local[0]);
            lastFrame = frame;
        }
        if (anim != lastAnimation) {
            local[1].set(local[0])
                    .translate(1.5F, 1.25F, 0F)
                    .rotateZ((float) (anim * 45D) * Mth.DEG_TO_RAD)
                    .translate(-1.5F, -1.25F, 0F);
            local[2].set(local[0]).translate((float) (BobMathUtil.sps(anim * .25D) * .5D), 0F, 0F);
            write(1, local[1]);
            write(2, local[2]);
            lastAnimation = anim;
        }
        initialized = true;
    }

    private void write(int index, Matrix4f pose) {
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instances[index].setTransform(world).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 2,
                                pos.getY(),
                                pos.getZ() - 2,
                                pos.getX() + 3,
                                pos.getY() + 5,
                                pos.getZ() + 3)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (TransformedInstance instance : instances) consumer.accept(instance);
    }

    @Override
    protected void _delete() {
        for (TransformedInstance instance : instances) instance.delete();
    }
}
