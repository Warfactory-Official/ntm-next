// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.GroupObject;
import com.hbm.tileentity.machine.BlockEntityReactorResearch;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class SmallReactorVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityReactorResearch>
        implements ShaderLightVisual {
    private static final float BODY_YAW = 180F;
    private static final double GLOW_MIN = .285D, GLOW_STEP = .025D;
    private static final double GLOW_TOP = 1.375D, GLOW_BOTTOM = 1.375D;
    private static final int GLOW_COLOR = 0x66E5FF;
    private static final int GLOW_FLUX = 10;
    private static final int GLOW_SHELLS = 17;
    private static final MeshPart[] ROD_PARTS = buildRods();
    private static final Model[] GLOW_MODELS = buildGlow();
    private final TransformedInstance[] rods;
    private final TransformedInstance[] shells = new TransformedInstance[GLOW_SHELLS];
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f rodPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final Matrix4f shellPose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private final AABB rawBodyBounds;
    private final int[] lastShellColors = new int[GLOW_SHELLS];
    private final Random glowRandom = new Random();
    private @Nullable AABB lastLightBounds;
    private float lastControl = Float.NaN;
    private boolean lastGlowing;
    private boolean initialized;

    public SmallReactorVisual(
            VisualizationContext context,
            BlockEntityReactorResearch blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        var rawBodyLocal =
                new Matrix4f().translation(.5F, 0F, .5F).rotateY(BODY_YAW * Mth.DEG_TO_RAD);
        var rawBodyExtent = new AABB(pos);
        for (GroupObject group : ResourceManager.reactor_small_base.groups)
            rawBodyExtent = rawBodyExtent.minmax(LightBounds.of(group, rawBodyLocal, pos));
        rawBodyBounds = rawBodyExtent;
        basePose.set(rawBodyLocal);
        shellPose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(basePose);
        rods = new TransformedInstance[ROD_PARTS.length];
        for (int i = 0; i < ROD_PARTS.length; i++)
            rods[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, ROD_PARTS[i].model())
                            .createInstance();
        for (int i = 0; i < shells.length; i++) {
            shells[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, GLOW_MODELS[i])
                            .createInstance();
            shells[i].setVisible(false);
        }
        writeFrame(partialTick);
    }

    public static void initModels() {}

    private static MeshPart[] buildRods() {
        Material rodMaterial =
                SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                        .texture(ResourceManager.reactor_small_rods_tex)
                        .mipmap(false)
                        .cutout(CutoutShaders.ONE_TENTH)
                        .light(LightShaders.SMOOTH)
                        .ambientOcclusion(false)
                        .cardinalLightingMode(CardinalLightingMode.CHUNK)
                        .build();
        return MeshPart.objParts(ResourceManager.reactor_small_rods, rodMaterial);
    }

    private static Model[] buildGlow() {
        Material glowMaterial =
                SimpleMaterial.builderOf(Materials.ADDITIVE_NO_CULL)
                        .texture(EffectVisuals.Shared.WHITE)
                        .mipmap(false)
                        .cutout(CutoutShaders.OFF)
                        .light(LightShaders.SMOOTH)
                        .useLight(true)
                        .useOverlay(false)
                        .ambientOcclusion(false)
                        .cardinalLightingMode(CardinalLightingMode.OFF)
                        .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                        .writeMask(WriteMask.COLOR)
                        .depthTest(DepthTest.LEQUAL)
                        .build();
        var models = new Model[GLOW_SHELLS];
        for (int i = 0; i < models.length; i++)
            models[i] = new SingleMeshModel(shell(GLOW_MIN + i * GLOW_STEP), glowMaterial);
        return models;
    }

    private static Mesh shell(double d) {
        float[] vertices = new float[24 * 8];
        int[] colors = new int[24];
        int[] lights = new int[24];
        double top = GLOW_TOP + d, bottom = GLOW_BOTTOM - d;
        int[] index = {0};
        face(vertices, index, d, bottom, -d, d, top, -d, d, top, d, d, bottom, d);
        face(vertices, index, -d, bottom, -d, -d, top, -d, -d, top, d, -d, bottom, d);
        face(vertices, index, -d, bottom, d, -d, top, d, d, top, d, d, bottom, d);
        face(vertices, index, -d, bottom, -d, -d, top, -d, d, top, -d, d, bottom, -d);
        face(vertices, index, -d, top, -d, -d, top, d, d, top, d, d, top, -d);
        face(vertices, index, -d, bottom, -d, -d, bottom, d, d, bottom, d, d, bottom, -d);
        Arrays.fill(colors, -1);
        return PackedQuadMesh.of(vertices, colors, lights);
    }

    private static void face(
            float[] vertices,
            int[] index,
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            double x3,
            double y3,
            double z3) {
        point(vertices, index, x0, y0, z0);
        point(vertices, index, x1, y1, z1);
        point(vertices, index, x2, y2, z2);
        point(vertices, index, x3, y3, z3);
    }

    private static void point(float[] vertices, int[] index, double x, double y, double z) {
        int at = index[0]++ * 8;
        vertices[at] = (float) x;
        vertices[at + 1] = (float) y;
        vertices[at + 2] = (float) z;
        vertices[at + 6] = 1F;
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        float control =
                Mth.lerp(
                        partialTick,
                        (float) blockEntity.lastControlLevel,
                        (float) blockEntity.controlLevel);
        boolean controlChanged =
                !initialized || Float.floatToIntBits(control) != Float.floatToIntBits(lastControl);
        if (controlChanged) {
            rodPose.set(basePose).translate(0F, control, 0F);
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(rodPose);
            for (TransformedInstance rod : rods)
                rod.setTransform(instancePose).light(0).setChanged();
            LightBounds.resetBounds(lightBounds, rawBodyBounds);
            for (MeshPart rod : ROD_PARTS)
                LightBounds.includeLightBounds(lightBounds, rod.model(), rodPose, pos);
            lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
            lastControl = control;
        }

        boolean glowing = blockEntity.totalFlux > GLOW_FLUX && blockEntity.isSubmerged();
        if (glowing) {
            glowRandom.setSeed(GameTime.millis(blockEntity.getLevel()) + pos.hashCode());
            for (int i = 0; i < shells.length; i++) {
                float alpha =
                        .025F
                                + glowRandom.nextFloat() * .015F
                                + .125F * blockEntity.totalFlux / 1000F;
                int color =
                        ARGB.color(
                                Mth.clamp((int) (Mth.clamp(alpha, 0F, 1F) * 255F), 0, 255),
                                GLOW_COLOR);
                if (!lastGlowing || color != lastShellColors[i]) {
                    shells[i].setVisible(true);
                    if (!lastGlowing) shells[i].setTransform(shellPose).light(0);
                    shells[i].colorArgb(color).setChanged();
                }
                lastShellColors[i] = color;
            }
        } else if (!initialized || lastGlowing) {
            for (TransformedInstance shell : shells) shell.setVisible(false);
        }
        lastGlowing = glowing;
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(
                new AABB(
                                pos.getX() - 1,
                                pos.getY(),
                                pos.getZ() - 1,
                                pos.getX() + 2,
                                pos.getY() + 5,
                                pos.getZ() + 2)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var rod : rods) consumer.accept(rod);
    }

    @Override
    protected void _delete() {
        for (var rod : rods) rod.delete();
        for (var shell : shells) shell.delete();
    }
}
