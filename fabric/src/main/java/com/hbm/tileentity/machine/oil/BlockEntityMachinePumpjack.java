// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.oil;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.data.MachineData;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityMachinePumpjack extends BlockEntityOilDrillBase {

    private static final Direction[] VENT_DIRS = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    public float rot;
    public float prevRot;
    public float speed;

    public BlockEntityMachinePumpjack(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_PUMPJACK.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.pumpjack");
    }

    @Override
    public long getMaxPower() {
        return MachineData.PUMPJACK_MAX_POWER.get();
    }

    @Override
    public long getPowerReq() {
        return MachineData.PUMPJACK_CONSUMPTION.get();
    }

    @Override
    public int getDelay() {
        return MachineData.PUMPJACK_DELAY.get();
    }

    @Override
    public void tickClient() {
        this.prevRot = rot;
        if (indicator == 0) this.rot += speed;
        if (rot >= 360F) {
            this.prevRot -= 360F;
            this.rot -= 360F;
        }
    }

    @Override
    protected void onDrill(BlockPos dug) {
        Block b = level.getBlockState(dug).getBlock();
        ItemStack stack = new ItemStack(b);
        if (stack.isEmpty()) return;

        if (stack.is(Mats.MAT_URANIUM.tag(MaterialShapes.ORE))) {
            ventSides(ModBlocks.GAS_RADON_DENSE.get().defaultBlockState());
        }
        if (stack.is(Mats.MAT_ASBESTOS.tag(MaterialShapes.ORE))) {
            ventSides(ModBlocks.GAS_ASBESTOS.get().defaultBlockState());
        }
    }

    private void ventSides(BlockState gas) {
        for (Direction dir : VENT_DIRS) {
            BlockPos vent = worldPosition.relative(dir);
            if (level.getBlockState(vent).canBeReplaced()) {
                level.setBlock(vent, gas, 3);
            }
        }
    }

    @Override
    protected void onSuck(BlockPos pos) {
        tanks[0].setFill(tanks[0].getFill() + MachineData.PUMPJACK_OIL_PER_DEPOSIT.get());
        int gasMin = MachineData.PUMPJACK_GAS_PER_DEPOSIT_MIN.get();
        tanks[1].setFill(
                tanks[1].getFill()
                        + gasMin
                        + level.getRandom()
                                .nextInt(
                                        MachineData.PUMPJACK_GAS_PER_DEPOSIT_MAX.get()
                                                - gasMin
                                                + 1));

        if (level.getRandom().nextDouble() < MachineData.PUMPJACK_DRAIN_CHANCE.get()) {
            level.setBlock(pos, ModBlocks.ORE_OIL_EMPTY.get().defaultBlockState(), 3);
        }
        setChanged();
    }

    private void writeDrillState(ByteBuf output) {
        output.writeInt(indicator);
        output.writeFloat(indicator == 0 ? (5F + 2F * speedLevel) + (overLevel - 1F) * 10F : 0F);
    }

    private void readDrillState(ByteBuf input) {
        indicator = input.readInt();
        speed = input.readFloat();
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 5;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 5 -> writeDrillState(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 5 -> readDrillState(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
