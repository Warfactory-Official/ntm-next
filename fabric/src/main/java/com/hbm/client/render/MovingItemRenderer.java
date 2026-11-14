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

package com.hbm.client.render;

import com.hbm.entity.item.MovingItemEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.Random;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionfc;

public class MovingItemRenderer extends EntityRenderer<MovingItemEntity, MovingItemRenderer.State>
        implements ConcurrentRenderStateExtraction {

    private final ItemModelResolver itemModelResolver;

    public MovingItemRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MovingItemEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.lift = (float) (new Random(entity.getId()).nextDouble() * 0.0625D);
        ItemStack stack = entity.getItemStack();
        state.count = stack.getCount();
        state.arm =
                FramedItem.resolve(itemModelResolver, state.item, stack, entity.level(), null, 0);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (state.item.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(0F, state.lift, 0F);

        if (state.arm == FramedItem.Arm.BLOCK) {
            FramedItem.framedBlockPrefix(poseStack);
            FramedItem.framedBlockCopies(
                    poseStack,
                    state.item,
                    collector,
                    state.lightCoords,
                    state.outlineColor,
                    FramedItem.miniBlockCount(state.count));
        } else {
            poseStack.rotate((Quaternionfc) Axis.XP.rotationDegrees(90F));
            poseStack.translate(0F, -0.1875F, 0F);
            if (state.arm.mesh()) {
                FramedItem.framedMeshPrefix(poseStack, state.arm);
                state.item.submit(
                        poseStack,
                        collector,
                        state.lightCoords,
                        OverlayTexture.NO_OVERLAY,
                        state.outlineColor);
            } else {
                FramedItem.framedSpritePrefix(poseStack);
                FramedItem.framedSpriteAnchor(poseStack);
                FramedItem.framedSpriteCopies(
                        poseStack,
                        state.item,
                        collector,
                        state.lightCoords,
                        state.outlineColor,
                        FramedItem.miniItemCount(state.count));
            }
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        float lift;
        int count;
        FramedItem.Arm arm = FramedItem.Arm.SPRITE;
    }
}
