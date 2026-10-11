// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.oil.BlockEntityMachineVacuumDistill;
import com.hbm.blockentity.machine.oil.BlockEntityMachineVacuumDistill;
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

public class MachineVacuumDistill extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {
    public MachineVacuumDistill(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineVacuumDistill(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.VACUUUM_DISTILL)
                .powerIn()
                .fluidIn()
                .fluidOut()
                .items()
                .fe()
                .faces(BlockEntityMachineVacuumDistill.class, (be, side) -> side != Direction.DOWN)
                .fluidFaces(
                        BlockEntityMachineVacuumDistill.class,
                        (be, face) -> face.side() != Direction.DOWN);
    }
}
