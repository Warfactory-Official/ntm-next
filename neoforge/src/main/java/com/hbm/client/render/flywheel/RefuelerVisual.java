// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.BlockMachineHorizontal;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityRefueler;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.ClipTransformedInstance;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class RefuelerVisual extends HbmDynamicBlockEntityVisual<BlockEntityRefueler>
        implements ShaderLightVisual {
    private static final int FLUID = ResourceManager.refueler.partId("Fluid");
    private static final HFRWavefrontObject MODEL = ResourceManager.refueler;
    private static final Material MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_CLIP_HALFSPACE)
                    .texture(ResourceManager.white_tex)
                    .mipmap(false)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                    .writeMask(WriteMask.COLOR)
                    .backfaceCulling(true)
                    .build();
    private static final Model FLUID_MODEL =
            new SingleMeshModel(PackedQuadMesh.of(MODEL, FLUID), MATERIAL);
    private final AABB bodyBounds;
    private final ClipTransformedInstance fluid;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;
    private double lastFill = Double.NaN;
    private int lastColor;
    private boolean lastVisible;
    private boolean initialized;

    public RefuelerVisual(
            VisualizationContext context, BlockEntityRefueler blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Direction facing = blockEntity.getBlockState().getValue(BlockMachineHorizontal.FACING);
        Matrix4f bodyPose =
                new Matrix4f()
                        .translate(.5F, 0F, .5F)
                        .rotateY((Facing.yaw(facing, 90)) * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(MODEL, "Fueler", bodyPose, pos);
        basePose.set(bodyPose);
        fluid =
                instancerProvider()
                        .instancer(InstanceTypes.CLIP_TRANSFORMED, FLUID_MODEL)
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static int fluidColor(@Nullable Fluid fluid) {
        if (fluid == null) return CommonColors.WHITE;
        NTMFluidProperty property = NTMFluidProperties.get(fluid);
        return property.colorARGB();
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        double fill =
                blockEntity.prevFillLevel
                        + (blockEntity.fillLevel - blockEntity.prevFillLevel) * partialTick;
        double travel = (1D - fill) * -.625D;
        float clip = (float) (travel - .125D);
        int color = 0xBF000000 | (fluidColor(blockEntity.tank.getFluid()) & 0x00FFFFFF);
        boolean visible = fill > 0D;
        boolean fillChanged =
                !initialized || Double.doubleToLongBits(fill) != Double.doubleToLongBits(lastFill);
        boolean colorChanged = !initialized || color != lastColor;
        boolean visibilityChanged = !initialized || visible != lastVisible;
        if (!fillChanged && !colorChanged && !visibilityChanged) return;
        if (visibilityChanged) fluid.setVisible(visible);
        if (visible) {
            localPose.set(basePose).translate(0F, (float) travel, 0F);
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(localPose);
            fluid.setTransform(instancePose).colorArgb(color).light(0);
            fluid.setSlide(0F, 0F, 0F).setPlane(0F, -1F, 0F, clip).setChanged();
        }
        LightBounds.resetBounds(lightBounds, bodyBounds);
        if (visible) LightBounds.includeLightBounds(lightBounds, FLUID_MODEL, localPose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastFill = fill;
        lastColor = color;
        lastVisible = visible;
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(new AABB(pos).inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        fluid.delete();
    }
}
