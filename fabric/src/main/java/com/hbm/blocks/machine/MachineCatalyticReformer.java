// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntityMachineCatalyticReformer;
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

public class MachineCatalyticReformer extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {
    public MachineCatalyticReformer(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineCatalyticReformer(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.CATALYTIC_REFORMER)
                .powerIn()
                .fluidIn()
                .fluidOut()
                .items()
                .fe()
                .faces(
                        BlockEntityMachineCatalyticReformer.class,
                        (be, side) -> side != Direction.DOWN)
                .fluidFaces(
                        BlockEntityMachineCatalyticReformer.class,
                        (be, face) -> face.side() != Direction.DOWN);
    }
}
