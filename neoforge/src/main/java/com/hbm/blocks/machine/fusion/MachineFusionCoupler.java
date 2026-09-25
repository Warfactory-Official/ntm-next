// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.fusion;

import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionCoupler;
import com.hbm.tileentity.machine.fusion.FusionPorts;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineFusionCoupler extends BlockFusionMachine implements ICapabilityBlock {
    private static final double[] PLACEMENT_EXTRAS = {
        1.5, 3.5, 1, -1, 1.5, 1.5,
        1.5, 3.5, 1, -1, -1.5, -1.5
    };

    @Override
    protected double[] placementExtraBoxes() {
        return PLACEMENT_EXTRAS;
    }

    private static final int[] DIMENSIONS = {3, 0, 1, 1, 1, 1};

    public MachineFusionCoupler(Properties props) {
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
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFusionCoupler(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed();
    }

    @Override
    public List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        return BlockEntityFusionCoupler.links(core, facing);
    }
}
