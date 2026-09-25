// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.generic.BlockPlushie.PlushieType;
import com.hbm.client.render.PlushieItemRenderer;
import com.hbm.client.render.RenderPlushie;
import com.hbm.tileentity.BlockEntityPlushie;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class PlushieItemVisual extends HbmItemVisual {
    private final PlushieType type;
    private final List<TransformedInstance> parts = new ArrayList<>();
    private final List<Matrix4f> locals = new ArrayList<>();
    private final Matrix4f world = new Matrix4f();

    public PlushieItemVisual(VisualizationContext ctx, PlushieType type) {
        super(ctx);
        this.type = type;
        PoseStack poses = new PoseStack();
        PlushieItemRenderer.itemPose(type, poses);
        RenderPlushie.visitParts(
                type,
                poses,
                (part, mesh, group, face, ps) -> {
                    if (!face.shows(false)) return;
                    parts.add(
                            transformed(
                                    ItemMaterials.item(
                                            PlushieVisual.meshPart(part, mesh, group).model())));
                    locals.add(new Matrix4f(ps.last().pose()));
                });
        if (type == PlushieType.NUMBERNINE) {
            parts.add(
                    transformed(
                            PropItem.model(RenderPlushie.cigarette(), ItemDisplayContext.NONE)));
            RenderPlushie.cigarettePose(poses);
            locals.add(new Matrix4f(poses.last().pose()));
        }
    }

    @Override
    public boolean update(ItemStack stack) {
        return BlockEntityPlushie.typeOf(stack) == type;
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        for (int i = 0; i < parts.size(); i++) {
            TransformedInstance part = parts.get(i);
            write(part, world.set(pose).mul(locals.get(i)), -1, light);
        }
    }
}
