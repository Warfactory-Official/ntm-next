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

import com.hbm.client.render.CrabModel;
import com.hbm.entity.mob.CyberCrabEntity;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionfc;

public class CyberCrabRenderer
        extends MobRenderer<CyberCrabEntity, LivingEntityRenderState, CrabModel>
        implements ConcurrentRenderStateExtraction {

    public CyberCrabRenderer(EntityRendererProvider.Context context) {
        super(context, new CrabModel(CrabModel.createBodyLayer().bakeRoot()), 1.0F);
        this.shadowStrength = 0F;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        poseStack.translate(0F, 1.5F, 0F);
        poseStack.mulPose((Quaternionfc) Axis.YN.rotationDegrees(90F));
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return ResourceManager.crab_tex;
    }
}
