// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.generic.BlockSnowglobe.SnowglobeType;
import com.hbm.client.render.RenderSnowglobe;
import com.hbm.tileentity.BlockEntitySnowglobe;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public final class SnowglobeItemVisual extends HbmItemVisual {
    private final SnowglobeType type;
    private final List<TransformedInstance> parts = new ArrayList<>(3);
    private final WorldText label;
    private final int lineHeight = Minecraft.getInstance().font.lineHeight;
    private final Matrix4f globe = new Matrix4f();
    private final Matrix4f labelBase = new Matrix4f();
    private final WorldText.Posing labelPosing =
            (width, out) -> RenderSnowglobe.labelWidth(out.set(labelBase), width, lineHeight);

    public SnowglobeItemVisual(VisualizationContext ctx, SnowglobeType type) {
        super(ctx);
        this.type = type;
        parts.add(transformed(ItemMaterials.item(SnowglobeVisual.SOCKET_PART.model())));
        parts.add(transformed(ItemMaterials.item(SnowglobeVisual.GLASS_PART.model())));
        @Nullable MeshPart feature = SnowglobeVisual.FEATURE_PARTS[type.ordinal()];
        if (feature != null) parts.add(transformed(ItemMaterials.item(feature.model())));
        label = new WorldText(instancers, WorldText.Style.NORMAL, true);
        label.set(Component.literal(type.label), 0F, 0F, RenderSnowglobe.LABEL_COLOR);
    }

    @Override
    public boolean update(ItemStack stack) {
        return BlockEntitySnowglobe.typeOf(stack) == type;
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        globe.set(pose).scale(RenderSnowglobe.SCALE);
        for (TransformedInstance part : parts) {
            write(part, globe, -1, light);
        }
        label.setVisible(true);
        RenderSnowglobe.labelBase(labelBase.set(globe));
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
