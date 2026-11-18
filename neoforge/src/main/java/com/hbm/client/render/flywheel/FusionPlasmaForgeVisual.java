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

import com.hbm.blockentity.machine.fusion.FusionPlasmaForgeBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.client.render.FramedItem;
import com.hbm.client.render.FusionPlasmaForgeRenderer;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;

public final class FusionPlasmaForgeVisual
        extends AbstractBoundedBlockEntityVisual<FusionPlasmaForgeBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final float BASE_YAW = 90F;
    private static final float BEAM_IN = .4375F;
    private static final float BEAM_BOTTOM = 1F;
    private static final float BEAM_TEX = 1.5F;
    private static final float BEAM_TOP = BEAM_BOTTOM + BEAM_TEX;
    private static final Identifier STELLAR_FLUX =
            RegistryUtil.id("textures/gui/fluids/stellar_flux.png");
    private static final String[] BODY_NAMES = {
        "Bolts1",
        "SliderStriker",
        "ArmLowerStriker",
        "ArmUpperStriker",
        "StrikerMount",
        "StrikerRight",
        "PistonRight",
        "StrikerLeft",
        "PistonLeft",
        "SliderJet",
        "ArmLowerJet",
        "ArmUpperJet",
        "Jet"
    };
    private static final HFRWavefrontObject MODEL = ResourceManager.fusion_plasma_forge;
    private static final HFRWavefrontObject TORUS_MODEL = ResourceManager.fusion_torus;
    private static final Material BODY_MATERIAL =
            MeshPart.litCutout(ResourceManager.fusion_plasma_forge_tex);
    private static final Material COLD_MATERIAL = MeshPart.litCutout(EffectVisuals.Shared.WHITE);
    private static final Material PLASMA_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.fusion_plasma_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .depthTest(DepthTest.LEQUAL)
                    .light(LightShaders.NONE)
                    .useLight(false)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .build();
    private static final Material GLOW_MATERIAL =
            SimpleMaterial.builderOf(Materials.ADDITIVE_NO_CULL)
                    .texture(ResourceManager.fusion_plasma_glow_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.OFF)
                    .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                    .writeMask(WriteMask.COLOR)
                    .depthTest(DepthTest.LEQUAL)
                    .fog(EffectVisuals.FADE)
                    .light(LightShaders.NONE)
                    .useLight(false)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .build();
    private static final Material FLAME_MATERIAL =
            SimpleMaterial.builderOf(Materials.ADDITIVE_NO_CULL)
                    .texture(EffectVisuals.Shared.WHITE)
                    .mipmap(false)
                    .cutout(CutoutShaders.TINY)
                    .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                    .writeMask(WriteMask.COLOR)
                    .depthTest(DepthTest.LEQUAL)
                    .fog(EffectVisuals.FADE)
                    .light(LightShaders.SMOOTH)
                    .useLight(true)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .build();
    private static final Material BEAM_MATERIAL =
            SimpleMaterial.builderOf(Materials.ADDITIVE_NO_CULL)
                    .texture(STELLAR_FLUX)
                    .mipmap(false)
                    .cutout(CutoutShaders.TINY)
                    .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                    .writeMask(WriteMask.COLOR)
                    .depthTest(DepthTest.LEQUAL)
                    .fog(EffectVisuals.FADE)
                    .light(LightShaders.SMOOTH)
                    .useLight(true)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .build();
    private static final MeshPart[] BODY_PARTS = buildBodyParts();
    private static final ChainInstance.Chain[] STRIKER_RIGHT = {
        strikerArm(1),
        strikerArm(2),
        strikerArm(3),
        strikerArm(4),
        striker(1F).end(),
        striker(1F).slideY(5, -1F)
    };
    private static final ChainInstance.Chain[] STRIKER_LEFT = {
        striker(-1F).end(), striker(-1F).slideY(5, -1F)
    };
    private static final ChainInstance.Chain[] JET = {jetArm(1), jetArm(2), jetArm(3), jetArm(4)};
    private static final float[] PISTON_LOW = new float[6];
    private static final float[] PISTON_HIGH = {0F, 0F, 0F, 0F, 0F, .5F};
    private static final MeshPart COLD_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Plasma")], MODEL.smoothing(), COLD_MATERIAL);
    private static final MeshPart PLASMA_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Plasma")], MODEL.smoothing(), PLASMA_MATERIAL);
    private static final MeshPart GLOW_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Plasma")], MODEL.smoothing(), GLOW_MATERIAL);
    private static final Model FLAME1_PART = new SingleMeshModel(flame(.01F), FLAME_MATERIAL);
    private static final Model FLAME2_PART = new SingleMeshModel(flame(.09375F), FLAME_MATERIAL);
    private static final Model ICON_PART = new SingleMeshModel(iconBeam(), BEAM_MATERIAL);
    private final TransformedInstance bolts;
    private final ChainParts strikerRight;
    private final ChainParts strikerLeft;
    private final ChainParts jetArm;
    private final AABB lightBounds;
    private final TransformedInstance cold;
    private final UvTransformedInstance plasma;
    private final UvTransformedInstance glow1;
    private final UvTransformedInstance glow2;
    private final TransformedInstance flame1;
    private final TransformedInstance flame2;
    private final UvTransformedInstance iconBeam;
    private final Matrix4f rootPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final Matrix4f flamePose = new Matrix4f();
    private final Matrix4f jetEndPose = new Matrix4f();
    private final double[] jet = new double[4];
    private final Random random = new Random();
    private long lastTime = Long.MIN_VALUE;
    private double lastTicks = Double.NaN;
    private long lastEnergy = Long.MIN_VALUE;
    private boolean lastConnected;
    private boolean lastBeamInRange;
    private boolean lastJetFiring;
    private boolean coldShown;
    private boolean initialized;
    private final float yaw;
    private final WorldItem icon = new WorldItem(visualizationContext, level, pos);
    private final PoseStack iconPoses = new PoseStack();
    private float iconPartialTick;
    private final Consumer<PoseStack> iconWrite =
            poses -> icon.write(poses.last().pose(), iconPartialTick);

    public FusionPlasmaForgeVisual(
            VisualizationContext context,
            FusionPlasmaForgeBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        yaw = BASE_YAW + Facing.yaw(MultiblockCoreBlock.coreFacing(blockState), 90);
        rootPose.translation(.5F, 0F, .5F).rotateY(yaw * Mth.DEG_TO_RAD);
        AABB bodyBounds = LightBounds.of(MODEL, "Body", rootPose, pos);
        Matrix4f boltsPose = new Matrix4f(rootPose).translate(-2F, 0F, 0F);
        bolts =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, BODY_PARTS[0].model())
                        .createInstance();
        bolts.setTransform(
                        new Matrix4f()
                                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                                .mul(boltsPose))
                .light(0)
                .setChanged();
        strikerRight = new ChainParts(context, visualPos, rootPose, models(1, 7), STRIKER_RIGHT, 6);
        strikerLeft = new ChainParts(context, visualPos, rootPose, models(7, 9), STRIKER_LEFT, 6);
        jetArm = new ChainParts(context, visualPos, rootPose, models(9, 13), JET, 4);
        double[] bounds = new double[6];
        LightBounds.resetBounds(
                bounds,
                bodyBounds.minmax(
                        new AABB(
                                        pos.getX() - 5,
                                        pos.getY(),
                                        pos.getZ() - 5,
                                        pos.getX() + 5,
                                        pos.getY() + 6,
                                        pos.getZ() + 6)
                                .inflate(1)));
        LightBounds.includeLightBounds(bounds, BODY_PARTS[0].model(), boltsPose, pos);
        strikerRight.includeLightBounds(bounds, pos, PISTON_LOW, PISTON_HIGH);
        strikerLeft.includeLightBounds(bounds, pos, PISTON_LOW, PISTON_HIGH);
        jetArm.includeLightBounds(bounds, pos);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        writeArms();

        cold =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, COLD_PART.model())
                        .createInstance();
        plasma =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, PLASMA_PART.model())
                        .createInstance();
        glow1 =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, GLOW_PART.model())
                        .createInstance();
        glow2 =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, GLOW_PART.model())
                        .createInstance();
        flame1 =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, FLAME1_PART)
                        .createInstance();
        flame2 =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, FLAME2_PART)
                        .createInstance();
        iconBeam =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, ICON_PART)
                        .createInstance();
        updateFrame(partialTick);
    }

    public static void initModels() {}

    public static boolean vanillaNeeded(FusionPlasmaForgeBlockEntity be) {
        GenericRecipe recipe = be.module.getRecipe();
        return recipe != null
                && !WorldItem.drawsFramed(
                        FusionPlasmaForgeRenderer.icon(
                                recipe,
                                FusionPlasmaForgeRenderer.recipeDistanceSq(be.getBlockPos())));
    }

    private static MeshPart[] buildBodyParts() {
        MeshPart[] parts = new MeshPart[BODY_NAMES.length];
        for (int i = 0; i < parts.length; i++) {
            HFRWavefrontObject source = i == 0 ? TORUS_MODEL : MODEL;
            Material material =
                    i == 0 ? MeshPart.litCutout(ResourceManager.fusion_torus_tex) : BODY_MATERIAL;
            parts[i] =
                    MeshPart.obj(
                            source.groups[source.partId(BODY_NAMES[i])],
                            source.smoothing(),
                            material);
        }
        return parts;
    }

    private static Model[] models(int from, int to) {
        Model[] models = new Model[to - from];
        for (int i = from; i < to; i++) models[i - from] = BODY_PARTS[i].model();
        return models;
    }

    private static ChainInstance.Chain strikerArm(int joints) {
        var chain = new ChainInstance.Chain().rotateY(0, Mth.DEG_TO_RAD);
        if (joints > 1)
            chain.translate(-2.75F, 2.5F, 0F)
                    .rotateZ(1, -Mth.DEG_TO_RAD)
                    .translate(2.75F, -2.5F, 0F);
        if (joints > 2)
            chain.translate(-2.75F, 3.75F, 0F)
                    .rotateZ(2, -Mth.DEG_TO_RAD)
                    .translate(2.75F, -3.75F, 0F);
        if (joints > 3)
            chain.translate(-1.5F, 3.75F, 0F)
                    .rotateZ(3, -Mth.DEG_TO_RAD)
                    .translate(1.5F, -3.75F, 0F);
        return chain.end();
    }

    private static ChainInstance.Chain striker(float side) {
        return new ChainInstance.Chain()
                .rotateY(0, Mth.DEG_TO_RAD)
                .translate(-2.75F, 2.5F, 0F)
                .rotateZ(1, -Mth.DEG_TO_RAD)
                .translate(2.75F, -2.5F, 0F)
                .translate(-2.75F, 3.75F, 0F)
                .rotateZ(2, -Mth.DEG_TO_RAD)
                .translate(2.75F, -3.75F, 0F)
                .translate(-1.5F, 3.75F, 0F)
                .rotateZ(3, -Mth.DEG_TO_RAD)
                .translate(1.5F, -3.75F, 0F)
                .translate(0F, 3.375F, side * .5F)
                .rotateX(4, side * Mth.DEG_TO_RAD)
                .translate(0F, -3.375F, side * -.5F);
    }

    private static ChainInstance.Chain jetArm(int joints) {
        var chain = new ChainInstance.Chain().rotateY(0, Mth.DEG_TO_RAD);
        if (joints > 1)
            chain.translate(2.75F, 2.5F, 0F)
                    .rotateZ(1, Mth.DEG_TO_RAD)
                    .translate(-2.75F, -2.5F, 0F);
        if (joints > 2)
            chain.translate(2.75F, 3.75F, 0F)
                    .rotateZ(2, Mth.DEG_TO_RAD)
                    .translate(-2.75F, -3.75F, 0F);
        if (joints > 3)
            chain.translate(1.5F, 3.75F, 0F)
                    .rotateZ(3, Mth.DEG_TO_RAD)
                    .translate(-1.5F, -3.75F, 0F);
        return chain.end();
    }

    private static Mesh flame(float narrow) {
        float[] data = new float[16 * 8];
        int[] colors = new int[16];
        float side = .125F, near = 1.375F, far = 1.625F;
        float[][] points = {
            {near, 0F, side},
            {far, 0F, side},
            {far - narrow, -1F, side - narrow},
            {near + narrow, -1F, side - narrow},
            {near, 0F, -side},
            {far, 0F, -side},
            {far - narrow, -1F, -side + narrow},
            {near + narrow, -1F, -side + narrow},
            {near, 0F, side},
            {near, 0F, -side},
            {near + narrow, -1F, -side + narrow},
            {near + narrow, -1F, side - narrow},
            {far, 0F, side},
            {far, 0F, -side},
            {far - narrow, -1F, -side + narrow},
            {far - narrow, -1F, side - narrow}
        };
        for (int i = 0; i < points.length; i++) {
            int at = i * 8;
            System.arraycopy(points[i], 0, data, at, 3);
            data[at + 6] = 1F;
            colors[i] = (i & 3) < 2 ? -1 : 0x00FFFFFF;
        }
        return PackedQuadMesh.of(data, colors, new int[16]);
    }

    private static Mesh iconBeam() {
        float[] data = {
            -BEAM_IN,
            BEAM_BOTTOM,
            BEAM_IN,
            BEAM_TEX,
            0F,
            0F,
            1F,
            0F,
            -BEAM_IN,
            BEAM_TOP,
            BEAM_IN,
            0F,
            0F,
            0F,
            1F,
            0F,
            -BEAM_IN,
            BEAM_TOP,
            -BEAM_IN,
            0F,
            1F,
            0F,
            1F,
            0F,
            -BEAM_IN,
            BEAM_BOTTOM,
            -BEAM_IN,
            BEAM_TEX,
            1F,
            0F,
            1F,
            0F,
            BEAM_IN,
            BEAM_TOP,
            BEAM_IN,
            0F,
            0F,
            0F,
            1F,
            0F,
            BEAM_IN,
            BEAM_BOTTOM,
            BEAM_IN,
            BEAM_TEX,
            0F,
            0F,
            1F,
            0F,
            BEAM_IN,
            BEAM_BOTTOM,
            -BEAM_IN,
            BEAM_TEX,
            1F,
            0F,
            1F,
            0F,
            BEAM_IN,
            BEAM_TOP,
            -BEAM_IN,
            0F,
            1F,
            0F,
            1F,
            0F,
            BEAM_IN,
            BEAM_BOTTOM,
            BEAM_IN,
            BEAM_TEX,
            0F,
            0F,
            1F,
            0F,
            BEAM_IN,
            BEAM_TOP,
            BEAM_IN,
            0F,
            0F,
            0F,
            1F,
            0F,
            -BEAM_IN,
            BEAM_TOP,
            BEAM_IN,
            0F,
            1F,
            0F,
            1F,
            0F,
            -BEAM_IN,
            BEAM_BOTTOM,
            BEAM_IN,
            BEAM_TEX,
            1F,
            0F,
            1F,
            0F,
            BEAM_IN,
            BEAM_TOP,
            -BEAM_IN,
            0F,
            0F,
            0F,
            1F,
            0F,
            BEAM_IN,
            BEAM_BOTTOM,
            -BEAM_IN,
            BEAM_TEX,
            0F,
            0F,
            1F,
            0F,
            -BEAM_IN,
            BEAM_BOTTOM,
            -BEAM_IN,
            BEAM_TEX,
            1F,
            0F,
            1F,
            0F,
            -BEAM_IN,
            BEAM_TOP,
            -BEAM_IN,
            0F,
            1F,
            0F,
            1F,
            0F
        };
        int[] colors = new int[16];
        Arrays.fill(colors, -1);
        return PackedQuadMesh.of(data, colors, new int[16]);
    }

    private static int opaque(float r, float g, float b) {
        return ARGB.colorFromFloat(
                1F, Mth.clamp(r, 0F, 1F), Mth.clamp(g, 0F, 1F), Mth.clamp(b, 0F, 1F));
    }

    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    @Override
    public void frame(DynamicVisual.Context context) {
        updateFrame(context.partialTick());
    }

    @Override
    public void tick(TickableVisual.Context context) {
        writeArms();
    }

    private void writeArms() {
        FusionPlasmaForgeBlockEntity.ForgeArm striker = blockEntity.armStriker;
        FusionPlasmaForgeBlockEntity.ForgeArm jet = blockEntity.armJet;
        float prevRing = (float) blockEntity.prevRing, ring = (float) blockEntity.ring;
        strikerRight.set(0, prevRing, ring);
        strikerLeft.set(0, prevRing, ring);
        jetArm.set(0, prevRing, ring);
        for (int i = 0; i < 4; i++) {
            strikerRight.set(i + 1, (float) striker.prevAngles[i], (float) striker.angles[i]);
            strikerLeft.set(i + 1, (float) striker.prevAngles[i], (float) striker.angles[i]);
        }
        strikerRight.set(5, (float) striker.prevAngles[4], (float) striker.angles[4]);
        strikerLeft.set(5, (float) striker.prevAngles[5], (float) striker.angles[5]);
        for (int i = 0; i < 3; i++)
            jetArm.set(i + 1, (float) jet.prevAngles[i], (float) jet.angles[i]);
        strikerRight.write();
        strikerLeft.write();
        jetArm.write();
    }

    private void updateFrame(float partialTick) {
        GenericRecipe recipe = blockEntity.module.getRecipe();
        double distanceSq =
                recipe == null
                        ? Double.POSITIVE_INFINITY
                        : FusionPlasmaForgeRenderer.recipeDistanceSq(pos);
        double ticks = FusionPlasmaForgeRenderer.ticks(partialTick);
        writeFrame(partialTick, distanceSq <= FusionPlasmaForgeRenderer.BEAM_RANGE_SQ, ticks);
        ItemStack stack = FusionPlasmaForgeRenderer.icon(recipe, distanceSq);
        FramedItem.Arm arm = icon.setFramed(stack, level);
        if (arm == null) return;
        iconPoses.setIdentity();
        iconPoses.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
        iconPoses.translate(0.5, 0.0, 0.5);
        iconPoses.rotate((Quaternionfc) Axis.YP.rotationDegrees(yaw));
        iconPartialTick = partialTick;
        FusionPlasmaForgeRenderer.iconPose(
                iconPoses, arm, stack.getItem() instanceof BlockItem, ticks, iconWrite);
    }

    private void writeFrame(float partialTick, boolean beamInRange, double ticks) {
        long time =
                GameTime.millis(level)
                        + (long) (partialTick * 50F)
                        + Math.floorMod(pos.hashCode(), 30_000L);
        long energy = blockEntity.plasmaEnergySync;
        boolean jetFiring =
                blockEntity.didProcess
                        && blockEntity.armJet.angles[2] == blockEntity.armJet.prevAngles[2]
                        && blockEntity.armJet.angles[2] != 0D;
        boolean iconVisible = beamInRange && blockEntity.module.getRecipe() != null;
        boolean animated = energy > 0L || jetFiring || iconVisible;
        boolean first = !initialized;
        boolean changed =
                first
                        || blockEntity.connected != lastConnected
                        || energy != lastEnergy
                        || beamInRange != lastBeamInRange
                        || jetFiring != lastJetFiring
                        || (animated && time != lastTime)
                        || (iconVisible && ticks != lastTicks);
        if (!changed) return;
        lastTime = time;
        lastTicks = ticks;
        lastEnergy = energy;
        lastConnected = blockEntity.connected;
        lastBeamInRange = beamInRange;
        lastJetFiring = jetFiring;
        initialized = true;
        bolts.setVisible(blockEntity.connected);

        if (energy <= 0L) {
            if (first || !coldShown) {
                cold.setVisible(true);
                set(cold, rootPose, 0xFF000000, 0);
                plasma.setVisible(false);
                glow1.setVisible(false);
                glow2.setVisible(false);
                coldShown = true;
            }
        } else {
            if (first || coldShown) {
                cold.setVisible(false);
                coldShown = false;
            }
            float alpha = .5F + (float) Math.sin(time / 500D) * .25F;
            int baseColor =
                    opaque(
                            blockEntity.plasmaRed * alpha,
                            blockEntity.plasmaGreen * alpha,
                            blockEntity.plasmaBlue * alpha);
            plasma.setVisible(true);
            plasma.uvRegion(0F, (float) (sps(time / 750D) % 1D), 1F, 1F);
            set(plasma, rootPose, baseColor, 0);
            int glowColor =
                    opaque(
                            blockEntity.plasmaRed * 2F,
                            blockEntity.plasmaGreen * 2F,
                            blockEntity.plasmaBlue * 2F);
            glow1.setVisible(true);
            glow1.uvRegion(0F, (float) (Math.sin(time / 1000D) % 1D + time / 10_000D % 1D), 1F, 1F);
            set(glow1, rootPose, glowColor, LightCoordsUtil.FULL_BRIGHT);
            glow2.setVisible(true);
            glow2.uvRegion(
                    0F, (float) (Math.sin(time / 600D + 2D) % 1D + time / 5_000D % 1D), 1F, 1F);
            set(glow2, rootPose, glowColor, LightCoordsUtil.FULL_BRIGHT);
        }

        flame1.setVisible(jetFiring);
        flame2.setVisible(jetFiring);
        if (jetFiring) {
            jetEnd(partialTick);
            random.setSeed(time);
            float length = 1F + random.nextFloat() * .125F;
            flamePose.set(jetEndPose).translate(0F, 3F, 0F).scale(1F, length, 1F);
            set(
                    flame1,
                    flamePose,
                    opaque(blockEntity.plasmaRed, blockEntity.plasmaGreen, blockEntity.plasmaBlue),
                    0);
            flamePose.set(jetEndPose).translate(0F, 3F, 0F).scale(1F, length * 1.5F, 1F);
            set(
                    flame2,
                    flamePose,
                    opaque(blockEntity.plasmaRed, blockEntity.plasmaGreen, blockEntity.plasmaBlue),
                    0);
        }

        iconBeam.setVisible(iconVisible);
        if (iconVisible) {
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(rootPose);
            iconBeam.uvRegion((float) (ticks / 15D % 1D), 0F, 1F, 1F)
                    .setTransform(instancePose)
                    .light(0)
                    .setChanged();
        }
    }

    private void jetEnd(float partialTick) {
        blockEntity.armJet.getPositions(partialTick, jet);
        float ring =
                (float)
                                Mth.lerp(
                                        partialTick,
                                        (float) blockEntity.prevRing,
                                        (float) blockEntity.ring)
                        * Mth.DEG_TO_RAD;
        jetEndPose
                .set(rootPose)
                .rotateY(ring)
                .translate(2.75F, 2.5F, 0F)
                .rotateZ((float) jet[0] * Mth.DEG_TO_RAD)
                .translate(-2.75F, -2.5F, 0F)
                .translate(2.75F, 3.75F, 0F)
                .rotateZ((float) jet[1] * Mth.DEG_TO_RAD)
                .translate(-2.75F, -3.75F, 0F)
                .translate(1.5F, 3.75F, 0F)
                .rotateZ((float) jet[2] * Mth.DEG_TO_RAD)
                .translate(-1.5F, -3.75F, 0F);
    }

    private void set(TransformedInstance instance, Matrix4f local, int color, int light) {
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        instance.setTransform(instancePose).colorArgb(color).light(light).setChanged();
    }

    private void set(UvTransformedInstance instance, Matrix4f local, int color, int light) {
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        instance.setTransform(instancePose).colorArgb(color).light(light).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return lightBounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(bolts);
        strikerRight.collectCrumblingInstances(consumer);
        strikerLeft.collectCrumblingInstances(consumer);
        jetArm.collectCrumblingInstances(consumer);
        consumer.accept(cold);
    }

    @Override
    protected void _delete() {
        bolts.delete();
        strikerRight.delete();
        strikerLeft.delete();
        jetArm.delete();
        cold.delete();
        plasma.delete();
        glow1.delete();
        glow2.delete();
        flame1.delete();
        flame2.delete();
        iconBeam.delete();
        icon.delete();
    }
}
