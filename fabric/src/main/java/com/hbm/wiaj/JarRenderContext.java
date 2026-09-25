// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;

public record JarRenderContext(WorldInAJar world, PoseStack pose, SubmitNodeCollector collector) {
    public ItemModelResolver itemModelResolver() {
        return Minecraft.getInstance().getItemModelResolver();
    }
}
