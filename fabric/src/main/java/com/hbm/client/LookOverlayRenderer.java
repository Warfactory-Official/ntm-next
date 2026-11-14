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

import com.hbm.capability.NtmContracts;
import com.hbm.config.HudConfig;
import com.hbm.interfaces.EntityLookOverlayProvider;
import com.hbm.interfaces.ItemLookOverlayProvider;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

public final class LookOverlayRenderer {
    private static final LookOverlay CARD = new LookOverlay();

    private LookOverlayRenderer() {}

    public static @Nullable LookOverlay compose(Minecraft mc) {
        if (!HudConfig.doddRbmkDiagnostic || mc.level == null || mc.player == null) return null;
        HitResult hit = mc.hitResult;
        if (hit == null) return null;

        Level level = mc.level;
        LookOverlay card = CARD;
        card.reset();
        if (hit.getType() == HitResult.Type.ENTITY) {
            if (!(((EntityHitResult) hit).getEntity() instanceof EntityLookOverlayProvider entity))
                return null;
            entity.buildLookOverlay(level, card);
        } else if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            card.hit(pos);
            ItemStack held = mc.player.getMainHandItem();
            if (held.getItem() instanceof ItemLookOverlayProvider item) {
                item.buildLookOverlay(level, pos, held, mc.player, card);
            } else {
                NtmContracts.Target<LookOverlayProvider> source =
                        NtmContracts.LOOK_OVERLAY.target(level, pos);
                if (source == null) return null;
                source.handler().buildLookOverlay(level, source.pos(), card);
            }
        } else {
            return null;
        }
        return card.isEmpty() ? null : card;
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        LookOverlay card = compose(mc);
        if (card == null) return;

        Font font = mc.font;
        int x = mc.getWindow().getGuiScaledWidth() / 2 + 8;
        int y = mc.getWindow().getGuiScaledHeight() / 2;

        List<LookOverlay.Heading> headings = card.headings();
        for (int i = 0; i < headings.size(); i++) {
            LookOverlay.Heading heading = headings.get(i);
            graphics.text(
                    font,
                    heading.text(),
                    x + 1,
                    y + heading.offset() + 1,
                    ARGB.opaque(heading.shadowRgb()),
                    false);
            graphics.text(
                    font,
                    heading.text(),
                    x,
                    y + heading.offset(),
                    ARGB.opaque(heading.rgb()),
                    false);
        }
        Component title = card.title();
        if (title != null) {
            graphics.text(
                    font,
                    title,
                    x + 1,
                    y + card.titleOffset() + 1,
                    ARGB.opaque(card.titleShadowRgb()),
                    false);
            graphics.text(
                    font, title, x, y + card.titleOffset(), ARGB.opaque(card.titleRgb()), false);
        }
        boolean shadowed = card.shadowedRows();
        for (int i = 0, rows = card.rowCount(); i < rows; i++) {
            graphics.text(font, card.rowText(i), x, y, ARGB.opaque(card.rowRgb(i)), shadowed);
            y += 10;
        }
    }
}
