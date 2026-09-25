// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.render.FramedItem;
import com.hbm.client.render.RenderFusionPlasmaForge;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionPlasmaForge;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
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
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
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
import org.jspecify.annotations.Nullable;

public final class FusionPlasmaForgeVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityFusionPlasmaForge>
        implements ShaderLightVisual {
    private static final float BASE_YAW = 90F;
    private static final float BEAM_IN = .4375F;
    private static final float BEAM_BOTTOM = 1F;
    private static final float BEAM_TEX = 1.5F;
    private static final float BEAM_TOP = BEAM_BOTTOM + BEAM_TEX;
    private static final Identifier STELLAR_FLUX =
            Library.id("textures/gui/fluids/stellar_flux.png");
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
    private static final MeshPart COLD_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Plasma")], MODEL.smoothing(), COLD_MATERIAL);
    private static final MeshPart PLASMA_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Plasma")], MODEL.smoothing(), PLASMA_MATERIAL);
    private static final MeshPart GLOW_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Plasma")], MODEL.smoothing(), GLOW_MATERIAL);
    private static final Model FLAME1_PART = new SingleMeshModel(flame(.01F), FLAME_MATERIAL);
    private static final Model FLAME2_PART = new SingleMeshModel(flame(.09375F), FLAME_MATERIAL);
    private static final Model ICON_PART = new SingleMeshModel(iconBeam(), BEAM_MATERIAL);
    private final AABB bodyBounds;
    private final PartVisual[] bodyParts;
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
    private final Matrix4f ringPose = new Matrix4f();
    private final Matrix4f armPose = new Matrix4f();
    private final Matrix4f rightArmPose = new Matrix4f();
    private final Matrix4f leftArmPose = new Matrix4f();
    private final double[] striker = new double[6];
    private final double[] jet = new double[4];
    private final double[] lastStriker = new double[6];
    private final double[] lastJet = new double[4];
    private final double[] lightBoundsAccumulator = new double[6];
    private final Random random = new Random();
    private float lastRing = Float.NaN;
    private long lastTime = Long.MIN_VALUE;
    private double lastTicks = Double.NaN;
    private long lastEnergy = Long.MIN_VALUE;
    private boolean lastConnected;
    private boolean lastBeamInRange;
    private boolean lastJetFiring;
    private boolean coldShown;
    private boolean initialized;
    private @Nullable AABB lastLightBounds;
    private final float yaw;
    private final WorldItem icon = new WorldItem(visualizationContext, level, pos);
    private final PoseStack iconPoses = new PoseStack();
    private float iconPartialTick;
    private final Consumer<PoseStack> iconWrite =
            poses -> icon.write(poses.last().pose(), iconPartialTick);

    public FusionPlasmaForgeVisual(
            VisualizationContext context,
            BlockEntityFusionPlasmaForge blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        yaw = BASE_YAW + Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 90);
        rootPose.translation(.5F, 0F, .5F).rotateY(yaw * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(MODEL, "Body", rootPose, pos);
        bodyParts = new PartVisual[BODY_PARTS.length];
        for (int i = 0; i < bodyParts.length; i++) bodyParts[i] = new PartVisual(BODY_PARTS[i]);

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

    public static boolean vanillaNeeded(BlockEntityFusionPlasmaForge be) {
        GenericRecipe recipe = be.module.getRecipe();
        return recipe != null
                && !WorldItem.drawsFramed(
                        RenderFusionPlasmaForge.icon(
                                recipe,
                                RenderFusionPlasmaForge.recipeDistanceSq(be.getBlockPos())));
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
    protected void frame(Context context) {
        updateFrame(context.partialTick());
    }

    private void updateFrame(float partialTick) {
        GenericRecipe recipe = blockEntity.module.getRecipe();
        double distanceSq =
                recipe == null
                        ? Double.POSITIVE_INFINITY
                        : RenderFusionPlasmaForge.recipeDistanceSq(pos);
        double ticks = RenderFusionPlasmaForge.ticks(partialTick);
        writeFrame(partialTick, distanceSq <= RenderFusionPlasmaForge.BEAM_RANGE_SQ, ticks);
        ItemStack stack = RenderFusionPlasmaForge.icon(recipe, distanceSq);
        FramedItem.Arm arm = icon.setFramed(stack, level);
        if (arm == null) return;
        iconPoses.setIdentity();
        iconPoses.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
        iconPoses.translate(0.5, 0.0, 0.5);
        iconPoses.mulPose(Axis.YP.rotationDegrees(yaw));
        iconPartialTick = partialTick;
        RenderFusionPlasmaForge.iconPose(
                iconPoses, arm, stack.getItem() instanceof BlockItem, ticks, iconWrite);
    }

    private void writeFrame(float partialTick, boolean beamInRange, double ticks) {
        blockEntity.armStriker.getPositions(partialTick, striker);
        blockEntity.armJet.getPositions(partialTick, jet);
        float ring =
                (float)
                                Mth.lerp(
                                        partialTick,
                                        (float) blockEntity.prevRing,
                                        (float) blockEntity.ring)
                        * Mth.DEG_TO_RAD;
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
        boolean armsChanged =
                first
                        || Float.compare(ring, lastRing) != 0
                        || blockEntity.connected != lastConnected
                        || !Arrays.equals(striker, lastStriker)
                        || !Arrays.equals(jet, lastJet);
        boolean changed =
                armsChanged
                        || energy != lastEnergy
                        || beamInRange != lastBeamInRange
                        || jetFiring != lastJetFiring
                        || (animated && time != lastTime)
                        || (iconVisible && ticks != lastTicks);
        if (!changed) return;
        lastRing = ring;
        lastTime = time;
        lastTicks = ticks;
        lastEnergy = energy;
        lastConnected = blockEntity.connected;
        lastBeamInRange = beamInRange;
        lastJetFiring = jetFiring;
        initialized = true;
        if (armsChanged) {
            System.arraycopy(striker, 0, lastStriker, 0, striker.length);
            System.arraycopy(jet, 0, lastJet, 0, jet.length);
            LightBounds.resetBounds(lightBoundsAccumulator, bodyBounds);
            bodyParts[0].write(blockEntity.connected, armPose.set(rootPose).translate(-2F, 0F, 0F));
            bodyParts[0].include(lightBoundsAccumulator, armPose);
            ringPose.set(rootPose).rotateY(ring);
            armStriker(ringPose, striker);
            armJet(ringPose, jet);
            lastLightBounds =
                    LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
        }

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

    private void armStriker(Matrix4f base, double[] a) {
        Matrix4f pose = armPose.set(base);
        bodyParts[1].write(true, pose);
        bodyParts[1].include(lightBoundsAccumulator, pose);
        pose.translate(-2.75F, 2.5F, 0F)
                .rotateZ((float) -a[0] * Mth.DEG_TO_RAD)
                .translate(2.75F, -2.5F, 0F);
        bodyParts[2].write(true, pose);
        bodyParts[2].include(lightBoundsAccumulator, pose);
        pose.translate(-2.75F, 3.75F, 0F)
                .rotateZ((float) -a[1] * Mth.DEG_TO_RAD)
                .translate(2.75F, -3.75F, 0F);
        bodyParts[3].write(true, pose);
        bodyParts[3].include(lightBoundsAccumulator, pose);
        pose.translate(-1.5F, 3.75F, 0F)
                .rotateZ((float) -a[2] * Mth.DEG_TO_RAD)
                .translate(1.5F, -3.75F, 0F);
        bodyParts[4].write(true, pose);
        bodyParts[4].include(lightBoundsAccumulator, pose);
        Matrix4f right =
                rightArmPose
                        .set(pose)
                        .translate(0F, 3.375F, .5F)
                        .rotateX((float) a[3] * Mth.DEG_TO_RAD)
                        .translate(0F, -3.375F, -.5F);
        bodyParts[5].write(true, right);
        bodyParts[5].include(lightBoundsAccumulator, right);
        right.translate(0F, (float) -a[4], 0F);
        bodyParts[6].write(true, right);
        bodyParts[6].include(lightBoundsAccumulator, right);
        Matrix4f left =
                leftArmPose
                        .set(pose)
                        .translate(0F, 3.375F, -.5F)
                        .rotateX((float) -a[3] * Mth.DEG_TO_RAD)
                        .translate(0F, -3.375F, .5F);
        bodyParts[7].write(true, left);
        bodyParts[7].include(lightBoundsAccumulator, left);
        left.translate(0F, (float) -a[5], 0F);
        bodyParts[8].write(true, left);
        bodyParts[8].include(lightBoundsAccumulator, left);
    }

    private void armJet(Matrix4f base, double[] a) {
        Matrix4f pose = armPose.set(base);
        bodyParts[9].write(true, pose);
        bodyParts[9].include(lightBoundsAccumulator, pose);
        pose.translate(2.75F, 2.5F, 0F)
                .rotateZ((float) a[0] * Mth.DEG_TO_RAD)
                .translate(-2.75F, -2.5F, 0F);
        bodyParts[10].write(true, pose);
        bodyParts[10].include(lightBoundsAccumulator, pose);
        pose.translate(2.75F, 3.75F, 0F)
                .rotateZ((float) a[1] * Mth.DEG_TO_RAD)
                .translate(-2.75F, -3.75F, 0F);
        bodyParts[11].write(true, pose);
        bodyParts[11].include(lightBoundsAccumulator, pose);
        pose.translate(1.5F, 3.75F, 0F)
                .rotateZ((float) a[2] * Mth.DEG_TO_RAD)
                .translate(-1.5F, -3.75F, 0F);
        bodyParts[12].write(true, pose);
        bodyParts[12].include(lightBoundsAccumulator, pose);
        jetEndPose.set(pose);
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
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 5,
                                pos.getY(),
                                pos.getZ() - 5,
                                pos.getX() + 5,
                                pos.getY() + 6,
                                pos.getZ() + 6)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var part : bodyParts) consumer.accept(part.instance);
        consumer.accept(cold);
    }

    @Override
    protected void _delete() {
        for (var part : bodyParts) part.instance.delete();
        cold.delete();
        plasma.delete();
        glow1.delete();
        glow2.delete();
        flame1.delete();
        flame2.delete();
        iconBeam.delete();
        icon.delete();
    }

    private final class PartVisual {
        private final MeshPart part;
        private final TransformedInstance instance;

        private PartVisual(MeshPart part) {
            this.part = part;
            instance =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, part.model())
                            .createInstance();
        }

        private void write(boolean shown, Matrix4f local) {
            instance.setVisible(shown);
            if (!shown) return;
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(local);
            instance.setTransform(instancePose).light(0).setChanged();
        }

        private void include(double[] bounds, Matrix4f local) {
            LightBounds.includeLightBounds(bounds, part.model(), local, pos);
        }
    }
}
