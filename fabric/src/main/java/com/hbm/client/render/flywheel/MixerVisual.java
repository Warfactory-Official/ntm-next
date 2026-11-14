/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.render.flywheel;

import com.hbm.blockentity.processing.FluidMixerBlockEntity;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class MixerVisual extends AbstractBoundedBlockEntityVisual<FluidMixerBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
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
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain().rotateY(0, -Mth.DEG_TO_RAD)
    };
    private final ChainParts mixer;
    private final TransformedInstance fluid;
    private final Matrix4f fluidPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private final AABB staticBounds;
    private @Nullable AABB lastLightBounds;
    private float lastFraction = Float.NaN;
    private int lastColor;
    private boolean lastFluidVisible;
    private boolean initialized;

    public MixerVisual(
            VisualizationContext context, FluidMixerBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f center = new Matrix4f().translation(.5F, 0F, .5F);
        AABB rawBodyBounds = new AABB(pos).minmax(LightBounds.of(MODEL, "Main", center, pos));
        mixer =
                new ChainParts(
                        context, visualPos, center, new Model[] {MIXER_PART.model()}, CHAINS, 1);
        LightBounds.resetBounds(lightBounds, rawBodyBounds);
        mixer.includeLightBounds(lightBounds, pos);
        staticBounds =
                new AABB(
                        lightBounds[0],
                        lightBounds[1],
                        lightBounds[2],
                        lightBounds[3],
                        lightBounds[4],
                        lightBounds[5]);
        fluid =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, FLUID_PART.model())
                        .createInstance();
        writeFrame();
        updateRotation();
    }

    public static void initModels() {}

    @Override
    public void frame(DynamicVisual.Context context) {
        writeFrame();
    }

    @Override
    public void tick(TickableVisual.Context context) {
        updateRotation();
    }

    private void updateRotation() {
        mixer.set(0, blockEntity.prevRotation, blockEntity.rotation);
        mixer.write();
    }

    private void writeFrame() {
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
                        blockEntity.tanks[FluidMixerBlockEntity.TANK_OUT].getFluid());
        int color = ARGB.color(0xBF, property == null ? NO_TYPE_COLOR : property.colorARGB());
        boolean fluidVisible = fraction > 0F;
        boolean fractionChanged =
                !initialized
                        || Float.floatToIntBits(fraction) != Float.floatToIntBits(lastFraction);
        boolean colorChanged = !initialized || color != lastColor;
        boolean visibilityChanged = !initialized || fluidVisible != lastFluidVisible;
        if (!fractionChanged && !colorChanged && !visibilityChanged) return;
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
        LightBounds.resetBounds(lightBounds, staticBounds);
        if (fluidVisible)
            LightBounds.includeLightBounds(lightBounds, FLUID_PART.model(), fluidPose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastFraction = fraction;
        lastColor = color;
        lastFluidVisible = fluidVisible;
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return staticBounds.minmax(
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
        mixer.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        mixer.delete();
        fluid.delete();
    }
}
