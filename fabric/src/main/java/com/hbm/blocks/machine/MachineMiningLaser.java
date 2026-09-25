// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityMachineMiningLaser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

@RefreshesNeighborState(
        be = BlockEntityMachineMiningLaser.class,
        calling = {"updateRedstonePower", "refreshUnloadTargets"})
public class MachineMiningLaser extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    private static final int[] DIMENSIONS = {1, 1, 1, 1, 1, 1};

    public MachineMiningLaser(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public int getHeightOffset() {
        return -1;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        visitor.cell(core.east(), MASK_EAST, ROLE_FLUID);
        visitor.cell(core.west(), MASK_WEST, ROLE_FLUID);
        visitor.cell(core.south(), MASK_SOUTH, ROLE_FLUID);
        visitor.cell(core.north(), MASK_NORTH, ROLE_FLUID);
        visitor.cell(core.above(), MASK_UP, ROLE_POWER);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int sides = PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.east(), MASK_ALL, sides);
        visitor.passiveCell(core.west(), MASK_ALL, sides);
        visitor.passiveCell(core.south(), MASK_ALL, sides);
        visitor.passiveCell(core.north(), MASK_ALL, sides);
        visitor.passiveCell(core.above(), MASK_ALL, PASSIVE_POWER_IN);
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu) {
            openCoreMenu(player, core, menu);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineMiningLaser(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.MINING_LASER)
                .powerIn()
                .fluidOut()
                .items()
                .itemsAtCells()
                .fe();
    }
}
