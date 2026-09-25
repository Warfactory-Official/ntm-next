// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.render.flywheel.HbmBlockEntityVisual;
import com.hbm.client.render.flywheel.WorldItem;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.BlockEntityFoundryCastingBase;
import com.hbm.tileentity.machine.IRenderFoundry;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderFoundry
        implements BlockEntityRenderer<BlockEntityFoundryCastingBase, RenderFoundry.State>,
                ConcurrentRenderStateExtraction {
    static final RenderPipeline SHEEN_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_foundry_sheen")
                            .withVertexShader("core/entity")
                            .withFragmentShader(ParticleRenderTypes.ENTITY_FADE)
                            .withShaderDefine("EMISSIVE")
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false))
                            .withCull(false));

    private final RenderType fillType;
    private final RenderType sheenType;
    private final ItemModelResolver itemModelResolver;

    public RenderFoundry(BlockEntityRendererProvider.Context context) {

        this.fillType = FlatCutout.of(ResourceManager.foundry_stream_tex);
        this.sheenType =
                RenderType.create(
                        "ntm_foundry_sheen",
                        RenderSetup.builder(SHEEN_PIPELINE)
                                .withTexture("Sampler0", ResourceManager.foundry_stream_tex)
                                .sortOnUpload()
                                .createRenderSetup());
        this.itemModelResolver = context.itemModelResolver();
    }

    private static void fillQuad(
            PoseStack.Pose pose,
            VertexConsumer buf,
            int argb,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            float y) {
        vertex(pose, buf, argb, minX, y, minZ, minZ, maxX);
        vertex(pose, buf, argb, minX, y, maxZ, maxZ, maxX);
        vertex(pose, buf, argb, maxX, y, maxZ, maxZ, minX);
        vertex(pose, buf, argb, maxX, y, minZ, minZ, minX);
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer buf,
            int argb,
            float x,
            float y,
            float z,
            float u,
            float v) {
        Vertices.emit(buf, pose, x, y, z, argb, u, v, LightCoordsUtil.FULL_BRIGHT, 0F, 1F, 0F);
    }

    private static void blockVertex(
            PoseStack.Pose pose,
            VertexConsumer buf,
            int light,
            float x,
            float y,
            float z,
            float u,
            float v) {
        Vertices.emit(buf, pose, x, y, z, -1, u, v, light, 0F, 1F, 0F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            BlockEntityFoundryCastingBase be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        IRenderFoundry foundry = (IRenderFoundry) be;
        state.moldHeight = foundry.moldHeight();
        state.outHeight = foundry.outHeight();

        ItemStack mold = be.inventory.get(0);
        ItemStack out = be.inventory.get(1);

        BlockItem block = out.getItem() instanceof BlockItem blockItem ? blockItem : null;
        if (block != null) out = ItemStack.EMPTY;
        state.blockSprite = null;
        if (HbmBlockEntityVisual.hasVisual(be)) {
            state.shouldRender = false;
            if (WorldItem.draws(mold, ItemDisplayContext.NONE)) mold = ItemStack.EMPTY;
            if (WorldItem.draws(out, ItemDisplayContext.NONE)) out = ItemStack.EMPTY;
        } else {
            state.shouldRender = foundry.shouldRender();
            if (state.shouldRender) {
                state.level = foundry.getMoltenLevel();
                state.color = foundry.getMat().moltenColor;
            }
            state.minX = foundry.minX();
            state.maxX = foundry.maxX();
            state.minZ = foundry.minZ();
            state.maxZ = foundry.maxZ();
            if (block != null) {
                state.blockSprite =
                        Minecraft.getInstance()
                                .getModelManager()
                                .getBlockStateModelSet()
                                .getParticleMaterial(block.getBlock().defaultBlockState())
                                .sprite();
            }
        }
        itemModelResolver.updateForTopItem(
                state.mold, mold, ItemDisplayContext.NONE, be.getLevel(), null, 0);
        itemModelResolver.updateForTopItem(
                state.out, out, ItemDisplayContext.NONE, be.getLevel(), null, 0);
    }

    public static void flatItemPose(PoseStack ps, double height) {
        ps.translate(0.5, height, 0.5);
        ps.mulPose(Axis.XP.rotationDegrees(-90));
        ps.scale(0.75F, 0.75F, 0.75F);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (!s.mold.isEmpty()) drawItemFlat(s.mold, ps, col, s.moldHeight, s.lightCoords);
        if (!s.out.isEmpty()) drawItemFlat(s.out, ps, col, s.outHeight, s.lightCoords);

        if (s.blockSprite != null) {

            final TextureAtlasSprite sprite = s.blockSprite;
            final float minX = (float) s.minX, maxX = (float) s.maxX;
            final float minZ = (float) s.minZ, maxZ = (float) s.maxZ;
            final float h = (float) s.outHeight;
            final int light = s.lightCoords;

            col.submitCustomGeometry(
                    ps,
                    RenderTypes.entityCutoutCull(sprite.atlasLocation()),
                    (pose, buf) -> {
                        blockVertex(
                                pose, buf, light, minX, h, minZ, sprite.getU0(), sprite.getV1());
                        blockVertex(
                                pose, buf, light, minX, h, maxZ, sprite.getU1(), sprite.getV1());
                        blockVertex(
                                pose, buf, light, maxX, h, maxZ, sprite.getU1(), sprite.getV0());
                        blockVertex(
                                pose, buf, light, maxX, h, minZ, sprite.getU0(), sprite.getV0());
                    });
        }

        if (s.shouldRender) {
            final float minX = (float) s.minX, maxX = (float) s.maxX;
            final float minZ = (float) s.minZ, maxZ = (float) s.maxZ;
            final float y = (float) s.level;
            final int tint = ARGB.opaque(s.color);

            col.submitCustomGeometry(
                    ps,
                    fillType,
                    (pose, buf) -> fillQuad(pose, buf, tint, minX, maxX, minZ, maxZ, y));
            col.submitCustomGeometry(
                    ps,
                    sheenType,
                    (pose, buf) -> fillQuad(pose, buf, 0x4CFFFFFF, minX, maxX, minZ, maxZ, y));
        }
    }

    private void drawItemFlat(
            ItemStackRenderState item,
            PoseStack ps,
            SubmitNodeCollector col,
            double height,
            int light) {
        ps.pushPose();
        flatItemPose(ps, height);
        item.submit(ps, col, light, OverlayTexture.NO_OVERLAY, 0);
        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public final ItemStackRenderState mold = new ItemStackRenderState();
        public final ItemStackRenderState out = new ItemStackRenderState();
        public boolean shouldRender;
        public double level;
        public int color;
        public double minX, maxX, minZ, maxZ;
        public double moldHeight, outHeight;
        public @Nullable TextureAtlasSprite blockSprite;
    }
}
