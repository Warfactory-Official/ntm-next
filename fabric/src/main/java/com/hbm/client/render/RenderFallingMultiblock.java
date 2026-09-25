// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.item.EntityFallingMultiblock;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderFallingMultiblock
        extends EntityRenderer<EntityFallingMultiblock, RenderFallingMultiblock.State> {

    public RenderFallingMultiblock(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    protected boolean affectedByCulling(EntityFallingMultiblock entity) {
        return false;
    }

    @Override
    public void extractRenderState(
            EntityFallingMultiblock entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.members.clear();
        state.offsets.clear();
        state.carried = null;
        BlockPos origin = entity.originCore();
        for (long member : entity.membersPacked()) {
            BlockPos offset =
                    new BlockPos(
                            EntityFallingMultiblock.offsetXOf(member),
                            EntityFallingMultiblock.offsetYOf(member),
                            EntityFallingMultiblock.offsetZOf(member));
            MovingBlockRenderState moving = new MovingBlockRenderState();
            moving.blockState = EntityFallingMultiblock.stateOf(member);
            moving.randomSeedPos = origin.offset(offset);
            state.offsets.add(offset);
            state.members.add(moving);
        }

        if (entity.level() instanceof ClientLevel clientLevel) {
            BlockPos anchor = entity.blockPosition();
            for (int i = 0; i < state.members.size(); i++) {
                MovingBlockRenderState moving = state.members.get(i);
                BlockPos pos = anchor.offset(state.offsets.get(i));
                moving.blockPos = pos;
                moving.biome = clientLevel.getBiome(pos);
                moving.cardinalLighting = clientLevel.cardinalLighting();
                moving.lightEngine = clientLevel.getLightEngine();
            }

            BlockEntity carried = entity.carriedBlockEntity(anchor);
            if (carried != null) state.carried = extractCarried(carried, partialTicks);
        }
    }

    private <E extends BlockEntity, S extends BlockEntityRenderState> @Nullable S extractCarried(
            E carried, float partialTicks) {
        BlockEntityRenderer<E, S> renderer =
                Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(carried);
        if (renderer == null) return null;
        Vec3 camera = entityRenderDispatcher.camera.position();
        if (!renderer.shouldRender(carried, camera)) return null;
        S carriedState = renderer.createRenderState();
        renderer.extractRenderState(carried, carriedState, partialTicks, camera, null);
        return carriedState;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera) {
        for (int i = 0; i < state.members.size(); i++) {
            MovingBlockRenderState moving = state.members.get(i);
            BlockState blockState = moving.blockState;
            if (blockState.getRenderShape() != RenderShape.MODEL) continue;
            BlockPos offset = state.offsets.get(i);
            poseStack.pushPose();
            poseStack.translate(offset.getX() - 0.5, offset.getY(), offset.getZ() - 0.5);
            submitNodeCollector.submitMovingBlock(poseStack, moving, state.outlineColor);
            poseStack.popPose();
        }
        if (state.carried != null) {
            poseStack.pushPose();
            poseStack.translate(-0.5, 0.0, -0.5);
            Minecraft.getInstance()
                    .getBlockEntityRenderDispatcher()
                    .submit(state.carried, poseStack, submitNodeCollector, camera);
            poseStack.popPose();
        }
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    public static final class State extends EntityRenderState {
        private final List<MovingBlockRenderState> members = new ArrayList<>();
        private final List<BlockPos> offsets = new ArrayList<>();
        public @Nullable BlockEntityRenderState carried;
    }
}
