// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.pile;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.pile.BlockGraphiteDrilledBase;
import com.hbm.blocks.machine.pile.BlockGraphiteNeutronDetector;
import com.hbm.interfaces.IPileNeutronReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityPileNeutronDetector extends BlockEntity implements IPileNeutronReceiver {

    public int lastNeutrons;
    public int neutrons;
    public int maxNeutrons = 10;

    public BlockEntityPileNeutronDetector(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_NEUTRONDETECTOR.get(), pos, state);
    }

    public void tickServer() {

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof BlockGraphiteNeutronDetector detector)) return;
        boolean withdrawn = state.getValue(BlockGraphiteDrilledBase.WITHDRAWN);

        if (this.neutrons >= this.maxNeutrons && withdrawn) {
            detector.triggerRods(level, worldPosition);
        }
        if (this.neutrons < this.maxNeutrons
                && this.lastNeutrons < this.maxNeutrons
                && !withdrawn) {
            detector.triggerRods(level, worldPosition);
        }

        this.lastNeutrons = this.neutrons;
        this.neutrons = 0;
    }

    @Override
    public void receiveNeutrons(int n) {
        this.neutrons += n;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("maxNeutrons", this.maxNeutrons);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.maxNeutrons = input.getIntOr("maxNeutrons", 10);
    }
}
