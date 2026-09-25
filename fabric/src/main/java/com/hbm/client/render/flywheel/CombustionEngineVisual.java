// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.CD_Canister;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineCombustionEngine;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class CombustionEngineVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineCombustionEngine>
        implements ShaderLightVisual {
    private static final int CANISTER = ResourceManager.combustion_engine.partId("Canister");
    private static final int HATCH = ResourceManager.combustion_engine.partId("Hatch");
    private static final HFRWavefrontObject MODEL = ResourceManager.combustion_engine;
    private static final Material MATERIAL =
            MeshPart.litCutout(ResourceManager.combustion_engine_tex);
    private static final MeshPart[] PARTS = {
        MeshPart.obj(MODEL.groups[CANISTER], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[HATCH], MODEL.smoothing(), MATERIAL)
    };
    private final TransformedInstance[] instances;
    private final Matrix4f hatchPose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private final AABB rawBodyBounds;
    private final Matrix4f basePose = new Matrix4f();
    private float lastDoorAngle = Float.NaN;
    private Fluid lastTankType;
    private boolean initialized;

    public CombustionEngineVisual(
            VisualizationContext context,
            BlockEntityMachineCombustionEngine blockEntity,
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
        rawBodyBounds = new AABB(pos).minmax(LightBounds.of(MODEL, "Engine", rawBodyLocal, pos));
        instances = new TransformedInstance[PARTS.length];
        for (int i = 0; i < PARTS.length; i++)
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, PARTS[i].model())
                            .createInstance();
        basePose.set(rawBodyLocal).translate(-.5F, 0F, 3F);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static int channel(int value) {
        return Math.round((value & 0xFF) * 255F / 256F);
    }

    private static int canisterColor(Fluid tankType) {
        CD_Canister canister = NTMFluidProperties.getTrait(tankType, CD_Canister.class);
        if (canister == null) return -1;
        return ARGB.color(
                255,
                channel(canister.color >> 16),
                channel(canister.color >> 8),
                channel(canister.color));
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float doorAngle = Mth.lerp(partialTick, blockEntity.prevDoorAngle, blockEntity.doorAngle);
        Fluid tankType = blockEntity.tank.getTankType();
        if (!initialized || tankType != lastTankType) {
            lastTankType = tankType;
            write(0, basePose, canisterColor(tankType));
        }
        if (!initialized || doorAngle != lastDoorAngle) {
            hatchPose
                    .set(basePose)
                    .translate(1F, 0F, -2.6875F)
                    .rotateY(-doorAngle * Mth.DEG_TO_RAD)
                    .translate(-1F, 0F, 2.6875F);
            write(1, hatchPose, -1);
            lastDoorAngle = doorAngle;
        }
        initialized = true;
    }

    private void write(int index, Matrix4f pose, int color) {
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instances[index].setTransform(world).light(0).colorArgb(color).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(
                new AABB(
                                pos.getX() - 3,
                                pos.getY(),
                                pos.getZ() - 3,
                                pos.getX() + 4,
                                pos.getY() + 2,
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
