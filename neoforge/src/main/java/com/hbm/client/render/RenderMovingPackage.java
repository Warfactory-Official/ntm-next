// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.item.EntityMovingPackage;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class RenderMovingPackage
        extends EntityRenderer<EntityMovingPackage, RenderMovingPackage.State>
        implements ConcurrentRenderStateExtraction {

    private static final float SCALE = 2F;
    private static final float DROP = 0.0125F;

    private final ItemModelResolver itemModelResolver;

    public RenderMovingPackage(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityMovingPackage entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        FramedItem.resolve(
                itemModelResolver,
                state.item,
                new ItemStack(ModBlocks.CRATE.get()),
                entity.level(),
                null,
                0);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (state.item.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(0F, -DROP, 0F);
        poseStack.scale(SCALE, SCALE, SCALE);
        FramedItem.framedBlockPrefix(poseStack);
        state.item.submit(
                poseStack,
                collector,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
    }
}
