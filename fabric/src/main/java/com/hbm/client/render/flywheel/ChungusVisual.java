// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityChungus;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class ChungusVisual extends HbmDynamicBlockEntityVisual<BlockEntityChungus>
        implements ShaderLightVisual {
    private static final int LEVER = ResourceManager.chungus.partId("Lever");
    private static final int BLADES = ResourceManager.chungus.partId("Blades");
    private static final HFRWavefrontObject MODEL = ResourceManager.chungus;
    private static final Material MATERIAL = MeshPart.litCutout(ResourceManager.chungus_tex);
    private static final MeshPart[] PARTS = {
        MeshPart.obj(MODEL.groups[LEVER], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[BLADES], MODEL.smoothing(), MATERIAL)
    };
    private final AABB bodyBounds;
    private final TransformedInstance[] instances;
    private final Matrix4f[] local = {new Matrix4f(), new Matrix4f()};
    private final Matrix4f world = new Matrix4f();
    private final Matrix4f basePose = new Matrix4f();
    private float lastLever = Float.NaN;
    private float lastRotor = Float.NaN;

    public ChungusVisual(
            VisualizationContext context, BlockEntityChungus blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 180)
                                * Mth.DEG_TO_RAD)
                .translate(0F, 0F, -3F);
        bodyBounds = LightBounds.of(MODEL, "Body", basePose, pos);
        instances = new TransformedInstance[PARTS.length];
        for (int i = 0; i < PARTS.length; i++)
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, PARTS[i].model())
                            .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static int steamTier(Fluid fluid) {
        if (fluid == NTMFluids.HOTSTEAM) return 1;
        if (fluid == NTMFluids.SUPERHOTSTEAM) return 2;
        if (fluid == NTMFluids.ULTRAHOTSTEAM) return 3;
        return 0;
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float lever = 15F - steamTier(blockEntity.tanks[0].getTankType()) * 10F;
        float rotor = Mth.lerp(partialTick, blockEntity.lastRotor, blockEntity.rotor);
        if (lever != lastLever) {
            lastLever = lever;
            local[0].set(basePose)
                    .translate(0F, 0F, 4.5F)
                    .rotateX(lever * Mth.DEG_TO_RAD)
                    .translate(0F, 0F, -4.5F);
            write(0);
        }
        if (rotor != lastRotor) {
            lastRotor = rotor;
            local[1].set(basePose)
                    .translate(0F, 2.5F, 0F)
                    .rotateZ(-rotor * Mth.DEG_TO_RAD)
                    .translate(0F, -2.5F, 0F);
            write(1);
        }
    }

    private void write(int index) {
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local[index]);
        instances[index].setTransform(world).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 14,
                                pos.getY() - 2,
                                pos.getZ() - 14,
                                pos.getX() + 15,
                                pos.getY() + 8,
                                pos.getZ() + 15)
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
