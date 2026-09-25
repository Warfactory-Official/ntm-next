// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineMixer;
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class MixerVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachineMixer>
        implements ShaderLightVisual {
    private static final int NO_TYPE_COLOR = 0x888888;
    private static final HFRWavefrontObject MODEL = ResourceManager.mixer;
    private static final Material MIXER_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.mixer_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .build();
    private static final Material FLUID_MATERIAL =
            SimpleMaterial.builderOf(Materials.TRANSLUCENT_NO_DEPTH_WRITE_NO_CULL)
                    .texture(ResourceManager.white_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.TINY)
                    .transparency(Transparency.ORDER_INDEPENDENT)
                    .writeMask(WriteMask.COLOR)
                    .depthTest(DepthTest.LEQUAL)
                    .light(LightShaders.SMOOTH)
                    .useLight(true)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .backfaceCulling(false)
                    .build();
    private static final MeshPart MIXER_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Mixer")], MODEL.smoothing(), MIXER_MATERIAL);
    private static final MeshPart FLUID_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Fluid")], MODEL.smoothing(), FLUID_MATERIAL);
    private final TransformedInstance mixer;
    private final TransformedInstance fluid;
    private final Matrix4f mixerPose = new Matrix4f();
    private final Matrix4f fluidPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private final AABB rawBodyBounds;
    private @Nullable AABB lastLightBounds;
    private float lastRotation = Float.NaN;
    private float lastFraction = Float.NaN;
    private int lastColor;
    private boolean lastFluidVisible;
    private boolean initialized;

    public MixerVisual(
            VisualizationContext context, BlockEntityMachineMixer blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        rawBodyBounds =
                new AABB(pos)
                        .minmax(
                                LightBounds.of(
                                        MODEL,
                                        "Main",
                                        new Matrix4f().translation(.5F, 0F, .5F),
                                        pos));
        mixer =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, MIXER_PART.model())
                        .createInstance();
        fluid =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, FLUID_PART.model())
                        .createInstance();
        writeFrame(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        float rotation = Mth.lerp(partialTick, blockEntity.prevRotation, blockEntity.rotation);
        boolean rotationChanged =
                !initialized
                        || Float.floatToIntBits(rotation) != Float.floatToIntBits(lastRotation);

        int totalFill = 0;
        int totalMax = 0;
        for (FluidTankNTM tank : blockEntity.tanks) {
            if (tank.getTankType() != null && tank.getTankType() != NTMFluids.NONE) {
                totalFill += tank.getFill();
                totalMax += tank.getMaxFill();
            }
        }
        float fraction = totalMax > 0 ? (float) totalFill / totalMax : 0F;
        NTMFluidProperty property =
                NTMFluidProperties.get(
                        blockEntity.tanks[BlockEntityMachineMixer.TANK_OUT].getFluid());
        int color = ARGB.color(0xBF, property == null ? NO_TYPE_COLOR : property.colorARGB());
        boolean fluidVisible = fraction > 0F;
        boolean fractionChanged =
                !initialized
                        || Float.floatToIntBits(fraction) != Float.floatToIntBits(lastFraction);
        boolean colorChanged = !initialized || color != lastColor;
        boolean visibilityChanged = !initialized || fluidVisible != lastFluidVisible;
        if (!rotationChanged && !fractionChanged && !colorChanged && !visibilityChanged) return;
        if (rotationChanged) {
            mixerPose.identity().translate(.5F, 0F, .5F).rotateY(-rotation * Mth.DEG_TO_RAD);
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(mixerPose);
            mixer.setTransform(instancePose).light(0).setChanged();
        }
        if (visibilityChanged) fluid.setVisible(fluidVisible);
        if (fluidVisible && (fractionChanged || colorChanged || visibilityChanged)) {
            fluidPose
                    .identity()
                    .translate(.5F, 1F, .5F)
                    .scale(1F, fraction * .99F, 1F)
                    .translate(0F, -1F, 0F);
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(fluidPose);
            fluid.setTransform(instancePose).colorArgb(color).light(0).setChanged();
        }
        LightBounds.resetBounds(lightBounds, rawBodyBounds);
        LightBounds.includeLightBounds(lightBounds, MIXER_PART.model(), mixerPose, pos);
        if (fluidVisible)
            LightBounds.includeLightBounds(lightBounds, FLUID_PART.model(), fluidPose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastRotation = rotation;
        lastFraction = fraction;
        lastColor = color;
        lastFluidVisible = fluidVisible;
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(
                new AABB(
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                pos.getX() + 1,
                                pos.getY() + 3,
                                pos.getZ() + 1)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(mixer);
    }

    @Override
    protected void _delete() {
        mixer.delete();
        fluid.delete();
    }
}
