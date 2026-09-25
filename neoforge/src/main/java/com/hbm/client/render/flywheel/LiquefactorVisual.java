// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.oil.BlockEntityMachineLiquefactor;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.function.Consumer;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class LiquefactorVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineLiquefactor>
        implements ShaderLightVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.liquefactor;
    private static final Material FLUID_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT)
                    .texture(ResourceManager.white_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .backfaceCulling(false)
                    .build();
    private static final Material GLASS_MATERIAL =
            SimpleMaterial.builderOf(Materials.TRANSLUCENT_NO_DEPTH_WRITE_NO_CULL)
                    .texture(ResourceManager.white_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.TINY)
                    .transparency(Transparency.ORDER_INDEPENDENT)
                    .writeMask(WriteMask.COLOR)
                    .depthTest(DepthTest.LEQUAL)
                    .light(LightShaders.SMOOTH)
                    .useLight(true)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .backfaceCulling(false)
                    .build();
    private static final MeshPart FLUID_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Fluid")], MODEL.smoothing(), FLUID_MATERIAL);
    private static final MeshPart GLASS_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Glass")], MODEL.smoothing(), GLASS_MATERIAL);
    private final AABB bodyBounds;
    private final TransformedInstance fluid;
    private final TransformedInstance glass;
    private final Matrix4f fluidPose = new Matrix4f();
    private final Matrix4f glassPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private float lastFraction = Float.NaN;
    private int lastColor;
    private boolean lastHasFluid;
    private boolean initialized;

    public LiquefactorVisual(
            VisualizationContext context,
            BlockEntityMachineLiquefactor blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        glassPose.identity().translate(.5F, 0F, .5F);
        bodyBounds = LightBounds.of(MODEL, "Main", glassPose, pos);
        fluid =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, FLUID_PART.model())
                        .createInstance();
        glass =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, GLASS_PART.model())
                        .createInstance();
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(glassPose);
        glass.setTransform(instancePose).colorArgb(0x26BFFFFF).light(0).setChanged();
        writeFrame(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        FluidTankNTM tank = blockEntity.tank;
        float fraction = tank.getMaxFill() > 0 ? (float) tank.getFill() / tank.getMaxFill() : 0F;
        NTMFluidProperty property =
                tank.getFill() > 0 ? NTMFluidProperties.get(tank.getFluid()) : null;
        int color = property == null ? 0 : property.colorARGB();
        boolean hasFluid = tank.getFill() > 0;
        if (initialized
                && hasFluid == lastHasFluid
                && fraction == lastFraction
                && color == lastColor) return;
        fluidPose
                .set(glassPose)
                .translate(0F, 1F, 0F)
                .scale(1F, fraction, 1F)
                .translate(0F, -1F, 0F);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(fluidPose);
        fluid.setVisible(hasFluid);
        if (hasFluid) {
            fluid.setTransform(instancePose).colorArgb(ARGB.opaque(color)).light(0).setChanged();
        }
        lastHasFluid = hasFluid;
        lastFraction = fraction;
        lastColor = color;
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
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(fluid);
    }

    @Override
    protected void _delete() {
        fluid.delete();
        glass.delete();
    }
}
