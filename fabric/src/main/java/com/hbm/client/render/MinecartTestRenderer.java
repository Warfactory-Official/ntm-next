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

import com.hbm.entity.item.MinecartTestEntity;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.renderer.entity.state.MinecartTntRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;

public class MinecartTestRenderer extends TntMinecartRenderer
        implements ConcurrentRenderStateExtraction {
    public static final RenderType TYPE =
            RenderTypes.entityCutoutCull(ResourceManager.bomb_boy_tex);

    public MinecartTestRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void extractRenderState(
            MinecartTNT entity, MinecartTntRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (!((MinecartTestEntity) entity).isFlipped()) return;
        Vec3 front = state.frontPos, back = state.backPos;
        if (state.isNewRender || front == null || back == null) {
            state.yRot += 180F;
            return;
        }
        state.frontPos = new Vec3(back.x, front.y, back.z);
        state.backPos = new Vec3(front.x, back.y, front.z);
    }

    @Override
    protected void submitMinecartContents(
            MinecartTntRenderState state,
            BlockModelRenderState blockModel,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords) {
        poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(-90F));
        poseStack.translate(0.5F, 0.5F, -0.5F);
        float fuse = state.fuseRemainingInTicks;
        if (fuse > -1F && fuse < 10F) {
            float scale = 1F + TntRenderer.getSwellAmount(fuse);
            poseStack.scale(scale, scale, scale);
        }
        int overlay =
                TntRenderer.isLit(fuse)
                        ? OverlayTexture.pack(OverlayTexture.u(1F), 10)
                        : OverlayTexture.NO_OVERLAY;
        collector.submitCustomGeometry(
                poseStack,
                TYPE,
                (pose, buffer) ->
                        ResourceManager.lil_boy.render(pose, buffer, lightCoords, -1, overlay));
    }
}
