// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.render.flywheel.HbmBlockEntityVisual;
import com.hbm.client.render.flywheel.WorldItem;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionPlasmaForge;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderFusionPlasmaForge
        implements BlockEntityRenderer<BlockEntityFusionPlasmaForge, RenderFusionPlasmaForge.State>,
                ConcurrentRenderStateExtraction {
    private static final int ARM_LOWER_JET =
            ResourceManager.fusion_plasma_forge.partId("ArmLowerJet");
    private static final int ARM_LOWER_STRIKER =
            ResourceManager.fusion_plasma_forge.partId("ArmLowerStriker");
    private static final int ARM_UPPER_JET =
            ResourceManager.fusion_plasma_forge.partId("ArmUpperJet");
    private static final int ARM_UPPER_STRIKER =
            ResourceManager.fusion_plasma_forge.partId("ArmUpperStriker");
    private static final int JET = ResourceManager.fusion_plasma_forge.partId("Jet");
    private static final int PISTON_LEFT = ResourceManager.fusion_plasma_forge.partId("PistonLeft");
    private static final int PISTON_RIGHT =
            ResourceManager.fusion_plasma_forge.partId("PistonRight");
    private static final int PLASMA = ResourceManager.fusion_plasma_forge.partId("Plasma");
    private static final int SLIDER_JET = ResourceManager.fusion_plasma_forge.partId("SliderJet");
    private static final int SLIDER_STRIKER =
            ResourceManager.fusion_plasma_forge.partId("SliderStriker");
    private static final int STRIKER_LEFT =
            ResourceManager.fusion_plasma_forge.partId("StrikerLeft");
    private static final int STRIKER_MOUNT =
            ResourceManager.fusion_plasma_forge.partId("StrikerMount");
    private static final int STRIKER_RIGHT =
            ResourceManager.fusion_plasma_forge.partId("StrikerRight");
    private static final int BOLTS1 = ResourceManager.fusion_torus.partId("Bolts1");

    private static final float BASE_YAW = 90F;

    public static final double ICON_RANGE_SQ = 35.0 * 35.0;
    public static final double BEAM_RANGE_SQ = 50.0 * 50.0;
    private static final RenderType BEAM_TYPE =
            FusionPlasmaRenderTypes.beam(Library.id("textures/gui/fluids/stellar_flux.png"));

    private static final float BEAM_IN = 0.4375F;
    private static final float BEAM_BOTTOM = 1F;
    private static final float BEAM_TEX = 1.5F;
    private static final float BEAM_TOP = BEAM_BOTTOM + BEAM_TEX;
    private static final int BEAM_OPAQUE = 0xFFFFFFFF;
    private static final int BEAM_CLEAR = 0x00FFFFFF;

    private final HFRWavefrontObject model = ResourceManager.fusion_plasma_forge;
    private final HFRWavefrontObject torus = ResourceManager.fusion_torus;

    private final RenderType baseType =
            RenderTypes.entityCutoutCull(ResourceManager.fusion_plasma_forge_tex);
    private final RenderType torusType =
            RenderTypes.entityCutoutCull(ResourceManager.fusion_torus_tex);
    private final RenderType plasmaBaseType =
            FusionPlasmaRenderTypes.plasmaBase(ResourceManager.fusion_plasma_tex);
    private final RenderType glowType =
            FusionPlasmaRenderTypes.plasma(ResourceManager.fusion_plasma_glow_tex);
    private final ItemModelResolver itemModelResolver;

    public RenderFusionPlasmaForge(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    public static double recipeDistanceSq(BlockPos pos) {
        LocalPlayer player = Minecraft.getInstance().player;
        double dx = pos.getX() + 0.5 - player.getX();
        double dy = pos.getY() + 1 - player.getEyeY();
        double dz = pos.getZ() + 0.5 - player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    public static ItemStack icon(@Nullable GenericRecipe recipe, double distanceSq) {
        return recipe != null && distanceSq <= ICON_RANGE_SQ ? recipe.getIcon() : ItemStack.EMPTY;
    }

    public static double ticks(float partialTicks) {
        return Minecraft.getInstance().player.tickCount + partialTicks;
    }

    public static void iconPose(
            PoseStack ps,
            FramedItem.Arm arm,
            boolean blockItem,
            double ticks,
            Consumer<PoseStack> draw) {
        ps.mulPose(Axis.YP.rotationDegrees(90F));
        ps.translate(0F, 1.75F, 0F);

        boolean rendered3d = arm == FramedItem.Arm.BLOCK;
        if (rendered3d) {
            ps.translate(0F, -0.0625F, 0F);
        } else if (blockItem) {
            ps.scale(0.5F, 0.5F, 0.5F);
        } else {
            ps.mulPose(Axis.YP.rotationDegrees(90F));
        }

        ps.translate(0.0, Math.sin(ticks * 0.1) * 0.0625, 0.0);
        ps.scale(1.5F, 1.5F, 1.5F);

        if (arm.mesh()) {
            ps.translate(0F, arm.groundLift(), 0F);
            draw.accept(ps);
        } else if (rendered3d) {
            FramedItem.framedBlockPrefix(ps);
            draw.accept(ps);
        } else {
            FramedItem.framedSpritePrefix(ps);
            FramedItem.framedSpriteAnchor(ps);

            FramedItem.framedSpriteCopies(ps, 1, draw);
        }
    }

    private static void pivot(
            PoseStack ps, double x, double y, double z, Axis axis, float degrees) {
        ps.translate(x, y, z);
        ps.mulPose(axis.rotationDegrees(degrees));
        ps.translate(-x, -y, -z);
    }

    private static void shell(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            double outerLen,
            double narrow,
            int hot,
            int cold,
            int light) {
        double side = 0.125;
        double near = 1.375;
        double far = 1.625;
        double tip = 3 - outerLen;

        quad(
                pose,
                buffer,
                hot,
                cold,
                light,
                near,
                3,
                side,
                far,
                3,
                side,
                far - narrow,
                tip,
                side - narrow,
                near + narrow,
                tip,
                side - narrow);
        quad(
                pose,
                buffer,
                hot,
                cold,
                light,
                near,
                3,
                -side,
                far,
                3,
                -side,
                far - narrow,
                tip,
                -side + narrow,
                near + narrow,
                tip,
                -side + narrow);
        quad(
                pose,
                buffer,
                hot,
                cold,
                light,
                near,
                3,
                side,
                near,
                3,
                -side,
                near + narrow,
                tip,
                -side + narrow,
                near + narrow,
                tip,
                side - narrow);
        quad(
                pose,
                buffer,
                hot,
                cold,
                light,
                far,
                3,
                side,
                far,
                3,
                -side,
                far - narrow,
                tip,
                -side + narrow,
                far - narrow,
                tip,
                side - narrow);
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityFusionPlasmaForge be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 5.375,
                pos.getY() - 0.125,
                pos.getZ() - 5.375,
                pos.getX() + 6.375,
                pos.getY() + 6,
                pos.getZ() + 6.375);
    }

    private static void quad(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int hot,
            int cold,
            int light,
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
        vertex(pose, buffer, x0, y0, z0, hot, light);
        vertex(pose, buffer, x1, y1, z1, hot, light);
        vertex(pose, buffer, x2, y2, z2, cold, light);
        vertex(pose, buffer, x3, y3, z3, cold, light);
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            double x,
            double y,
            double z,
            int color,
            int light) {
        Vertices.emit(
                buffer, pose, (float) x, (float) y, (float) z, color, 0F, 0F, light, 0F, 0F, 1F);
    }

    private static int opaque(float r, float g, float b) {
        return ARGB.colorFromFloat(1F, clamp(r), clamp(g), clamp(b));
    }

    private static float clamp(float v) {
        return Mth.clamp(v, 0F, 1F);
    }

    private static void beamVertex(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            float x,
            float y,
            float z,
            float u,
            float v,
            int light) {
        Vertices.emit(
                buffer,
                pose,
                x,
                y,
                z,
                y == BEAM_BOTTOM ? BEAM_OPAQUE : BEAM_CLEAR,
                u,
                v,
                light,
                0F,
                1F,
                0F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityFusionPlasmaForge be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.itemsOnly = HbmBlockEntityVisual.hasVisual(be);

        state.yaw = BASE_YAW + FusionYaw.of(BlockMultiblockCore.coreFacing(be.getBlockState()));
        extractRecipe(be, state, partialTicks);
        if (state.itemsOnly) return;

        state.connected = be.connected;
        state.ring = (float) Mth.lerp(partialTicks, be.prevRing, be.ring);
        be.armStriker.getPositions(partialTicks, state.striker);
        be.armJet.getPositions(partialTicks, state.jet);

        state.plasmaEnergy = be.plasmaEnergySync;
        state.r = be.plasmaRed;
        state.g = be.plasmaGreen;
        state.b = be.plasmaBlue;
        state.timeMs = GameTime.now() + Math.floorMod(be.getBlockPos().hashCode(), 30_000);

        state.jetFiring =
                be.didProcess
                        && be.armJet.angles[2] == be.armJet.prevAngles[2]
                        && be.armJet.angles[2] != 0;
        state.jetLength = 1D + ThreadLocalRandom.current().nextDouble() * 0.125D;
    }

    private void extractRecipe(BlockEntityFusionPlasmaForge be, State state, float partialTicks) {
        GenericRecipe recipe = be.module.getRecipe();
        double distanceSq =
                recipe == null ? Double.POSITIVE_INFINITY : recipeDistanceSq(be.getBlockPos());
        state.ticks = ticks(partialTicks);

        ItemStack icon = icon(recipe, distanceSq);
        if (state.itemsOnly && WorldItem.drawsFramed(icon)) icon = ItemStack.EMPTY;
        state.iconBlockItem = icon.getItem() instanceof BlockItem;
        state.iconArm =
                FramedItem.resolve(itemModelResolver, state.icon, icon, be.getLevel(), null, 0);

        state.beamType = !state.itemsOnly && distanceSq <= BEAM_RANGE_SQ ? BEAM_TYPE : null;
        state.beamOffset = (float) (state.ticks / 15D % 1D);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        if (s.connected && !s.itemsOnly) {
            ps.pushPose();
            ps.translate(-2.0, 0.0, 0.0);
            col.submitCustomGeometry(
                    ps,
                    torusType,
                    (pose, buffer) -> torus.renderPart(pose, buffer, light, -1, BOLTS1));
            ps.popPose();
        }

        if (!s.itemsOnly) submitPlasma(s, ps, col);
        submitIcon(s, ps, col);
        submitBeam(s, ps, col);

        if (!s.itemsOnly) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(s.ring));
            submitStrikerArm(s, ps, col, light);
            submitJetArm(s, ps, col, light);
            ps.popPose();
        }

        ps.popPose();
    }

    private void submitIcon(State s, PoseStack ps, SubmitNodeCollector col) {
        if (s.icon.isEmpty()) return;

        ps.pushPose();
        iconPose(
                ps,
                s.iconArm,
                s.iconBlockItem,
                s.ticks,
                copy -> s.icon.submit(copy, col, s.lightCoords, OverlayTexture.NO_OVERLAY, 0));
        ps.popPose();
    }

    private void submitBeam(State s, PoseStack ps, SubmitNodeCollector col) {
        RenderType type = s.beamType;
        if (type == null) return;

        float in = BEAM_IN, b = BEAM_BOTTOM, h = BEAM_TOP;
        float lo = s.beamOffset, hi = s.beamOffset + BEAM_TEX;
        int light = s.lightCoords;
        col.submitCustomGeometry(
                ps,
                type,
                (pose, buffer) -> {
                    beamVertex(pose, buffer, -in, b, in, hi, 0F, light);
                    beamVertex(pose, buffer, -in, h, in, lo, 0F, light);
                    beamVertex(pose, buffer, -in, h, -in, lo, 1F, light);
                    beamVertex(pose, buffer, -in, b, -in, hi, 1F, light);

                    beamVertex(pose, buffer, in, h, in, lo, 0F, light);
                    beamVertex(pose, buffer, in, b, in, hi, 0F, light);
                    beamVertex(pose, buffer, in, b, -in, hi, 1F, light);
                    beamVertex(pose, buffer, in, h, -in, lo, 1F, light);

                    beamVertex(pose, buffer, in, b, in, hi, 0F, light);
                    beamVertex(pose, buffer, in, h, in, lo, 0F, light);
                    beamVertex(pose, buffer, -in, h, in, lo, 1F, light);
                    beamVertex(pose, buffer, -in, b, in, hi, 1F, light);

                    beamVertex(pose, buffer, in, h, -in, lo, 0F, light);
                    beamVertex(pose, buffer, in, b, -in, hi, 0F, light);
                    beamVertex(pose, buffer, -in, b, -in, hi, 1F, light);
                    beamVertex(pose, buffer, -in, h, -in, lo, 1F, light);
                });
    }

    private void submitStrikerArm(State s, PoseStack ps, SubmitNodeCollector col, int light) {
        double[] a = s.striker;
        ps.pushPose();

        part(col, ps, light, SLIDER_STRIKER);

        pivot(ps, -2.75, 2.5, 0, Axis.ZP, (float) -a[0]);
        part(col, ps, light, ARM_LOWER_STRIKER);

        pivot(ps, -2.75, 3.75, 0, Axis.ZP, (float) -a[1]);
        part(col, ps, light, ARM_UPPER_STRIKER);

        pivot(ps, -1.5, 3.75, 0, Axis.ZP, (float) -a[2]);
        part(col, ps, light, STRIKER_MOUNT);

        ps.pushPose();
        pivot(ps, 0, 3.375, 0.5, Axis.XP, (float) a[3]);
        part(col, ps, light, STRIKER_RIGHT);
        ps.translate(0.0, -a[4], 0.0);
        part(col, ps, light, PISTON_RIGHT);
        ps.popPose();

        ps.pushPose();
        pivot(ps, 0, 3.375, -0.5, Axis.XP, (float) -a[3]);
        part(col, ps, light, STRIKER_LEFT);
        ps.translate(0.0, -a[5], 0.0);
        part(col, ps, light, PISTON_LEFT);
        ps.popPose();

        ps.popPose();
    }

    private void submitJetArm(State s, PoseStack ps, SubmitNodeCollector col, int light) {
        double[] a = s.jet;
        ps.pushPose();

        part(col, ps, light, SLIDER_JET);

        pivot(ps, 2.75, 2.5, 0, Axis.ZP, (float) a[0]);
        part(col, ps, light, ARM_LOWER_JET);

        pivot(ps, 2.75, 3.75, 0, Axis.ZP, (float) a[1]);
        part(col, ps, light, ARM_UPPER_JET);

        pivot(ps, 1.5, 3.75, 0, Axis.ZP, (float) a[2]);
        part(col, ps, light, JET);

        if (s.jetFiring) submitJetFlame(s, ps, col);

        ps.popPose();
    }

    private void submitPlasma(State s, PoseStack ps, SubmitNodeCollector col) {
        if (s.plasmaEnergy <= 0) {
            col.submitCustomGeometry(
                    ps,
                    FusionPlasmaRenderTypes.UNTEXTURED_OPAQUE,
                    (pose, buffer) ->
                            model.renderPart(pose, buffer, s.lightCoords, 0xFF000000, PLASMA));
            return;
        }

        double time = s.timeMs;
        float alpha = 0.5F + (float) (Math.sin(time / 500D) * 0.25F);
        double mainOsc = sps(time / 750D) % 1D;
        double glowOsc = Math.sin(time / 1000D) % 1D;
        double glowExtra = time / 10000D % 1D;

        int light = 0xF000F0;

        layer(
                col,
                ps,
                plasmaBaseType,
                light,
                opaque(s.r * alpha, s.g * alpha, s.b * alpha),
                0F,
                (float) mainOsc);
        layer(
                col,
                ps,
                glowType,
                light,
                opaque(s.r * 2F, s.g * 2F, s.b * 2F),
                0F,
                (float) (glowOsc + glowExtra));

        double glowOsc2 = Math.sin(time / 600D + 2) % 1D;
        double glowExtra2 = time / 5000D % 1D;
        layer(
                col,
                ps,
                glowType,
                light,
                opaque(s.r * 2F, s.g * 2F, s.b * 2F),
                0F,
                (float) (glowOsc2 + glowExtra2));
    }

    private void submitJetFlame(State s, PoseStack ps, SubmitNodeCollector col) {
        int hot = ARGB.colorFromFloat(1F, clamp(s.r), clamp(s.g), clamp(s.b));
        int cold = ARGB.colorFromFloat(0F, clamp(s.r), clamp(s.g), clamp(s.b));

        col.submitCustomGeometry(
                ps,
                FusionPlasmaRenderTypes.beam(RenderTextures.WHITE),
                (pose, buffer) -> {
                    shell(pose, buffer, s.jetLength, 0.01D, hot, cold, s.lightCoords);
                    shell(
                            pose,
                            buffer,
                            s.jetLength * 1.5D,
                            0.0625D * 1.5D,
                            hot,
                            cold,
                            s.lightCoords);
                });
    }

    private void layer(
            SubmitNodeCollector col,
            PoseStack ps,
            RenderType type,
            int light,
            int color,
            float uOff,
            float vOff) {
        col.submitCustomGeometry(
                ps,
                type,
                (pose, buffer) ->
                        model.renderPart(pose, buffer, light, color, PLASMA, 1F, 1F, uOff, vOff));
    }

    private void part(SubmitNodeCollector col, PoseStack ps, int light, int name) {
        col.submitCustomGeometry(
                ps, baseType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public final double[] striker = new double[6];
        public final double[] jet = new double[4];
        public final ItemStackRenderState icon = new ItemStackRenderState();
        public float yaw;
        public boolean connected;
        public float ring;
        public long plasmaEnergy;
        public float r;
        public float g;
        public float b;
        public long timeMs;
        public boolean jetFiring;
        public double jetLength;
        public boolean iconBlockItem;
        public FramedItem.Arm iconArm = FramedItem.Arm.SPRITE;
        public double ticks;
        public boolean itemsOnly;
        public @Nullable RenderType beamType;
        public float beamOffset;
    }
}
