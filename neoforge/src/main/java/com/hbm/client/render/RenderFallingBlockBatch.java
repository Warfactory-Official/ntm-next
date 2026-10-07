// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.render.flywheel.FallingBlockNTVisual;
import com.hbm.entity.item.EntityFallingBlockBatch;
import com.hbm.entity.item.EntityFallingBlockNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import java.util.Arrays;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;

public final class RenderFallingBlockBatch
        extends EntityRenderer<EntityFallingBlockBatch, RenderFallingBlockBatch.State>
        implements ConcurrentRenderStateExtraction {
    private final RenderFallingBlockNT child;

    public RenderFallingBlockBatch(EntityRendererProvider.Context context) {
        super(context);
        child = new RenderFallingBlockNT(context);
    }

    @Override
    protected boolean affectedByCulling(EntityFallingBlockBatch entity) {
        return false;
    }

    @Override
    public boolean shouldRender(
            EntityFallingBlockBatch entity, Frustum culler, double camX, double camY, double camZ) {
        for (EntityFallingBlockNT proxy : entity.clientFallers().values()) {
            if (child.shouldRender(proxy, culler, camX, camY, camZ)) return true;
        }
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityFallingBlockBatch entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.ensure(entity.clientFallers().size());
        state.count = 0;
        boolean flywheel = VisualizationManager.get(entity.level()) != null;
        double batchX = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double batchY = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double batchZ = Mth.lerp(partialTick, entity.zOld, entity.getZ());
        for (EntityFallingBlockNT proxy : entity.clientFallers().values()) {
            if (flywheel && FallingBlockNTVisual.visualized(proxy)) continue;
            int i = state.count++;
            child.extractRenderState(proxy, state.children[i], partialTick);
            state.offsetX[i] = (float) (Mth.lerp(partialTick, proxy.xOld, proxy.getX()) - batchX);
            state.offsetY[i] = (float) (Mth.lerp(partialTick, proxy.yOld, proxy.getY()) - batchY);
            state.offsetZ[i] = (float) (Mth.lerp(partialTick, proxy.zOld, proxy.getZ()) - batchZ);
        }
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < state.count; i++) {
            pose.pushPose();
            pose.translate(state.offsetX[i], state.offsetY[i], state.offsetZ[i]);
            child.submit(state.children[i], pose, collector, camera);
            pose.popPose();
        }
    }

    public static final class State extends EntityRenderState {
        private FallingBlockRenderState[] children = new FallingBlockRenderState[0];
        private float[] offsetX = new float[0];
        private float[] offsetY = new float[0];
        private float[] offsetZ = new float[0];
        private int count;

        public int fallerCount() {
            return count;
        }

        private void ensure(int capacity) {
            int old = children.length;
            if (capacity <= old) return;
            int next = Math.max(capacity, Math.max(4, old * 2));
            children = Arrays.copyOf(children, next);
            offsetX = Arrays.copyOf(offsetX, next);
            offsetY = Arrays.copyOf(offsetY, next);
            offsetZ = Arrays.copyOf(offsetZ, next);
            for (int i = old; i < next; i++) children[i] = new FallingBlockRenderState();
        }
    }
}
