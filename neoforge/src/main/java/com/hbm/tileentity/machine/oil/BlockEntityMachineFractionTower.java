// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.oil;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.FractionRecipe;
import com.hbm.inventory.recipes.FractionRecipes;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityMachineFractionTower extends BlockEntityMachineBase
        implements FluidTankEndpoint, IFluidCopiable, SyncUnitSchema {

    public static final int TANK_CAPACITY = 4000;

    @SyncField(units = 1L << 0)
    public final FluidTankNTM[] tanks = new FluidTankNTM[3];

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityMachineFractionTower(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FRACTION_TOWER.get(), pos, state, 0);
        tanks[0] = new FluidTankNTM(NTMFluids.HEAVYOIL, TANK_CAPACITY);
        tanks[1] = new FluidTankNTM(NTMFluids.BITUMEN, TANK_CAPACITY);
        tanks[2] = new FluidTankNTM(NTMFluids.SMEAR, TANK_CAPACITY);
        receiving = new FluidTankNTM[] {tanks[0]};
        sending = new FluidTankNTM[] {tanks[1], tanks[2]};
    }

    @Override
    public void tickServer() {

        if (level.getBlockEntity(worldPosition.above(3))
                instanceof BlockEntityMachineFractionTower above) {
            for (int i = 0; i < 3; i++) above.tanks[i].setTankType(tanks[i].getTankType());

            int oil =
                    Math.min(
                            tanks[0].getFill(),
                            above.tanks[0].getMaxFill() - above.tanks[0].getFill());
            int left =
                    Math.min(above.tanks[1].getFill(), tanks[1].getMaxFill() - tanks[1].getFill());
            int right =
                    Math.min(above.tanks[2].getFill(), tanks[2].getMaxFill() - tanks[2].getFill());

            move(tanks[0], above.tanks[0], oil);
            move(above.tanks[1], tanks[1], left);
            move(above.tanks[2], tanks[2], right);
        }

        setupTanks();
        if (TickPhase.every(this, 10)) fractionate();

        flush.provide((ServerLevel) level, this);

        networkPackNT(50);
    }

    private static void move(FluidTankNTM from, FluidTankNTM to, int amount) {
        Fluid fluid = from.getFluid();
        if (to.accepts(fluid)) from.setFill(from.getFill() - to.receive(fluid, amount));
    }

    private void setupTanks() {
        FractionRecipe split = FractionRecipes.INSTANCE.getFractions(tanks[0].getTankType());
        if (split != null) {
            tanks[1].setTankType(split.outputFluid[0].type());
            tanks[2].setTankType(split.outputFluid[1].type());
        } else {
            tanks[0].setTankType(null);
            tanks[1].setTankType(null);
            tanks[2].setTankType(null);
        }
    }

    private void fractionate() {
        FractionRecipe split = FractionRecipes.INSTANCE.getFractions(tanks[0].getTankType());
        if (split == null) return;

        int drain = (int) split.inputFluid[0].amount();
        int left = (int) split.outputFluid[0].amount();
        int right = (int) split.outputFluid[1].amount();

        if (tanks[0].getFill() >= drain && hasSpace(left, right)) {
            tanks[0].setFill(tanks[0].getFill() - drain);
            tanks[1].setFill(tanks[1].getFill() + left);
            tanks[2].setFill(tanks[2].getFill() + right);
        }
    }

    private boolean hasSpace(int left, int right) {
        return tanks[1].getFill() + left <= tanks[1].getMaxFill()
                && tanks[2].getFill() + right <= tanks[2].getMaxFill();
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < 3; i++) {
            FluidTankNTM tank = tanks[i];
            input.child("tank" + i).ifPresent(tank::deserialize);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < 3; i++) tanks[i].serialize(output.child("tank" + i));
    }

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 3; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 3; i++) tanks[i].packetDeserialize(input);
    }

    @Override
    public long syncUnitMask() {
        return 1L << 0;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeTanks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
