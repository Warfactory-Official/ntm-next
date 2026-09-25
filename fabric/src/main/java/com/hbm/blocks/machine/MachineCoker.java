// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.oil.BlockEntityMachineCoker;
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

@RefreshesNeighborState(be = BlockEntityMachineCoker.class, calling = "refreshHeatBelow")
public class MachineCoker extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {

    private static final int[] PLACEMENT_DIMENSIONS = {
        22, 0, 1, 1, 1, 1, 0, 0, 0,
        5, 0, 2, 2, 2, 2, 0, 1, 0,
        0, 1, 0, 0, 0, 0, 2, 1, 2,
        0, 1, 0, 0, 0, 0, 2, 1, -2,
        0, 1, 0, 0, 0, 0, -2, 1, 2,
        0, 1, 0, 0, 0, 0, -2, 1, -2,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final int[] DIMENSIONS = {22, 0, 1, 1, 1, 1};

    private static final int[] PLATFORM = {5, 0, 2, 2, 2, 2};
    private static final int[] LEG = {0, 1, 0, 0, 0, 0};

    public MachineCoker(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        BlockPos core = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);

        return MultiblockHandlerXR.checkSpace(level, core.above(), PLATFORM, core, Direction.NORTH)
                && MultiblockHandlerXR.checkSpace(
                        level, core.offset(2, 1, 2), LEG, core, Direction.NORTH)
                && MultiblockHandlerXR.checkSpace(
                        level, core.offset(2, 1, -2), LEG, core, Direction.NORTH)
                && MultiblockHandlerXR.checkSpace(
                        level, core.offset(-2, 1, 2), LEG, core, Direction.NORTH)
                && MultiblockHandlerXR.checkSpace(
                        level, core.offset(-2, 1, -2), LEG, core, Direction.NORTH);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        MultiblockHandlerXR.visitBox(core.above(), PLATFORM, Direction.NORTH, visitor);
        MultiblockHandlerXR.visitBox(core.offset(2, 1, 2), LEG, Direction.NORTH, visitor);
        MultiblockHandlerXR.visitBox(core.offset(2, 1, -2), LEG, Direction.NORTH, visitor);
        MultiblockHandlerXR.visitBox(core.offset(-2, 1, 2), LEG, Direction.NORTH, visitor);
        MultiblockHandlerXR.visitBox(core.offset(-2, 1, -2), LEG, Direction.NORTH, visitor);

        for (int i = -1; i <= 1; i += 2) {
            for (int j = -1; j <= 1; j += 2) {
                visitor.cell(core.offset(i, 0, j), MachineChemicalPlant.ringMask(i, j));
            }
        }
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.SUCCESS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu)
            openCoreMenu(player, core, menu);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineCoker(pos, state);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        for (int i = -1; i <= 1; i += 2) {
            for (int j = -1; j <= 1; j += 2) {
                visitor.passiveCell(core.offset(i, 0, j), MASK_ALL, domains);
            }
        }
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.COKER).fluidIn().fluidOut().items().itemsAtCells();
    }
}
