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

import com.hbm.blockentity.generator.RadGenBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
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
import dev.engine_room.flywheel.lib.material.SimpleCutoutShader;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.material.SimpleMaterialShaders;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class RadGenVisual extends AbstractBoundedBlockEntityVisual<RadGenBlockEntity>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.radgen;
    private static final Material RAW_BODY_MATERIAL =
            SimpleMaterial.builderOf(MeshPart.litCutout(ResourceManager.radgen_tex))
                    .backfaceCulling(false)
                    .build();
    private static final Material LAMP_MATERIAL =
            SimpleMaterial.builder()
                    .texture(ResourceManager.white_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .useLight(false)
                    .useOverlay(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .light(LightShaders.NONE)
                    .ambientOcclusion(false)
                    .backfaceCulling(false)
                    .build();
    private static final Material TINT_MATERIAL =
            SimpleMaterial.builderOf(MeshPart.litCutout(ResourceManager.radgen_tex))
                    .shaders(
                            new SimpleMaterialShaders(
                                    Identifier.fromNamespaceAndPath(
                                            "flywheel", "material/default.vert"),
                                    Identifier.fromNamespaceAndPath(
                                            "flywheel", "material/geometry_tint_mask.frag"),
                                    true))
                    .cutout(
                            new SimpleCutoutShader(
                                    Identifier.fromNamespaceAndPath(
                                            "flywheel", "cutout/inverse_one_tenth.glsl")))
                    .transparency(Transparency.ORDER_INDEPENDENT)
                    .writeMask(WriteMask.COLOR)
                    .backfaceCulling(false)
                    .build();
    private static final Material SHEET_MATERIAL =
            SimpleMaterial.builderOf(MeshPart.litCutout(ResourceManager.radgen_tex))
                    .transparency(Transparency.OPAQUE)
                    .writeMask(WriteMask.COLOR_DEPTH)
                    .backfaceCulling(false)
                    .build();
    private static final int ROTOR = MODEL.partId("Rotor");
    private static final int LIGHT = MODEL.partId("Light");
    private static final int GLASS = MODEL.partId("Glass");
    private static final MeshPart LAMP_PART =
            MeshPart.obj(MODEL.groups[LIGHT], MODEL.smoothing(), LAMP_MATERIAL);
    private static final MeshPart TINT_PART =
            MeshPart.obj(MODEL.groups[GLASS], MODEL.smoothing(), TINT_MATERIAL);
    private static final MeshPart GLASS_PART =
            MeshPart.obj(MODEL.groups[GLASS], MODEL.smoothing(), SHEET_MATERIAL);
    private static final MeshPart ROTOR_PART =
            MeshPart.obj(MODEL.groups[ROTOR], MODEL.smoothing(), RAW_BODY_MATERIAL);
    private static final int SPIN_PERIOD = 3600;
    private static final float SPIN_RATE = -.1F;
    private static final ChainInstance.Chain[] ROTOR_CHAINS = {
        new ChainInstance.Chain()
                .clock(0)
                .translate(0F, 1.5F, 0F)
                .rotateX(0, Mth.DEG_TO_RAD)
                .translate(0F, -1.5F, 0F)
                .end()
    };
    private final ChainParts rotor;
    private final TransformedInstance lamp;
    private final TransformedInstance tint;
    private final TransformedInstance glass;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final AABB lightBounds;
    private boolean lastOn;
    private boolean initialized;

    public RadGenVisual(
            VisualizationContext context, RadGenBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        var rawBodyLocal =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(
                                                MultiblockCoreBlock.coreFacing(
                                                        blockEntity.getBlockState()),
                                                90)
                                        * Mth.DEG_TO_RAD);
        AABB rawBodyBounds = new AABB(pos).minmax(LightBounds.of(MODEL, "Base", rawBodyLocal, pos));
        basePose.set(rawBodyLocal);
        rotor =
                new ChainParts(
                        context,
                        visualPos,
                        basePose,
                        new Model[] {ROTOR_PART.model()},
                        ROTOR_CHAINS,
                        1);
        rotor.clock(ChainInstance.clockBase(GameTime.now(), SPIN_PERIOD), SPIN_PERIOD);
        double[] bounds = new double[6];
        LightBounds.resetBounds(
                bounds, rawBodyBounds.minmax(new AABB(pos).expandTowards(1, 3, 1).inflate(1)));
        rotor.includeLightBounds(bounds, pos);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        lamp =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, LAMP_PART.model())
                        .createInstance();
        tint =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, TINT_PART.model())
                        .createInstance();
        glass =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, GLASS_PART.model())
                        .createInstance();
        write(lamp, basePose, ARGB.colorFromFloat(1F, 0F, .1F, 0F), LightCoordsUtil.FULL_BRIGHT);
        write(tint, basePose, ARGB.colorFromFloat(.3F, .5F, .75F, 1F), 0);
        write(glass, basePose, 0xFFFFFFFF, 0);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    public void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        boolean on = blockEntity.isOn;
        if (initialized && on == lastOn) return;
        rotor.set(0, 0F, on ? SPIN_RATE : 0F);
        rotor.write();
        write(
                lamp,
                basePose,
                on ? ARGB.colorFromFloat(1F, 0F, 1F, 0F) : ARGB.colorFromFloat(1F, 0F, .1F, 0F),
                LightCoordsUtil.FULL_BRIGHT);
        lastOn = on;
        initialized = true;
    }

    private void write(TransformedInstance instance, Matrix4f pose, int color, int light) {
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instance.setTransform(instancePose).colorArgb(color).light(light).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return lightBounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        rotor.collectCrumblingInstances(consumer);
        consumer.accept(glass);
    }

    @Override
    protected void _delete() {
        rotor.delete();
        lamp.delete();
        tint.delete();
        glass.delete();
    }
}
