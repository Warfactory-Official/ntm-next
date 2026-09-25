// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityMachineIndustrialTurbine;
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
import org.jspecify.annotations.Nullable;

public final class IndustrialTurbineVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineIndustrialTurbine>
        implements ShaderLightVisual {
    private static final Material MATERIAL =
            MeshPart.litCutout(ResourceManager.industrial_turbine_tex);
    private static final MeshPart GAUGE_PART =
            MeshPart.obj(
                    ResourceManager.industrial_turbine
                            .groups[ResourceManager.industrial_turbine.partId("Gauge")],
                    ResourceManager.industrial_turbine.smoothing(),
                    MATERIAL);
    private static final MeshPart FLYWHEEL_PART =
            MeshPart.obj(
                    ResourceManager.industrial_turbine
                            .groups[ResourceManager.industrial_turbine.partId("Flywheel")],
                    ResourceManager.industrial_turbine.smoothing(),
                    MATERIAL);

    private final TransformedInstance gauge;
    private final TransformedInstance flywheel;
    private final AABB bodyBounds;
    private final Matrix4f gaugePose = new Matrix4f();
    private final Matrix4f flywheelPose = new Matrix4f();
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;
    private int lastSteamTier = -1;
    private float lastRotor = Float.NaN;
    private boolean initialized;

    public IndustrialTurbineVisual(
            VisualizationContext context,
            BlockEntityMachineIndustrialTurbine blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        gauge =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, GAUGE_PART.model())
                        .createInstance();
        flywheel =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, FLYWHEEL_PART.model())
                        .createInstance();
        basePose.identity()
                .translate(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 180)
                                * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(ResourceManager.industrial_turbine, "Turbine", basePose, pos);
        writeFrame(partialTick);
    }

    public static void initModels() {}

    private static int steamTier(@Nullable Fluid type) {
        if (type == NTMFluids.HOTSTEAM) return 1;
        if (type == NTMFluids.SUPERHOTSTEAM) return 2;
        if (type == NTMFluids.ULTRAHOTSTEAM) return 3;
        return 0;
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        int tier = steamTier(blockEntity.tanks[0].getTankType());
        float rotor = Mth.lerp(partialTick, blockEntity.lastRotor, blockEntity.rotor);
        boolean gaugeChanged = !initialized || tier != lastSteamTier;
        boolean flywheelChanged =
                !initialized || Float.floatToIntBits(rotor) != Float.floatToIntBits(lastRotor);
        if (!gaugeChanged && !flywheelChanged) return;
        if (gaugeChanged) {
            float gaugeAngle = 135F - tier * 90F;
            gaugePose
                    .set(basePose)
                    .translate(0F, 1.5F, 0F)
                    .rotateZ(gaugeAngle * Mth.DEG_TO_RAD)
                    .translate(0F, -1.5F, 0F);
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(gaugePose);
            gauge.setTransform(instancePose).light(0).setChanged();
        }
        if (flywheelChanged) {
            flywheelPose
                    .set(basePose)
                    .translate(0F, 1.5F, 0F)
                    .rotateZ(-rotor * Mth.DEG_TO_RAD)
                    .translate(0F, -1.5F, 0F);
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(flywheelPose);
            flywheel.setTransform(instancePose).light(0).setChanged();
        }
        LightBounds.resetBounds(lightBounds, bodyBounds);
        LightBounds.includeLightBounds(lightBounds, GAUGE_PART.model(), gaugePose, pos);
        LightBounds.includeLightBounds(lightBounds, FLYWHEEL_PART.model(), flywheelPose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastSteamTier = tier;
        lastRotor = rotor;
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 3,
                                pos.getY(),
                                pos.getZ() - 3,
                                pos.getX() + 4,
                                pos.getY() + 3,
                                pos.getZ() + 4)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(gauge);
        consumer.accept(flywheel);
    }

    @Override
    protected void _delete() {
        gauge.delete();
        flywheel.delete();
    }
}
