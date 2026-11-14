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

package com.hbm.client.render.flywheel;

import com.hbm.client.render.GunB92ItemRenderer;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class GunB92ItemVisual extends HbmItemVisual {
    private final boolean tips;
    private final List<TransformedInstance> body = new ArrayList<>();
    private final List<TransformedInstance> pump = new ArrayList<>();
    private final PoseStack.Pose bodyLocal = new PoseStack.Pose();
    private final PoseStack.Pose pumpLocal = new PoseStack.Pose();
    private final Matrix4f world = new Matrix4f();

    public GunB92ItemVisual(VisualizationContext ctx, GunB92ItemRenderer.Argument argument) {
        super(ctx);
        tips = argument.tips();
        Material material = ItemMaterials.cutout(ResourceManager.b92_tex, true);
        for (int part : GunB92ItemRenderer.BODY_IDS) {
            body.add(transformed(ItemMaterials.part(ResourceManager.b92, part, material)));
        }
        for (int part : GunB92ItemRenderer.PUMP_IDS) {
            pump.add(transformed(ItemMaterials.part(ResourceManager.b92, part, material)));
        }
        pose(argument);
    }

    private void pose(GunB92ItemRenderer.Argument argument) {
        bodyLocal.setIdentity();
        GunB92ItemRenderer.tip(argument, bodyLocal);
        pumpLocal.set(bodyLocal);
        GunB92ItemRenderer.body(bodyLocal);
        GunB92ItemRenderer.pump(argument, pumpLocal);
    }

    @Override
    public boolean update(ItemStack stack) {
        pose(GunB92ItemRenderer.argument(stack, tips));
        return true;
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        write(body, world.set(pose).mul(bodyLocal.pose()), light);
        write(pump, world.set(pose).mul(pumpLocal.pose()), light);
    }

    private static void write(List<TransformedInstance> parts, Matrix4fc world, int light) {
        for (TransformedInstance part : parts) {
            write(part, world, -1, light);
        }
    }
}
