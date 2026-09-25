// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.tileentity.machine.BlockEntityMachineElectrolyser.PourStream;
import com.hbm.tileentity.machine.BlockEntityMachineElectrolyser;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class ElectrolyserVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineElectrolyser> {
    private static final float MAX_AGE = 20F;
    private static final double LIP_OUT = 5.875D;
    private static final double LIP_Y = 2D;
    private static final float LIP_OFFSET = .625F;
    private static final float LIP_BASE = .625F;
    private static final int POUR_REACH = 6;
    private final List<PourVisual> pours = new ArrayList<>();
    private final Direction facing;
    private int activePours;

    public ElectrolyserVisual(
            VisualizationContext context,
            BlockEntityMachineElectrolyser blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        facing = blockState.getValue(BlockMultiblockCore.FACING);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        long now = level.getGameTime();
        int previousActive = activePours;
        int slot = 0;
        for (PourStream stream : blockEntity.streams) {
            Direction direction = stream.left() ? facing.getOpposite() : facing;
            while (pours.size() <= slot)
                pours.add(new PourVisual(visualizationContext, level, pos));
            updateMovingParts(pours.get(slot++), stream, direction, now, partialTick);
        }
        while (slot < previousActive) {
            PourVisual target = pours.get(slot++);
            target.update(
                    0,
                    facing,
                    0F,
                    MAX_AGE,
                    LIP_BASE,
                    LIP_OFFSET,
                    (float) (.5D + facing.getStepX() * LIP_OUT),
                    (float) LIP_Y,
                    (float) (.5D + facing.getStepZ() * LIP_OUT));
        }
        activePours = blockEntity.streams.size();
    }

    private void updateMovingParts(
            PourVisual target,
            PourStream stream,
            Direction direction,
            long now,
            float partialTick) {
        if (stream == null) {
            target.update(
                    0,
                    direction,
                    0F,
                    MAX_AGE,
                    LIP_BASE,
                    LIP_OFFSET,
                    (float) (.5D + direction.getStepX() * LIP_OUT),
                    (float) LIP_Y,
                    (float) (.5D + direction.getStepZ() * LIP_OUT));
            return;
        }
        float age = (now - stream.birth()) + partialTick;
        target.update(
                stream.color(),
                direction,
                stream.len(),
                age,
                LIP_BASE,
                LIP_OFFSET,
                (float) (.5D + direction.getStepX() * LIP_OUT),
                (float) LIP_Y,
                (float) (.5D + direction.getStepZ() * LIP_OUT));
    }

    @Override
    protected AABB visibleBounds() {
        return new AABB(
                pos.getX() - 6,
                pos.getY() + LIP_Y - POUR_REACH - 1,
                pos.getZ() - 6,
                pos.getX() + 7,
                pos.getY() + 4,
                pos.getZ() + 7);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        for (PourVisual pour : pours) pour.delete();
    }
}
