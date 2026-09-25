// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.handler.BossSpawnHandler;
import com.hbm.tileentity.machine.BlockEntityReactorZirnox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class ReactorZirnox extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {

    private static final int[] PLACEMENT_DIMENSIONS = {
        1, 0, 2, 2, 2, 2, 0, 0, 0,
        4, -2, 1, 1, 1, 1, 0, 0, 0,
        4, -2, 0, 0, 2, -2, 0, 0, 0,
        4, -2, 0, 0, -2, 2, 0, 0, 0,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final int[] DIMENSIONS = {1, 0, 2, 2, 2, 2};

    private static final int[] CHIMNEY = {4, -2, 1, 1, 1, 1};
    private static final int[] TOWER_CW = {4, -2, 0, 0, 2, -2};
    private static final int[] TOWER_CCW = {4, -2, 0, 0, -2, 2};

    private static final int PORT_DOMAINS = PASSIVE_FLUID_IN | PASSIVE_ITEMS;

    public ReactorZirnox(Properties props) {
        super(props);
    }

    @Override
    protected boolean tilts() {
        return true;
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        BlockPos origin = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);
        return MultiblockHandlerXR.checkSpace(level, origin, CHIMNEY, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, origin, TOWER_CW, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, origin, TOWER_CCW, placed, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        MultiblockHandlerXR.visitBox(core, CHIMNEY, facing, visitor);
        MultiblockHandlerXR.visitBox(core, TOWER_CW, facing, visitor);
        MultiblockHandlerXR.visitBox(core, TOWER_CCW, facing, visitor);

        Direction rot = facing.getClockWise();

        visitor.cell(core.relative(rot, 2).above(1), MASK_WEST);
        visitor.cell(core.relative(rot, 2).above(3), MASK_WEST);
        visitor.cell(core.relative(rot, -2).above(1), MASK_EAST);
        visitor.cell(core.relative(rot, -2).above(3), MASK_EAST);

        visitor.cell(core.above(4), MASK_NONE);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();

        visitor.passiveCell(core.relative(rot, 2).above(1), MASK_ALL, PORT_DOMAINS);
        visitor.passiveCell(core.relative(rot, 2).above(3), MASK_ALL, PORT_DOMAINS);
        visitor.passiveCell(core.relative(rot, -2).above(1), MASK_ALL, PORT_DOMAINS);
        visitor.passiveCell(core.relative(rot, -2).above(3), MASK_ALL, PORT_DOMAINS);
        visitor.passiveCell(core.above(4), MASK_ALL, PORT_DOMAINS);
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    @Override
    public boolean wantsNeighborUpdates() {
        return true;
    }

    @Override
    public void cellNeighborChanged(ServerLevel level, BlockPos core, BlockPos cell) {
        if (!(level.getBlockEntity(core) instanceof BlockEntityReactorZirnox reactor)) return;

        boolean powered = false;
        for (int dx = -2; dx <= 2 && !powered; dx++) {
            for (int dy = 0; dy <= 4 && !powered; dy++) {
                for (int dz = -2; dz <= 2 && !powered; dz++) {
                    if (dx == -2 || dx == 2 || dy == 0 || dy == 4 || dz == -2 || dz == 2) {
                        if (level.hasNeighborSignal(core.offset(dx, dy, dz))) powered = true;
                    }
                }
            }
        }
        reactor.setRedstonePowered(powered);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (!player.isSecondaryUseActive()) BossSpawnHandler.markFBI(player);
        return super.useAtCore(coreState, level, core, player, hit);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityReactorZirnox(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.ZIRNOX).fluidIn().fluidOut().items().itemsAtCells();
    }
}
