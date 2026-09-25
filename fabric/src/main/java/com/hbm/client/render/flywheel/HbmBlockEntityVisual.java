// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.joml.FrustumIntersection;

public abstract class HbmBlockEntityVisual<T extends BlockEntity>
        extends AbstractBlockEntityVisual<T> {
    private final float[] visible = new float[6];
    private boolean visibleRead;

    protected HbmBlockEntityVisual(VisualizationContext context, T blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
    }

    public static boolean hasVisual(BlockEntity blockEntity) {
        Level level = blockEntity.getLevel();
        return VisualizationManager.supportsVisualization(level)
                && level.getBlockEntity(blockEntity.getBlockPos()) == blockEntity;
    }

    protected AABB visibleBounds() {
        return getRenderBoundingBox();
    }

    protected final void refreshVisibleBounds() {
        visibleRead = false;
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        if (!visibleRead) {
            AABB bounds = visibleBounds();
            var origin = renderOrigin();
            visible[0] = (float) (bounds.minX - origin.getX());
            visible[1] = (float) (bounds.minY - origin.getY());
            visible[2] = (float) (bounds.minZ - origin.getZ());
            visible[3] = (float) (bounds.maxX - origin.getX());
            visible[4] = (float) (bounds.maxY - origin.getY());
            visible[5] = (float) (bounds.maxZ - origin.getZ());
            visibleRead = true;
        }
        return frustum.testAab(
                visible[0], visible[1], visible[2], visible[3], visible[4], visible[5]);
    }

    @Override
    public void updateLight(float partialTick) {}
}
