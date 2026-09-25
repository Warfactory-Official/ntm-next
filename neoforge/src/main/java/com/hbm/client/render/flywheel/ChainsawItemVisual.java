// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.ChainsawItemRenderer;
import com.hbm.items.tool.ItemToolAbilityFueled;
import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;

public final class ChainsawItemVisual extends HbmItemVisual {
    private final boolean swings;
    private final boolean running;
    private final TransformedInstance saw;
    private final TransformedInstance[] teeth = new TransformedInstance[ChainsawItemRenderer.TEETH];
    private final PoseStack.Pose base = new PoseStack.Pose();
    private final PoseStack.Pose tooth = new PoseStack.Pose();

    public ChainsawItemVisual(VisualizationContext ctx, boolean swings, boolean running) {
        super(ctx);
        this.swings = swings;
        this.running = running;
        Material material = ItemMaterials.cutout(ResourceManager.chainsaw_tex, true);
        saw =
                transformed(
                        ItemMaterials.part(
                                ResourceManager.chainsaw, ChainsawItemRenderer.SAW, material));
        Model toothModel =
                ItemMaterials.part(ResourceManager.chainsaw, ChainsawItemRenderer.TOOTH, material);
        for (int i = 0; i < teeth.length; i++) teeth[i] = transformed(toothModel);
    }

    @Override
    public boolean update(ItemStack stack) {
        return ((ItemToolAbilityFueled) stack.getItem()).canOperate(stack) == running;
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        base.setIdentity();
        base.pose().set(pose);
        if (swings) ChainsawItemRenderer.swing(base);
        write(saw, base.pose(), -1, light);
        double run = ChainsawItemRenderer.run(running, GameTime.now());
        for (int i = 0; i < teeth.length; i++) {
            tooth.set(base);
            ChainsawItemRenderer.tooth(tooth, ChainsawItemRenderer.forward(i, run));
            write(teeth[i], tooth.pose(), -1, light);
        }
    }
}
