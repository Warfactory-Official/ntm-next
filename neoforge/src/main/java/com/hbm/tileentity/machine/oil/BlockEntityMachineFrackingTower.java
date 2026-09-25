// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.oil;

import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.data.MachineData;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncField;
import com.hbm.world.feature.OilSurfaceScarring;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineFrackingTower extends BlockEntityOilDrillBase
        implements IFluidHandlerMK2 {

    private static final String[] PERSISTENT_KEYS = {"power", "oil", "gas", "fracksol"};

    @SyncField(units = 1L << 6)
    public final FluidTankNTM frackSolTank = new FluidTankNTM(NTMFluids.FRACKSOL, 64_000);

    public BlockEntityMachineFrackingTower(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FRACKING_TOWER.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.frackingTower");
    }

    @Override
    public long getMaxPower() {
        return MachineData.FRACKING_MAX_POWER.get();
    }

    @Override
    public long getPowerReq() {
        return MachineData.FRACKING_CONSUMPTION.get();
    }

    @Override
    public int getDelay() {
        return MachineData.FRACKING_DELAY.get();
    }

    @Override
    protected int getDrillDepth() {
        return level.getMinY();
    }

    @Override
    protected boolean canPump() {
        boolean b = frackSolTank.getFill() >= MachineData.FRACKING_SOLUTION_REQUIRED.get();
        if (!b) indicator = 3;
        return b;
    }

    @Override
    protected boolean canSuckBlock(Block b) {
        return super.canSuckBlock(b) || b == ModBlocks.ORE_BEDROCK_OIL.get();
    }

    @Override
    protected void doSuck(BlockPos pos) {
        super.doSuck(pos);
        if (level.getBlockState(pos).is(ModBlocks.ORE_BEDROCK_OIL.get())) onSuck(pos);
    }

    @Override
    protected void onSuck(BlockPos pos) {
        Block b = level.getBlockState(pos).getBlock();
        RandomSource rand = level.getRandom();

        int oil = 0, gas = 0;
        if (b == ModBlocks.ORE_OIL.get()) {
            oil = MachineData.FRACKING_OIL_PER_DEPOSIT.get();
            int gasMin = MachineData.FRACKING_GAS_PER_DEPOSIT_MIN.get();
            gas =
                    gasMin
                            + rand.nextInt(
                                    MachineData.FRACKING_GAS_PER_DEPOSIT_MAX.get() - gasMin + 1);
            if (rand.nextDouble() < MachineData.FRACKING_DRAIN_CHANCE.get()) {
                level.setBlock(pos, ModBlocks.ORE_OIL_EMPTY.get().defaultBlockState(), 3);
            }
        }

        if (b == ModBlocks.ORE_BEDROCK_OIL.get()) {
            oil = MachineData.FRACKING_OIL_PER_BEDROCK_DEPOSIT.get();
            int gasMin = MachineData.FRACKING_GAS_PER_BEDROCK_DEPOSIT_MIN.get();
            gas =
                    gasMin
                            + rand.nextInt(
                                    MachineData.FRACKING_GAS_PER_BEDROCK_DEPOSIT_MAX.get()
                                            - gasMin
                                            + 1);
        }

        tanks[0].setFill(Math.min(tanks[0].getFill() + oil, tanks[0].getMaxFill()));
        tanks[1].setFill(Math.min(tanks[1].getFill() + gas, tanks[1].getMaxFill()));

        frackSolTank.setFill(frackSolTank.getFill() - MachineData.FRACKING_SOLUTION_REQUIRED.get());

        OilSurfaceScarring.generateOilSpot(
                level,
                rand,
                worldPosition.getX(),
                worldPosition.getZ(),
                MachineData.FRACKING_DESTRUCTION_RANGE.get(),
                10);
        setChanged();
    }

    @Override
    public @Nullable FluidTankNTM extraTank() {
        return frackSolTank;
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (!frackSolTank.accepts(type) || pressure != frackSolTank.getPressure()) return 0L;
        return (long) frackSolTank.getMaxFill() - frackSolTank.getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (!frackSolTank.accepts(type) || pressure != frackSolTank.getPressure()) return amount;
        int accepted = frackSolTank.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        return amount - accepted;
    }

    @Override
    protected FluidTankNTM[] drillTanks() {
        return new FluidTankNTM[] {tanks[0], tanks[1], frackSolTank};
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("fracksol").ifPresent(frackSolTank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        frackSolTank.serialize(output.child("fracksol"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 6;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 6 -> this.frackSolTank.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 6 -> this.frackSolTank.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
