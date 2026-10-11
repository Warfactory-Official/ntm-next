// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntityMachineRadarLarge;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MachineRadarLarge extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {
    public MachineRadarLarge(Properties props) {
        super(props);
    }

    @Override
    protected BlockState cellStateOfKind(int kind, Direction facing, int shapeId) {
        if (kind == 0) return super.cellStateOfKind(kind, facing, shapeId);
        return ModBlocks.RADAR_MAST_CELL
                .get()
                .defaultBlockState()
                .setValue(BlockMultiblockCell.SOUND, cellSound())
                .setValue(BlockMultiblockCell.SEALED, cellsSealRadiation());
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return MachineRadar.openAbove(level, core, player);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineRadarLarge(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.RADAR_LARGE).powerIn().fe();
    }
}
