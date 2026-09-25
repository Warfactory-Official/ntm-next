// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.InfoSystem;
import com.hbm.client.WiringReadout;
import com.hbm.items.ModDataComponents;
import com.hbm.items.tool.ItemRebarPlacer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.Shapes;
import org.jspecify.annotations.Nullable;

public final class RebarPlacerPreview {

    private RebarPlacerPreview() {}

    private static @Nullable AABB span() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null
                || !(mc.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        Long corner = null;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof ItemRebarPlacer
                    && (corner = held.get(ModDataComponents.REBAR_CORNER.get())) != null) break;
        }
        if (corner == null) return null;
        return new AABB(BlockPos.of(corner))
                .minmax(new AABB(hit.getBlockPos().relative(hit.getDirection())));
    }

    public static @Nullable AABB extract() {
        AABB span = span();
        return span == null ? null : span.deflate(0.125);
    }

    public static void submit(
            @Nullable AABB box,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (box == null) return;
        poseStack.pushPose();
        poseStack.translate(
                box.minX - camera.pos.x, box.minY - camera.pos.y, box.minZ - camera.pos.z);
        collector.submitShapeOutline(
                poseStack,
                Shapes.box(0, 0, 0, box.getXsize(), box.getYsize(), box.getZsize()),
                RenderTypes.lines(),
                -1,
                1.0F,
                false);
        poseStack.popPose();
    }

    public static void clientTick() {
        AABB span = span();
        LocalPlayer player = Minecraft.getInstance().player;
        if (span == null || player == null) return;
        int left = ItemRebarPlacer.countRebar(player);
        int required = (int) Math.round(span.getXsize() * span.getYsize() * span.getZsize());
        InfoSystem.push(
                new InfoSystem.InfoEntry(
                        Component.literal(left + " / " + required)
                                .withStyle(
                                        required > left
                                                ? ChatFormatting.RED
                                                : ChatFormatting.GREEN),
                        WiringReadout.MILLIS),
                WiringReadout.ID_CABLE);
    }
}
