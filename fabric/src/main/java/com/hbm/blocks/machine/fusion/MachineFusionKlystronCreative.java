// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.fusion;

import com.hbm.blockentity.machine.fusion.BlockEntityFusionKlystron;
import com.hbm.blockentity.machine.fusion.BlockEntityFusionKlystronCreative;
import com.hbm.blockentity.machine.fusion.FusionPorts;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineFusionKlystronCreative extends BlockFusionMachine implements ICapabilityBlock {
    public MachineFusionKlystronCreative(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFusionKlystronCreative(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed();
    }

    @Override
    public List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        return BlockEntityFusionKlystron.links(core, facing);
    }
}
