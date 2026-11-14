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

package com.hbm.client;

import com.hbm.client.gui.ToolAbilityScreen;
import com.hbm.config.HudConfig;
import com.hbm.handler.ability.ToolAreaAbility;
import com.hbm.handler.ability.ToolPreset;
import com.hbm.items.tool.ToolAbilityItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ToolAbilityHudRenderer {

    private static final int SIZE = 16;

    private ToolAbilityHudRenderer() {}

    private static int textureU(ToolAreaAbility area) {
        if (area == ToolAreaAbility.VEIN_MINER) return 0;
        if (area == ToolAreaAbility.AOE) return 16;
        if (area == ToolAreaAbility.FLAT_AOE) return 32;
        if (area == ToolAreaAbility.EXPLOSION) return 48;
        return -1;
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ToolAbilityItem tool)) return;

        ToolPreset preset = tool.activePreset(held);
        int u = textureU(preset.area());
        if (u == -1) return;

        int x = graphics.guiWidth() / 2 - SIZE - 8 + HudConfig.toolIndicatorX;
        int y = graphics.guiHeight() / 2 + 8 + HudConfig.toolIndicatorY;
        graphics.blit(
                RenderPipelines.CROSSHAIR,
                ToolAbilityScreen.TEXTURE,
                x,
                y,
                u,
                138,
                SIZE,
                SIZE,
                256,
                256);
    }
}
