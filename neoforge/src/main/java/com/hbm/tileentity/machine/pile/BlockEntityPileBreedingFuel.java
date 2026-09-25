// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.pile;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.pile.BlockGraphiteDrilledBase;
import com.hbm.config.BalanceConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.interfaces.IPileNeutronReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityPileBreedingFuel extends BlockEntityPileBase
        implements IPileNeutronReceiver {

    public int neutrons;
    public int lastNeutrons;
    public int progress;

    public BlockEntityPileBreedingFuel(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_BREEDINGFUEL.get(), pos, state);
    }

    public static int maxProgress() {
        return BalanceConfig.enable528 ? 50000 : 30000;
    }

    @Override
    public void tickServer() {
        react();

        if (this.progress >= maxProgress()) {
            level.setBlockAndUpdate(
                    worldPosition,
                    BlockGraphiteDrilledBase.copyShape(
                            getBlockState(),
                            ModBlocks.BLOCK_GRAPHITE_TRITIUM.get().defaultBlockState()));
        }
    }

    private void react() {

        this.lastNeutrons = this.neutrons;
        this.progress += this.neutrons;

        this.neutrons = 0;

        if (lastNeutrons <= 0) return;

        for (int i = 0; i < 2; i++) this.castRay(1);
    }

    @Override
    public void receiveNeutrons(int n) {
        this.neutrons += n;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.progress = input.getIntOr("progress", 0);
        this.neutrons = input.getIntOr("neutrons", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("progress", this.progress);
        output.putInt("neutrons", this.neutrons);
    }
}
