// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.oil.BlockEntityMachinePyroOven;
import com.hbm.util.BobMathUtil;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class PyroOvenVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachinePyroOven>
        implements ShaderLightVisual {
    private static final int SLIDER = ResourceManager.pyrooven.partId("Slider");
    private static final int FAN = ResourceManager.pyrooven.partId("Fan");
    private static final HFRWavefrontObject MODEL = ResourceManager.pyrooven;
    private static final Material MATERIAL = MeshPart.litCutout(ResourceManager.pyrooven_tex);
    private static final MeshPart[] PARTS = {
        MeshPart.obj(MODEL.groups[SLIDER], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[FAN], MODEL.smoothing(), MATERIAL)
    };
    private final AABB bodyBounds;
    private final TransformedInstance[] instances;
    private final Matrix4f[] local = {new Matrix4f(), new Matrix4f()};
    private final Matrix4f world = new Matrix4f();
    private final Matrix4f basePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastAnim = Float.NaN;

    public PyroOvenVisual(
            VisualizationContext context,
            BlockEntityMachinePyroOven blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translate(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 180)
                                * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(MODEL, "Oven", basePose, pos);
        instances = new TransformedInstance[PARTS.length];
        for (int i = 0; i < PARTS.length; i++)
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, PARTS[i].model())
                            .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float anim = Mth.lerp(partialTick, blockEntity.prevAnim, blockEntity.anim);
        if (Float.floatToIntBits(anim) == Float.floatToIntBits(lastAnim)) return;
        local[0].set(basePose)
                .translate((float) (BobMathUtil.sps(anim * .125D) * .5D - .5D), 0F, 0F);
        local[1].set(basePose)
                .translate(1.5F, 0F, 1.5F)
                .rotateY((float) (anim * 45D % 360D) * Mth.DEG_TO_RAD)
                .translate(-1.5F, 0F, -1.5F);
        write(0, local[0]);
        write(1, local[1]);
        LightBounds.resetBounds(lightBounds, bodyBounds);
        LightBounds.includeLightBounds(lightBounds, PARTS[0].model(), local[0], pos);
        LightBounds.includeLightBounds(lightBounds, PARTS[1].model(), local[1], pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastAnim = anim;
    }

    private void write(int index, Matrix4f local) {
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        instances[index].setTransform(world).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 3,
                                pos.getY(),
                                pos.getZ() - 3,
                                pos.getX() + 4,
                                pos.getY() + 3.5,
                                pos.getZ() + 4)
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
