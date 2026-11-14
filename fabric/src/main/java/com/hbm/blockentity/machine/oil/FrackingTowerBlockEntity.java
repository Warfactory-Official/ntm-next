/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blockentity.machine.oil;

import com.hbm.api.fluidmk2.PlainFluidReceiver;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.FluidReserve;
import com.hbm.data.MachineData;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.world.feature.OilSurfaceScarring;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class FrackingTowerBlockEntity extends OilDrillBaseBlockEntity
        implements PlainFluidReceiver {

    static final DepositYield YIELD =
            new DepositYield(
                    MachineData.FRACKING_OIL_PER_DEPOSIT,
                    MachineData.FRACKING_GAS_PER_DEPOSIT_MIN,
                    MachineData.FRACKING_GAS_PER_DEPOSIT_MAX,
                    MachineData.FRACKING_DRAIN_CHANCE);
    static final DepositYield BEDROCK_YIELD =
            new DepositYield(
                    MachineData.FRACKING_OIL_PER_BEDROCK_DEPOSIT,
                    MachineData.FRACKING_GAS_PER_BEDROCK_DEPOSIT_MIN,
                    MachineData.FRACKING_GAS_PER_BEDROCK_DEPOSIT_MAX,
                    null);
    public static final int SOLUTION_SHORT = 3;

    private static final int SCAR_SPOTS = 10;
    private static final String[] PERSISTENT_KEYS = {"power", "oil", "gas", "fracksol"};

    public final FluidTankNTM solution = new FluidTankNTM(NTMFluids.FRACKSOL, TANK_CAPACITY);
    private final FluidTankNTM[] allTanks = {tanks[0], tanks[1], solution};

    public FrackingTowerBlockEntity(BlockPos pos, BlockState state) {
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
    protected boolean canSuckBlock(Block block) {
        return super.canSuckBlock(block)
                || block == ModBlocks.ORE_BEDROCK_OIL.get()
                || FluidReserve.of(block) != null;
    }

    @Override
    protected boolean canPump() {
        if (solution.getFill() >= MachineData.FRACKING_SOLUTION_REQUIRED.get()) return true;
        hbm$syncSet$OilDrillBaseBlockEntity$indicator(SOLUTION_SHORT);
        return false;
    }

    @Override
    protected void doSuck(BlockPos pos) {
        super.doSuck(pos);
        if (level.getBlockState(pos).is(ModBlocks.ORE_BEDROCK_OIL.get())) onSuck(pos);
    }

    @Override
    protected void onSuck(BlockPos pos) {
        boolean bedrock = level.getBlockState(pos).is(ModBlocks.ORE_BEDROCK_OIL.get());
        (bedrock ? BEDROCK_YIELD : YIELD).pump(level, pos, tanks[0], tanks[1]);
        useSolution();
        OilSurfaceScarring.generateOilSpot(
                level,
                level.getRandom(),
                worldPosition.getX(),
                worldPosition.getZ(),
                MachineData.FRACKING_DESTRUCTION_RANGE.get(),
                SCAR_SPOTS);
        markChanged();
    }

    @Override
    protected void onReserveSuck(BlockPos pos, FluidReserve reserve) {
        super.onReserveSuck(pos, reserve);
        useSolution();
        reserve.scarSurface(
                level,
                level.getRandom(),
                worldPosition.getX(),
                worldPosition.getZ(),
                MachineData.FRACKING_DESTRUCTION_RANGE.get());
        markChanged();
    }

    private void useSolution() {
        solution.setFill(solution.getFill() - MachineData.FRACKING_SOLUTION_REQUIRED.get());
    }

    @Override
    public @Nullable FluidTankNTM extraTank() {
        return solution;
    }

    @Override
    protected FluidTankNTM[] drillTanks() {
        return allTanks;
    }

    @Override
    public boolean canReceiveFluid(Fluid type) {
        return solution.joins(type);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        return pressure == solution.getPressure() && solution.accepts(type)
                ? solution.getMaxFill() - solution.getFill()
                : 0L;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != solution.getPressure() || !solution.accepts(type)) return amount;
        int added = solution.receive(type, (int) Math.min(amount, Integer.MAX_VALUE));
        if (added > 0) markChanged();
        return amount - added;
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        for (FluidTankNTM tank : allTanks) visitor.accept(tank);
    }

    @Override
    public int tankCount() {
        return allTanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return allTanks[index];
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("fracksol").ifPresent(solution::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        solution.serialize(output.child("fracksol"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 6;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit == 6) solution.packetSerialize(output);
        else super.writeSyncUnit(unit, output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit == 6) solution.packetDeserialize(input);
        else super.readSyncUnit(unit, input);
    }

    static {
        com.hbm.packet.SyncBindings.field(FrackingTowerBlockEntity.class, "solution", 3, 1L << 6);
    }
}
