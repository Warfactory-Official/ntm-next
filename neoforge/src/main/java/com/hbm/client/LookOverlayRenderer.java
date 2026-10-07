// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.capability.NtmContracts;
import com.hbm.interfaces.IEntityLookOverlay;
import com.hbm.interfaces.IItemLookOverlay;
import com.hbm.interfaces.ILookOverlay.LookInfo;
import com.hbm.interfaces.ILookOverlay;
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

public final class LookOverlayRenderer {

    private LookOverlayRenderer() {}

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        HitResult hit = mc.hitResult;
        if (hit == null) return;

        Level level = mc.level;
        LookInfo info;
        if (hit.getType() == HitResult.Type.ENTITY) {
            if (!(((EntityHitResult) hit).getEntity() instanceof IEntityLookOverlay entity)) return;
            info = new LookInfo();
            entity.buildLookOverlay(level, info);
        } else if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            info = new LookInfo().hitPos(pos);

            ItemStack held = mc.player.getMainHandItem();
            if (held.getItem() instanceof IItemLookOverlay item) {
                item.buildLookOverlay(level, pos, held, mc.player, info);
            } else {
                ILookOverlay overlay = NtmContracts.LOOK_OVERLAY.at(level, pos);
                if (overlay == null) return;
                overlay.buildLookOverlay(level, pos, info);
            }
        } else {
            return;
        }

        if (info.isEmpty()) return;

        Font font = mc.font;
        int x = mc.getWindow().getGuiScaledWidth() / 2 + 8;
        int y = mc.getWindow().getGuiScaledHeight() / 2;

        for (ILookOverlay.Heading heading : info.getHeadings()) {
            graphics.text(
                    font,
                    heading.text(),
                    x + 1,
                    y + heading.offset() + 1,
                    ARGB.opaque(heading.bgColor()),
                    false);
            graphics.text(
                    font,
                    heading.text(),
                    x,
                    y + heading.offset(),
                    ARGB.opaque(heading.color()),
                    false);
        }
        Component title = info.getTitle();
        if (title != null) {
            graphics.text(
                    font,
                    title,
                    x + 1,
                    y + info.getTitleOffset() + 1,
                    ARGB.opaque(info.getBgColor()),
                    false);
            graphics.text(
                    font,
                    title,
                    x,
                    y + info.getTitleOffset(),
                    ARGB.opaque(info.getTitleColor()),
                    false);
        }
        for (int i = 0; i < info.size(); i++) {
            graphics.text(
                    font,
                    info.line(i),
                    x,
                    y + i * 10,
                    ARGB.opaque(info.color(i)),
                    info.shadowedLines());
        }
    }
}
