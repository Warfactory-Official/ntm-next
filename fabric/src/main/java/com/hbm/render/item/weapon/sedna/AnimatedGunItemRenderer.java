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

package com.hbm.render.item.weapon.sedna;

import com.hbm.render.item.weapon.sedna.WeaponItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;

public abstract class AnimatedGunItemRenderer extends WeaponItemRenderer {

    private final GunRenderDefinition body;

    protected AnimatedGunItemRenderer(GunRenderDefinition body) {
        this.body = body;
    }

    @Override
    public final void setupFirstPerson(GunRenderState frame, PoseStack pose) {
        body.setupFirstPerson(frame, pose);
    }

    @Override
    public final void renderFirstPerson(
            GunRenderState frame, PoseStack pose, SubmitNodeCollector collector, int light) {
        body.renderFirstPerson(frame, pose, collector, light);
    }

    @Override
    public final void restBody(ItemStack stack, RestBody target) {
        body.restBody(stack, target);
    }

    @Override
    public final GunAnimationChannels rig() {
        return body.rig();
    }
}
