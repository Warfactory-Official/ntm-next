// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.oil;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.data.MachineData;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityMachineOilWell extends BlockEntityOilDrillBase {

    public BlockEntityMachineOilWell(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DERRICK.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.oilWell");
    }

    @Override
    public long getMaxPower() {
        return MachineData.DERRICK_MAX_POWER.get();
    }

    @Override
    public long getPowerReq() {
        return MachineData.DERRICK_CONSUMPTION.get();
    }

    @Override
    public int getDelay() {
        return MachineData.DERRICK_DELAY.get();
    }

    @Override
    protected void onDrill(BlockPos dug) {
        Block b = level.getBlockState(dug).getBlock();
        ItemStack stack = new ItemStack(b);
        if (stack.isEmpty()) return;

        if (stack.is(Mats.MAT_URANIUM.tag(MaterialShapes.ORE))) {
            ventColumn(ModBlocks.GAS_RADON_DENSE.get().defaultBlockState());
        }
        if (stack.is(Mats.MAT_ASBESTOS.tag(MaterialShapes.ORE))) {
            ventColumn(ModBlocks.GAS_ASBESTOS.get().defaultBlockState());
        }
    }

    private void ventColumn(BlockState gas) {
        for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 1; k++) {

                if (level.getBlockState(worldPosition.offset(j, 10, j)).canBeReplaced()) {
                    level.setBlock(worldPosition.offset(k, 10, k), gas, 3);
                }
            }
        }
    }

    @Override
    protected void onSuck(BlockPos pos) {
        level.playSound(
                null, worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 2.0F, 0.5F);

        tanks[0].setFill(tanks[0].getFill() + MachineData.DERRICK_OIL_PER_DEPOSIT.get());
        int gasMin = MachineData.DERRICK_GAS_PER_DEPOSIT_MIN.get();
        tanks[1].setFill(
                tanks[1].getFill()
                        + gasMin
                        + level.getRandom()
                                .nextInt(
                                        MachineData.DERRICK_GAS_PER_DEPOSIT_MAX.get()
                                                - gasMin
                                                + 1));

        if (level.getRandom().nextDouble() < MachineData.DERRICK_DRAIN_CHANCE.get()) {
            level.setBlock(pos, ModBlocks.ORE_OIL_EMPTY.get().defaultBlockState(), 3);
        }
        setChanged();
    }
}
