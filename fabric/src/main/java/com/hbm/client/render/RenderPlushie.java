// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.generic.BlockPlushie.PlushieType;
import com.hbm.blocks.generic.BlockPlushie;
import com.hbm.client.render.flywheel.HbmBlockEntityVisual;
import com.hbm.client.render.flywheel.WorldItem;
import com.hbm.items.ModItems;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.HorsePronter;
import com.hbm.tileentity.BlockEntityPlushie;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.Minecraft;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderPlushie
        implements BlockEntityRenderer<BlockEntityPlushie, RenderPlushie.State>,
                ConcurrentRenderStateExtraction {
    private static final int HUNDUN_PART = ResourceManager.hundun.partId("goober_posed");
    private static final int DERG_PART = ResourceManager.derg.partId("Derg");
    private static final int DERG_BLEP = ResourceManager.derg.partId("Blep");
    private static final int DERG_COLON_THREE = ResourceManager.derg.partId("ColonThree");
    private static final int NO9_HELMET = ResourceManager.armor_no9.partId("Helmet");
    private static final int NO9_INSIGNIA = ResourceManager.armor_no9.partId("Insignia");

    private static final double[][] NUMBER_NINE_POSE = numberNinePose();
    private static volatile @Nullable ItemStack cigaretteStack;

    private final ItemModelResolver itemModelResolver;

    public RenderPlushie(BlockEntityRendererProvider.Context context) {
        itemModelResolver = context.itemModelResolver();
    }

    private RenderPlushie(ItemModelResolver itemModelResolver) {
        this.itemModelResolver = itemModelResolver;
    }

    public static RenderPlushie forItem() {
        return new RenderPlushie(Minecraft.getInstance().getItemModelResolver());
    }

    public static ItemStack cigarette() {
        ItemStack stack = cigaretteStack;
        if (stack == null) cigaretteStack = stack = new ItemStack(ModItems.CIGARETTE);
        return stack;
    }

    public static void rootPose(
            PoseStack poses, PlushieType type, int rotation, boolean squishing, double squish) {
        poses.translate(.5, 0, .5);
        poses.mulPose(Axis.YN.rotationDegrees(22.5F * rotation + 90F));
        if (squishing) poses.scale(1F, (float) (1D + (-Math.sin(squish) * squish) * .025D), 1F);
        switch (type) {
            case YOMI -> poses.scale(.5F, .5F, .5F);
            case NUMBERNINE -> poses.scale(.75F, .75F, .75F);
            default -> {}
        }
    }

    private static double[][] numberNinePose() {
        double r = 45D;
        double[][] pose = new double[8][];
        for (int id = 0; id < pose.length; id++) pose[id] = new double[3];
        pose[HorsePronter.id_body] = new double[] {0, -r, 0};
        pose[HorsePronter.id_tail] = new double[] {0, 60, 90};
        pose[HorsePronter.id_lbl] = new double[] {0, -75 + r, 35};
        pose[HorsePronter.id_rbl] = new double[] {0, -75 + r, -35};
        pose[HorsePronter.id_lfl] = new double[] {0, r - 25, 5};
        pose[HorsePronter.id_rfl] = new double[] {0, r - 25, -5};
        pose[HorsePronter.id_head] = new double[] {0, r + 15, 0};
        return pose;
    }

    public static void cigarettePose(PoseStack poses) {
        numberNineFrame(poses);
        poses.mulPose(Axis.XP.rotationDegrees(15F));
        cigaretteOffset(poses);
    }

    private static void numberNineFrame(PoseStack poses) {
        poses.mulPose(Axis.YP.rotationDegrees(90F));
        poses.mulPose(Axis.XN.rotationDegrees(15F));
        poses.translate(0, -.25, .75);
    }

    private static void cigaretteOffset(PoseStack poses) {
        poses.translate(-.06, 1.13, -.42);
        poses.scale(.25F, .25F, .25F);
        poses.mulPose(Axis.YN.rotationDegrees(90F));
        poses.mulPose(Axis.ZN.rotationDegrees(60F));
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
    public void extractRenderState(
            BlockEntityPlushie be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.visualized = HbmBlockEntityVisual.hasVisual(be);
        state.type = be.type;
        state.rotation = be.getBlockState().getValue(BlockPlushie.ROTATION);
        long timer = be.squishTimer();
        state.squishing = timer > 0;
        state.squish = timer - partialTicks;
        if (state.type == PlushieType.NUMBERNINE
                && !(state.visualized && WorldItem.draws(cigarette(), ItemDisplayContext.NONE))) {
            resolveCigarette(state.cigarette, be.getLevel());
        } else {
            state.cigarette.clear();
        }
    }

    @Override
    public void submit(
            State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        poses.pushPose();
        rootPose(poses, state.type, state.rotation, state.squishing, state.squish);
        if (!state.visualized) {
            renderPlushie(
                    state.type,
                    state.squishing,
                    poses,
                    collector,
                    state.lightCoords,
                    state.cigarette);
        } else if (!state.cigarette.isEmpty()) {
            cigarettePose(poses);
            state.cigarette.submit(
                    poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        poses.popPose();
    }

    public void renderPlushie(
            PlushieType type,
            boolean squish,
            PoseStack poses,
            SubmitNodeCollector collector,
            int light,
            @Nullable ItemStackRenderState cigarette) {
        visitParts(
                type,
                poses,
                (part, mesh, group, face, ps) -> {
                    if (face.shows(squish))
                        collector.submitCustomGeometry(
                                ps,
                                part.type,
                                (pose, buffer) -> mesh.renderPart(pose, buffer, light, -1, group));
                });
        if (type != PlushieType.NUMBERNINE || cigarette == null) return;
        poses.pushPose();
        cigarettePose(poses);
        cigarette.submit(poses, collector, light, OverlayTexture.NO_OVERLAY, 0);
        poses.popPose();
    }

    public static void visitParts(PlushieType type, PoseStack poses, PartVisitor visitor) {
        switch (type) {
            case NONE -> {}
            case YOMI -> {
                for (int group = 0; group < ResourceManager.yomi.groups.length; group++) {
                    visitor.accept(Part.YOMI, ResourceManager.yomi, group, Face.ALWAYS, poses);
                }
            }
            case NUMBERNINE -> {
                poses.pushPose();
                numberNineFrame(poses);
                HorsePronter.visit(
                        poses,
                        NUMBER_NINE_POSE,
                        false,
                        false,
                        false,
                        (ps, group) ->
                                visitor.accept(
                                        Part.NUMBERNINE,
                                        ResourceManager.horse,
                                        group,
                                        Face.ALWAYS,
                                        ps));
                poses.mulPose(Axis.XP.rotationDegrees(15F));
                poses.translate(0, 1, -.6875);
                poses.scale(.0703125F, .0703125F, .0703125F);
                poses.mulPose(Axis.XP.rotationDegrees(180F));
                visitor.accept(Part.NO9, ResourceManager.armor_no9, NO9_HELMET, Face.ALWAYS, poses);
                visitor.accept(
                        Part.NO9_TRIM, ResourceManager.armor_no9, NO9_INSIGNIA, Face.ALWAYS, poses);
                poses.popPose();
            }
            case HUNDUN ->
                    visitor.accept(
                            Part.HUNDUN, ResourceManager.hundun, HUNDUN_PART, Face.ALWAYS, poses);
            case DERG -> {
                visitor.accept(Part.DERG, ResourceManager.derg, DERG_PART, Face.ALWAYS, poses);
                visitor.accept(Part.DERG, ResourceManager.derg, DERG_BLEP, Face.SQUISHED, poses);
                visitor.accept(
                        Part.DERG, ResourceManager.derg, DERG_COLON_THREE, Face.RELAXED, poses);
            }
        }
    }

    void resolveCigarette(ItemStackRenderState state, @Nullable Level level) {
        itemModelResolver.updateForTopItem(
                state, cigarette(), ItemDisplayContext.NONE, level, null, 0);
    }

    public enum Part {
        YOMI(ResourceManager.yomi_tex, true),
        NUMBERNINE(ResourceManager.numbernine_tex, false),
        HUNDUN(ResourceManager.hundun_tex, true),
        DERG(ResourceManager.derg_tex, true),
        NO9(ResourceManager.no9_tex, false),
        NO9_TRIM(ResourceManager.no9_insignia_tex, false);

        public final Identifier texture;
        public final boolean cull;
        private final RenderType type;

        Part(Identifier texture, boolean cull) {
            this.texture = texture;
            this.cull = cull;
            type =
                    cull
                            ? RenderTypes.entityCutoutCull(texture)
                            : WorldRenderPipeline.oneSidedCutout(texture);
        }
    }

    public enum Face {
        ALWAYS,
        SQUISHED,
        RELAXED;

        public boolean shows(boolean squish) {
            return this == ALWAYS || (this == SQUISHED) == squish;
        }
    }

    @FunctionalInterface
    public interface PartVisitor {
        void accept(Part part, HFRWavefrontObject mesh, int group, Face face, PoseStack poses);
    }

    public static final class State extends BlockEntityRenderState {
        public final ItemStackRenderState cigarette = new ItemStackRenderState();
        public PlushieType type = PlushieType.NONE;
        public int rotation;
        public boolean squishing;
        public double squish;
        public boolean visualized;
    }
}
