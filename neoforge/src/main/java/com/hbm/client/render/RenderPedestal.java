// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockBobble;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.items.tool.ItemBoltgun;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.tileentity.BlockEntityPedestal;
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
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderPedestal
        implements BlockEntityRenderer<BlockEntityPedestal, RenderPedestal.State>,
                ConcurrentRenderStateExtraction {

    @Override
    public AABB getRenderBoundingBox(BlockEntityPedestal be) {
        return new AABB(be.getBlockPos()).move(0.0D, 1.0D, 0.0D).inflate(1.0D);
    }

    private final ItemModelResolver itemModelResolver;

    public RenderPedestal(BlockEntityRendererProvider.Context context) {
        itemModelResolver = context.itemModelResolver();
    }

    private static void submitMesh(
            State state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.translate(0.0, 0.125, 0.0);
        poseStack.mulPose(Axis.YN.rotationDegrees(state.playerYaw + 180.0F));
        poseStack.translate(0.0, Math.sin(state.playerAge * 0.1F) * 0.0625F, 0.0);
        poseStack.translate(0.0, state.arm.groundLift(), 0.0);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
    }

    private static void submitBlock(
            State state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.translate(0.0, Math.sin(state.playerAge * 0.1F) * 0.0625F + 0.0625F, 0.0);
        FramedItem.framedBlockPrefix(poseStack);
        FramedItem.framedBlockCopies(
                poseStack, state.item, collector, state.lightCoords, 0, state.copies);
    }

    private static void submitFlat(
            State state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.translate(0.0, 0.125, 0.0);
        poseStack.mulPose(Axis.YN.rotationDegrees(state.playerYaw + 180.0F));
        poseStack.translate(0.0, Math.sin(state.playerAge * 0.1F) * 0.0625F, 0.0);
        FramedItem.framedSpritePrefix(poseStack);
        FramedItem.framedSpriteCopies(
                poseStack, state.item, collector, state.lightCoords, 0, state.sprites);
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
            BlockEntityPedestal pedestal,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                pedestal, state, partialTicks, cameraPosition, breakProgress);
        if (pedestal.item.isEmpty()) {
            state.item = null;
            return;
        }

        state.copies = FramedItem.miniBlockCount(pedestal.item.getCount());
        state.sprites = FramedItem.miniItemCount(pedestal.item.getCount());
        if (state.item == null) state.item = new ItemStackRenderState();
        state.arm =
                FramedItem.resolve(
                        itemModelResolver, state.item, pedestal.item, pedestal.getLevel(), null, 0);
        state.playerAge = Minecraft.getInstance().player.tickCount + partialTicks;

        state.playerYaw = Minecraft.getInstance().player.getViewYRot(partialTicks);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (state.item == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 1.0, 0.5);
        switch (state.arm) {
            case MESH, STILL_MESH -> submitMesh(state, poseStack, collector);
            case BLOCK -> submitBlock(state, poseStack, collector);
            case SPRITE -> submitFlat(state, poseStack, collector);
        }
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public @Nullable ItemStackRenderState item;
        public FramedItem.Arm arm = FramedItem.Arm.SPRITE;
        public int copies;
        public int sprites;
        public float playerAge;
        public float playerYaw;
    }
}
