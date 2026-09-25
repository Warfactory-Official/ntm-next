// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.generic.BlockBobble.BobbleType;
import com.hbm.client.render.RenderBobble;
import com.hbm.tileentity.BlockEntityBobble;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public final class BobbleItemVisual extends HbmItemVisual {
    private final BobbleType type;
    private final BobbleDraws draws;
    private final List<TransformedInstance> parts = new ArrayList<>();
    private final @Nullable TransformedInstance prop;
    private final WorldText label;
    private final Matrix4f world = new Matrix4f();
    private final Matrix4f labelBase = new Matrix4f();
    private final WorldText.Posing labelPosing =
            (width, out) -> RenderBobble.labelWidth(out.set(labelBase), width);

    public BobbleItemVisual(VisualizationContext ctx, BobbleType type) {
        super(ctx);
        this.type = type;
        draws = new BobbleDraws(type, new PoseStack());
        for (BobbleDraws.Draw draw : draws.draws)
            parts.add(transformed(ItemMaterials.item(draw.part().model())));
        ItemStack held = RenderBobble.prop(type);
        prop = held.isEmpty() ? null : transformed(PropItem.model(held, ItemDisplayContext.NONE));
        label = new WorldText(instancers, WorldText.Style.NORMAL, true);
        label.set(Component.literal(type.label), 0F, 0F, RenderBobble.labelColor(type));
    }

    @Override
    public boolean update(ItemStack stack) {
        return BlockEntityBobble.typeOf(stack) == type;
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        long time = GameTime.now();
        int shine = RenderBobble.shine(time);
        for (int i = 0; i < parts.size(); i++) {
            BobbleDraws.Draw draw = draws.draws.get(i);
            TransformedInstance part = parts.get(i);
            Matrix4f pivot = draw.pivot();
            write(
                    part,
                    pivot == null
                            ? world.set(pose).mul(draw.pose())
                            : BobbleDraws.bob(world, pose, pivot, time, draw.pose()),
                    draw.shine() ? shine : draw.color(),
                    draw.light() != 0 ? draw.light() : light);
        }
        if (prop != null) {
            Matrix4f pivot = draws.propPivot;
            write(
                    prop,
                    pivot == null
                            ? world.set(pose).mul(draws.propLocal)
                            : BobbleDraws.bob(world, pose, pivot, time, draws.propLocal),
                    -1,
                    light);
        }
        label.setVisible(true);
        labelBase.set(pose).mul(draws.labelLocal);
        label.write(labelPosing);
    }

    @Override
    public void hide() {
        super.hide();
        label.setVisible(false);
    }

    @Override
    public void delete() {
        super.delete();
        label.delete();
    }
}
