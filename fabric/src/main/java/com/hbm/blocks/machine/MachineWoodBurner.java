// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntityMachineWoodBurner;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineWoodBurner extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {
    public MachineWoodBurner(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineWoodBurner(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.WOOD_BURNER)
                .powerOut()
                .fluidIn()
                .items()
                .itemsAtCells()
                .fe()
                .faces(
                        BlockEntityMachineWoodBurner.class,
                        (be, side) -> side == null || side == back(be))
                .fluidFaces(
                        BlockEntityMachineWoodBurner.class,
                        (be, face) -> face.side() == null || face.side() == back(be));
    }

    private static Direction back(BlockEntity be) {
        return be.getBlockState().getValue(FACING).getOpposite();
    }
}
