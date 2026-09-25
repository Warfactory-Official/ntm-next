// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.oil.BlockEntityMachineSolidifier;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class SolidifierVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineSolidifier>
        implements ShaderLightVisual {
    private static final Material FLUID_MATERIAL =
            SimpleMaterial.builder()
                    .texture(ResourceManager.white_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .useOverlay(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .backfaceCulling(false)
                    .build();
    private static final Material GLASS_MATERIAL =
            SimpleMaterial.builder()
                    .texture(ResourceManager.white_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.TINY)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .useOverlay(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .transparency(Transparency.ORDER_INDEPENDENT)
                    .writeMask(WriteMask.COLOR)
                    .backfaceCulling(false)
                    .build();
    private static final Model FLUID_MODEL =
            new SingleMeshModel(
                    PackedQuadMesh.of(
                            ResourceManager.solidifier
                                    .groups[ResourceManager.solidifier.partId("Fluid")],
                            ResourceManager.solidifier.smoothing()),
                    FLUID_MATERIAL);
    private static final Model GLASS_MODEL =
            new SingleMeshModel(
                    PackedQuadMesh.of(
                            ResourceManager.solidifier
                                    .groups[ResourceManager.solidifier.partId("Glass")],
                            ResourceManager.solidifier.smoothing()),
                    GLASS_MATERIAL);

    private final AABB bodyBounds;
    private final TransformedInstance fluid;
    private final TransformedInstance glass;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private double lastFraction = Double.NaN;
    private int lastFluidColor;
    private boolean lastFluidVisible;
    private boolean initialized;

    public SolidifierVisual(
            VisualizationContext context,
            BlockEntityMachineSolidifier blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f bodyLocal =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                (Facing.yaw(
                                                BlockMultiblockCore.coreFacing(
                                                        blockEntity.getBlockState()),
                                                90))
                                        * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(ResourceManager.solidifier, "Main", bodyLocal, pos);
        basePose.set(bodyLocal);
        fluid =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, FLUID_MODEL)
                        .createInstance();
        glass =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, GLASS_MODEL)
                        .createInstance();
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(basePose);
        glass.setTransform(instancePose).colorArgb(0x26BFFFFF).light(0).setChanged();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        FluidTankNTM tank = blockEntity.tank;
        double fraction = tank.getMaxFill() > 0 ? (double) tank.getFill() / tank.getMaxFill() : 0D;
        NTMFluidProperty property =
                tank.getFill() > 0 ? NTMFluidProperties.get(tank.getFluid()) : null;
        int fluidColor = property != null ? property.colorARGB() : 0;
        boolean fluidVisible = fluidColor != 0 && fraction > 0D;
        boolean fractionChanged =
                !initialized
                        || Double.doubleToLongBits(fraction)
                                != Double.doubleToLongBits(lastFraction);
        boolean colorChanged = !initialized || fluidColor != lastFluidColor;
        boolean visibleChanged = !initialized || fluidVisible != lastFluidVisible;
        if (!fractionChanged && !colorChanged && !visibleChanged) return;
        fluid.setVisible(fluidVisible);
        if (fluidVisible) {
            localPose
                    .set(basePose)
                    .translate(0F, 1.25F, 0F)
                    .scale(1F, (float) fraction, 1F)
                    .translate(0F, -1.25F, 0F);
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(localPose);
            fluid.setTransform(instancePose).colorArgb(fluidColor).light(0).setChanged();
        }
        lastFraction = fraction;
        lastFluidColor = fluidColor;
        lastFluidVisible = fluidVisible;
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 1,
                                pos.getY(),
                                pos.getZ() - 1,
                                pos.getX() + 2,
                                pos.getY() + 4,
                                pos.getZ() + 2)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        fluid.delete();
        glass.delete();
    }
}
