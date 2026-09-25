// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.oil;

import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CrackingRecipe;
import com.hbm.inventory.recipes.CrackingRecipes;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityMachineCrackingTower extends BlockEntityMachineBase
        implements IFluidHandlerMK2, IFluidCopiable, SyncUnitSchema {

    public static final int TANK_COUNT = 5;
    public static final int CRACK_PERIOD = 5;
    private static final int TANK_INPUT = 4000;
    private static final int TANK_STEAM = 8000;
    private static final int TANK_OUTPUT = 4000;

    private static final int TANK_SPENT = 800;

    @SyncField(units = 1L << 0)
    public final FluidTankNTM[] tanks = new FluidTankNTM[TANK_COUNT];

    public BlockEntityMachineCrackingTower(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CATALYTIC_CRACKER.get(), pos, state, 0);
        tanks[0] = new FluidTankNTM(NTMFluids.BITUMEN, TANK_INPUT);
        tanks[1] = new FluidTankNTM(NTMFluids.STEAM, TANK_STEAM);
        tanks[2] = new FluidTankNTM(NTMFluids.OIL, TANK_OUTPUT);
        tanks[3] = new FluidTankNTM(NTMFluids.PETROLEUM, TANK_OUTPUT);
        tanks[4] = new FluidTankNTM(NTMFluids.SPENTSTEAM, TANK_SPENT);
    }

    @Override
    public void tickServer() {
        setupTanks();
        if (TickPhase.every(this, CRACK_PERIOD)) crack();
        networkPackNT(25);
    }

    private void setupTanks() {
        CrackingRecipe recipe = CrackingRecipes.INSTANCE.getCracking(tanks[0].getTankType());

        if (recipe != null) {
            tanks[1].setTankType(NTMFluids.STEAM);
            tanks[2].setTankType(recipe.outputFluid[0].type());
            tanks[3].setTankType(recipe.outputFluid[1].type());
            tanks[4].setTankType(NTMFluids.SPENTSTEAM);
        } else {

            tanks[2].setTankType(NTMFluids.NONE);
            tanks[3].setTankType(NTMFluids.NONE);
            tanks[4].setTankType(NTMFluids.NONE);
        }
    }

    private void crack() {
        CrackingRecipe recipe = CrackingRecipes.INSTANCE.getCracking(tanks[0].getTankType());
        if (recipe == null) return;

        int left = (int) recipe.outputFluid[0].amount();
        int right = (int) recipe.outputFluid[1].amount();

        for (int i = 0; i < 2; i++) {
            if (tanks[0].getFill() >= 100 && tanks[1].getFill() >= 200 && hasSpace(left, right)) {
                tanks[0].setFill(tanks[0].getFill() - 100);
                tanks[1].setFill(tanks[1].getFill() - 200);
                tanks[2].setFill(tanks[2].getFill() + left);
                tanks[3].setFill(tanks[3].getFill() + right);
                tanks[4].setFill(tanks[4].getFill() + 2);
            }
        }
    }

    private boolean hasSpace(int left, int right) {
        return tanks[2].getFill() + left <= tanks[2].getMaxFill()
                && tanks[3].getFill() + right <= tanks[3].getMaxFill()
                && tanks[4].getFill() + 2 <= tanks[4].getMaxFill();
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (pressure != 0) return 0;
        long demand = 0;
        if (tanks[0].accepts(type)) demand += tanks[0].getMaxFill() - tanks[0].getFill();
        if (tanks[1].accepts(type)) demand += tanks[1].getMaxFill() - tanks[1].getFill();
        return demand;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != 0) return amount;
        long remaining = amount;
        if (tanks[0].accepts(type) && remaining > 0) {
            remaining -= tanks[0].fill(type, (int) Math.min(remaining, Integer.MAX_VALUE), true);
        }
        if (tanks[1].accepts(type) && remaining > 0) {
            remaining -= tanks[1].fill(type, (int) Math.min(remaining, Integer.MAX_VALUE), true);
        }
        return remaining;
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        long available = 0;
        for (int i = 2; i <= 4; i++) {
            if (tanks[i].provides(type) && tanks[i].getPressure() == pressure)
                available += tanks[i].getFill();
        }
        return available;
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        long remaining = amount;
        for (int i = 2; i <= 4 && remaining > 0; i++) {
            if (tanks[i].provides(type) && tanks[i].getPressure() == pressure) {
                remaining -= tanks[i].drain((int) Math.min(remaining, Integer.MAX_VALUE), true);
            }
        }
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tanks[0];
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < TANK_COUNT; i++) {
            int idx = i;
            input.child("tank" + i).ifPresent(tanks[idx]::deserialize);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < TANK_COUNT; i++) tanks[i].serialize(output.child("tank" + i));
    }

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 5; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 5; i++) tanks[i].packetDeserialize(input);
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
