// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineChemicalFactory;
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

public final class ChemicalFactoryVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineChemicalFactory>
        implements ShaderLightVisual {
    private static final int FRAME = ResourceManager.chemical_factory.partId("Frame");
    private static final int FAN1 = ResourceManager.chemical_factory.partId("Fan1");
    private static final int FAN2 = ResourceManager.chemical_factory.partId("Fan2");
    private static final HFRWavefrontObject MODEL = ResourceManager.chemical_factory;
    private static final Material MATERIAL =
            MeshPart.litCutout(ResourceManager.chemical_factory_tex);
    private static final MeshPart[] PARTS = {
        MeshPart.obj(MODEL.groups[FRAME], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[FAN1], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[FAN2], MODEL.smoothing(), MATERIAL)
    };

    private final TransformedInstance[] instances;
    private final Matrix4f base = new Matrix4f();
    private final Matrix4f[] local = {new Matrix4f(), new Matrix4f()};
    private final Matrix4f world = new Matrix4f();
    private final AABB rawBodyBounds;
    private float lastAnim = Float.NaN;
    private boolean lastFrame;
    private boolean initialized;

    public ChemicalFactoryVisual(
            VisualizationContext context,
            BlockEntityMachineChemicalFactory blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        var facing = BlockMultiblockCore.coreFacing(blockEntity.getBlockState());
        var rawBodyLocal =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(Facing.yaw(facing, 90) * Mth.DEG_TO_RAD);
        rawBodyBounds = new AABB(pos).minmax(LightBounds.of(MODEL, "Base", rawBodyLocal, pos));
        base.translation(.5F, 0F, .5F).rotateY((90F + Facing.yaw(facing, 0)) * Mth.DEG_TO_RAD);
        instances = new TransformedInstance[PARTS.length];
        for (int i = 0; i < PARTS.length; i++) {
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, PARTS[i].model())
                            .createInstance();
        }
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float anim = Mth.lerp(partialTick, blockEntity.prevAnim, blockEntity.anim);
        boolean frame = blockEntity.frame;
        if (!initialized || frame != lastFrame) {
            lastFrame = frame;
            write(0, base, frame);
        }
        if (!initialized || anim != lastAnim) {
            lastAnim = anim;
            float fanAngle = (float) ((-anim * 45F) % 360F);
            local[0].set(base)
                    .translate(1F, 0F, 0F)
                    .rotateY(fanAngle * Mth.DEG_TO_RAD)
                    .translate(-1F, 0F, 0F);
            local[1].set(base)
                    .translate(-1F, 0F, 0F)
                    .rotateY(fanAngle * Mth.DEG_TO_RAD)
                    .translate(1F, 0F, 0F);
            write(1, local[0], true);
            write(2, local[1], true);
        }
        initialized = true;
    }

    private void write(int index, Matrix4f pose, boolean visible) {
        TransformedInstance instance = instances[index];
        instance.setVisible(visible);
        if (!visible) return;
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instance.setTransform(world).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(
                new AABB(
                                pos.getX() - 2,
                                pos.getY(),
                                pos.getZ() - 2,
                                pos.getX() + 3,
                                pos.getY() + 3,
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
