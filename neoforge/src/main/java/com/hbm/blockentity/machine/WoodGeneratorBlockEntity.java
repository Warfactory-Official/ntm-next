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

package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.WoodBurnerMenu;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FlammableTrait;
import com.hbm.items.ModItems;
import com.hbm.items.machine.AshType;
import com.hbm.modules.ModuleBurnTime;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class WoodGeneratorBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                FluidTankHandler,
                FluidSettingsHandler,
                SyncUnitSchema,
                MenuProvider {

    public static final int SLOT_FUEL = 0;
    public static final int SLOT_ASH = 1;
    public static final int SLOT_FLUID_ID = 2;
    public static final int SLOT_FLUID_IN = 3;
    public static final int SLOT_FLUID_OUT = 4;
    public static final int SLOT_BATTERY = 5;
    public static final int SLOT_COUNT = 6;
    public static final ModuleBurnTime BURN_MODULE =
            new ModuleBurnTime().setLogTimeMod(4).setWoodTimeMod(2);
    private static final int[] AUTOMATED_SLOTS = {SLOT_FUEL, SLOT_ASH};
    private static final int ASH_TICKS = 2_000;

    public final long getMaxPower = 100_000L;
    public long power;
    public int burnTime;
    public int maxBurnTime;
    public int powerGen;
    public boolean isOn;
    public boolean liquidBurn;
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.WOODOIL, 16_000);
    private final FluidTankNTM[] fuelTanks = {tank};
    public int ashLevelWood;
    public int ashLevelCoal;
    public int ashLevelMisc;

    public WoodGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOOD_BURNER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        if (tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory)) setChanged();
        if (tank.loadTank(SLOT_FLUID_IN, SLOT_FLUID_OUT, inventory)) setChanged();
        var charged = ItemEnergyTransfer.insert(this, SLOT_BATTERY, power, false);
        if (charged > 0) setPower(power - charged);
        hbm$syncSet$WoodGeneratorBlockEntity$powerGen(0);
        if (liquidBurn) burnLiquid();
        else burnSolid();
        networkPackNT(25);
    }

    private int fuelDuration(ItemStack stack) {
        return BURN_MODULE.getBurnTime(this, stack);
    }

    private void burnSolid() {
        if (burnTime > 0) {
            if (isOn && power < getMaxPower && breatheAir(1)) {
                hbm$syncSet$WoodGeneratorBlockEntity$burnTime((int) (burnTime - 1));
                hbm$syncSet$WoodGeneratorBlockEntity$powerGen(100);
                setChanged();
                setPower(power + powerGen);
                if (TickPhase.every(this, 20)) {
                    PollutionHandler.add(
                            level,
                            worldPosition,
                            PollutionType.SOOT,
                            PollutionHandler.SOOT_PER_SECOND);
                }
            }
            return;
        }
        var fuel = inventory.get(SLOT_FUEL);
        int duration = fuelDuration(fuel);
        if (duration <= 0) return;
        var remainder = fuel.getCraftingRemainder();
        var ash = FireboxBaseBlockEntity.getAshFromFuel(fuel);
        hbm$syncSet$WoodGeneratorBlockEntity$burnTime(duration);
        hbm$syncSet$WoodGeneratorBlockEntity$maxBurnTime(duration);
        switch (ash) {
            case WOOD -> ashLevelWood += duration;
            case COAL -> ashLevelCoal += duration;
            default -> ashLevelMisc += duration;
        }
        ashLevelWood = payAsh(ashLevelWood, AshType.WOOD);
        ashLevelCoal = payAsh(ashLevelCoal, AshType.COAL);
        ashLevelMisc = payAsh(ashLevelMisc, AshType.MISC);
        fuel.shrink(1);
        if (fuel.isEmpty())
            inventory.set(SLOT_FUEL, remainder == null ? ItemStack.EMPTY : remainder.create());
        setChanged();
    }

    private int payAsh(int ticks, AshType kind) {
        int available = ticks / ASH_TICKS;
        if (available <= 0) return ticks;
        var item = ModItems.POWDER_ASH.get(kind);
        var output = inventory.get(SLOT_ASH);
        if (output.isEmpty()) {
            var produced = new ItemStack(item);
            int count = Math.min(available, produced.getMaxStackSize());
            produced.setCount(count);
            inventory.set(SLOT_ASH, produced);
            return ticks - count * ASH_TICKS;
        }
        if (!output.is(item)) return ticks;
        int count = Math.min(available, output.getMaxStackSize() - output.getCount());
        if (count <= 0) return ticks;
        output.grow(count);
        return ticks - count * ASH_TICKS;
    }

    private void burnLiquid() {
        if (!isOn || power >= getMaxPower || tank.getFill() <= 0 || !breatheAir(1)) return;
        var fuel = NTMFluidProperties.getTrait(tank.getTankType(), FlammableTrait.class);
        if (fuel == null) return;
        int amount = Math.min(tank.getFill(), 2);
        tank.setFill(tank.getFill() - amount);
        hbm$syncSet$WoodGeneratorBlockEntity$powerGen(
                (int) (fuel.heatPerBucket() * amount / 2_000L));
        setChanged();
        setPower(power + powerGen);
        if (TickPhase.every(this, 20)) {
            PollutionHandler.add(
                    level,
                    worldPosition,
                    PollutionType.SOOT,
                    PollutionHandler.SOOT_PER_SECOND * amount / 2F);
        }
    }

    @Override
    public void tickClient() {
        if (powerGen <= 0) return;
        var facing = getBlockState().getValue(MultiblockCoreBlock.FACING);
        var right = facing.getClockWise();
        level.addParticle(
                ParticleTypes.SMOKE,
                worldPosition.getX() + 0.5D - facing.getStepX() + right.getStepX(),
                worldPosition.getY() + 4D,
                worldPosition.getZ() + 0.5D - facing.getStepZ() + right.getStepZ(),
                0D,
                0.05D,
                0D);
    }

    public void togglePower() {
        hbm$syncSet$WoodGeneratorBlockEntity$isOn(!isOn);
        setChanged();
    }

    public void toggleFuelMode() {
        hbm$syncSet$WoodGeneratorBlockEntity$liquidBurn(!liquidBurn);
        setChanged();
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public long getMaxPower() {
        return getMaxPower;
    }

    @Override
    public void setPower(long stored) {
        long clamped = Math.clamp(stored, 0L, getMaxPower);
        if (power == clamped) return;
        hbm$syncSet$WoodGeneratorBlockEntity$power(clamped);
        setChanged();
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return fuelTanks;
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return fuelTanks;
    }

    @Override
    public FluidTankNTM pasteTank() {
        return tank;
    }

    @Override
    public int tankCount() {
        return 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return fuelTanks[index];
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATED_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_FUEL && BURN_MODULE.isFuel(this, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_ASH;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineWoodBurner");
    }

    @Override
    public WoodBurnerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new WoodBurnerMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("burnTime", burnTime);
        output.putInt("maxBurnTime", maxBurnTime);
        output.putInt("ashWood", ashLevelWood);
        output.putInt("ashCoal", ashLevelCoal);
        output.putInt("ashMisc", ashLevelMisc);
        output.putBoolean("isOn", isOn);
        output.putBoolean("liquidBurn", liquidBurn);
        tank.serialize(output.child("tank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$WoodGeneratorBlockEntity$power(input.getLongOr("power", 0L));
        hbm$syncSet$WoodGeneratorBlockEntity$burnTime(input.getIntOr("burnTime", 0));
        hbm$syncSet$WoodGeneratorBlockEntity$maxBurnTime(input.getIntOr("maxBurnTime", 0));
        ashLevelWood = input.getIntOr("ashWood", 0);
        ashLevelCoal = input.getIntOr("ashCoal", 0);
        ashLevelMisc = input.getIntOr("ashMisc", 0);
        hbm$syncSet$WoodGeneratorBlockEntity$isOn(input.getBooleanOr("isOn", false));
        hbm$syncSet$WoodGeneratorBlockEntity$liquidBurn(input.getBooleanOr("liquidBurn", false));
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x7fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeInt(burnTime);
            case 2 -> output.writeInt(maxBurnTime);
            case 3 -> output.writeInt(powerGen);
            case 4 -> output.writeBoolean(isOn);
            case 5 -> output.writeBoolean(liquidBurn);
            case 6 -> tank.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$WoodGeneratorBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$WoodGeneratorBlockEntity$burnTime(input.readInt());
            case 2 -> hbm$syncSet$WoodGeneratorBlockEntity$maxBurnTime(input.readInt());
            case 3 -> hbm$syncSet$WoodGeneratorBlockEntity$powerGen(input.readInt());
            case 4 -> hbm$syncSet$WoodGeneratorBlockEntity$isOn(input.readBoolean());
            case 5 -> hbm$syncSet$WoodGeneratorBlockEntity$liquidBurn(input.readBoolean());
            case 6 -> tank.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$WoodGeneratorBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$WoodGeneratorBlockEntity$burnTime(int next) {
        if (this.burnTime != next) {
            this.burnTime = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$WoodGeneratorBlockEntity$maxBurnTime(int next) {
        if (this.maxBurnTime != next) {
            this.maxBurnTime = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$WoodGeneratorBlockEntity$powerGen(int next) {
        if (this.powerGen != next) {
            this.powerGen = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$WoodGeneratorBlockEntity$isOn(boolean next) {
        if (this.isOn != next) {
            this.isOn = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$WoodGeneratorBlockEntity$liquidBurn(boolean next) {
        if (this.liquidBurn != next) {
            this.liquidBurn = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(WoodGeneratorBlockEntity.class, "tank", 3, 1L << 6);
    }
}
