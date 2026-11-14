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
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.port.CapabilityPortProvider;
import com.hbm.capability.port.ItemPort;
import com.hbm.capability.port.ItemPortCache;
import com.hbm.inventory.container.CyclotronMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CyclotronRecipe;
import com.hbm.inventory.recipes.CyclotronRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class CyclotronBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                FluidTankHandler,
                FluidSettingsHandler,
                UpgradeInfoProvider,
                CapabilityPortProvider,
                MenuProvider,
                SyncUnitSchema {

    public static final int LINES = 3;
    public static final int SLOT_PARTICLE_START = 0;
    public static final int SLOT_TARGET_START = 3;
    public static final int SLOT_OUTPUT_START = 6;
    public static final int SLOT_BATTERY = 9;
    public static final int SLOT_UPGRADE_START = 10;
    public static final int SLOT_UPGRADE_END = 11;
    public static final int SLOT_COUNT = 12;
    public static final int PLUGS = 4;
    public static final long MAX_POWER = 100_000_000L;
    public static final int DURATION = 690;
    public static final int WATER_CAPACITY = 32_000;
    public static final int STEAM_CAPACITY = 32_000;
    public static final int AMAT_CAPACITY = 8_000;

    private static final int[][] LANE_SLOTS = {{0, 3, 6, 7, 8}, {1, 4, 6, 7, 8}, {2, 5, 6, 7, 8}};
    private static final UpgradeLimits UPGRADES =
            UpgradeLimits.of(UpgradeType.SPEED, 3)
                    .and(UpgradeType.POWER, 3)
                    .and(UpgradeType.EFFECT, 3);
    private static final FlushFaces OUTPUT_FACES = FlushFaces.activePlane();

    public long power;
    public int progress;
    public byte plugs;
    public final FluidTankNTM water;
    public final FluidTankNTM steam;
    public final FluidTankNTM amat;

    private final FluidTankNTM[] tanks;
    private final FluidTankNTM[] intake;
    private final FluidTankNTM[] sending;
    private final UpgradeManager upgrades = new UpgradeManager(this);
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final ItemPortCache ports =
            new ItemPortCache(
                    this::canPlaceItem,
                    (slot, _) ->
                            slot >= SLOT_OUTPUT_START && slot < SLOT_BATTERY);

    public CyclotronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CYCLOTRON.get(), pos, state, SLOT_COUNT);
        water = new FluidTankNTM(NTMFluids.WATER, WATER_CAPACITY);
        steam = new FluidTankNTM(NTMFluids.SPENTSTEAM, STEAM_CAPACITY);
        amat = new FluidTankNTM(NTMFluids.AMAT, AMAT_CAPACITY);
        tanks = new FluidTankNTM[] {water, steam, amat};
        intake = new FluidTankNTM[] {water};
        sending = new FluidTankNTM[] {steam, amat};
    }

    @Override
    public void tickServer() {
        setPower(power + ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false));
        upgrades.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        if (canProcess()) {
            hbm$syncSet$CyclotronBlockEntity$progress((int) (progress + (getSpeed())));
            setChanged();
            setPower(power - getConsumption());
            int coolant = getCoolantConsumption();
            water.setFill(water.getFill() - coolant);
            steam.setFill(steam.getFill() + coolant);
            if (progress >= DURATION) {
                process();
                hbm$syncSet$CyclotronBlockEntity$progress(0);
            }
        } else if (progress != 0) {
            hbm$syncSet$CyclotronBlockEntity$progress(0);
            setChanged();
        }
        flush.provide((ServerLevel) level, this);
        networkPackNT(25);
    }

    public int getSpeed() {
        return upgrades.getLevel(UpgradeType.SPEED) + 1;
    }

    public long getConsumption() {
        return 1_000_000L - 100_000L * upgrades.getLevel(UpgradeType.POWER);
    }

    public int getCoolantConsumption() {
        return 500 / (upgrades.getLevel(UpgradeType.EFFECT) + 1) * getSpeed();
    }

    public boolean canProcess() {
        int coolant = getCoolantConsumption();
        if (power < getConsumption()
                || water.getFill() < coolant
                || steam.getMaxFill() - steam.getFill() < coolant) return false;
        for (int lane = 0; lane < LINES; lane++) {
            if (usableRecipe(lane) != null) return true;
        }
        return false;
    }

    private @Nullable CyclotronRecipe usableRecipe(int lane) {
        var recipe =
                CyclotronRecipes.getOutput(
                        inventory.get(SLOT_TARGET_START + lane),
                        inventory.get(SLOT_PARTICLE_START + lane));
        if (recipe == null) return null;
        var result = recipe.output();
        if (result.isEmpty()) return null;
        var held = inventory.get(SLOT_OUTPUT_START + lane);
        return held.isEmpty()
                        || ItemStack.isSameItemSameComponents(held, result)
                                && held.getCount() < result.getMaxStackSize()
                ? recipe
                : null;
    }

    public void process() {
        for (int lane = 0; lane < LINES; lane++) {
            var recipe = usableRecipe(lane);
            if (recipe == null) continue;
            int outputSlot = SLOT_OUTPUT_START + lane;
            var held = inventory.get(outputSlot);
            if (held.isEmpty()) inventory.set(outputSlot, recipe.output().copy());
            else held.grow(1);
            inventory.get(SLOT_PARTICLE_START + lane).shrink(1);
            inventory.get(SLOT_TARGET_START + lane).shrink(1);
            amat.setFill(amat.getFill() + recipe.amat);
            setChanged();
        }
    }

    public boolean getPlug(int index) {
        return (plugs & 1 << index) != 0;
    }

    public void setPlug(int index) {
        hbm$syncSet$CyclotronBlockEntity$plugs((byte) (plugs | ((byte) (1 << index))));
        setChanged();
        syncToTracking();
    }

    public static Item itemForPlug(int index) {
        return switch (index) {
            case 0 -> ModItems.POWDER_BALEFIRE.get();
            case 1 -> ModItems.BOOK_OF.get();
            case 2 -> ModItems.DIAMOND_GAVEL.get();
            case 3 -> ModItems.COIN_MASKMAN.get();
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        long next = Math.clamp(stored, 0L, MAX_POWER);
        if (next == power) return;
        hbm$syncSet$CyclotronBlockEntity$power(next);
        setChanged();
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return intake;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(steam, OUTPUT_FACES);
        out.add(amat, OUTPUT_FACES);
    }

    @Override
    public int tankCount() {
        return tanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return tanks;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= SLOT_PARTICLE_START && slot < SLOT_TARGET_START)
            return CyclotronRecipes.isParticle(stack);
        if (slot >= SLOT_TARGET_START && slot < SLOT_OUTPUT_START)
            return CyclotronRecipes.isTarget(stack);
        return false;
    }

    @Override
    public ItemPort itemAccess(BlockPos cell, Direction side) {
        if (cell.getY() != worldPosition.getY()) return ItemPort.none();
        int x = cell.getX() - worldPosition.getX();
        int z = cell.getZ() - worldPosition.getZ();
        int clockwise;
        if (x == 2 && z >= -1 && z <= 1) clockwise = z;
        else if (x == -2 && z >= -1 && z <= 1) clockwise = -z;
        else if (z == 2 && x >= -1 && x <= 1) clockwise = -x;
        else if (z == -2 && x >= -1 && x <= 1) clockwise = x;
        else return ItemPort.none();
        return ports.forSlots(LANE_SLOTS[1 - clockwise]);
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return UPGRADES;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int level, List<Component> tooltip) {
        switch (type) {
            case SPEED -> {
                UpgradeInfo.percent(
                        tooltip, "upgrade.delay", UpgradeInfo.reduction(level + 1), true);
                UpgradeInfo.percent(tooltip, "upgrade.coolantConsumption", 100 * level, false);
            }
            case POWER -> UpgradeInfo.percent(tooltip, "upgrade.consumption", -10 * level, true);
            case EFFECT ->
                    UpgradeInfo.percent(
                            tooltip,
                            "upgrade.coolantConsumption",
                            UpgradeInfo.reduction(level + 1),
                            true);
            default -> {}
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.cyclotron");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new CyclotronMenu(containerId, playerInv, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$CyclotronBlockEntity$power(input.getLongOr("power", 0L));
        hbm$syncSet$CyclotronBlockEntity$progress(input.getIntOr("progress", 0));
        hbm$syncSet$CyclotronBlockEntity$plugs(decodePlugs(input.getIntOr("plugs", 0)));
        for (int i = 0; i < tanks.length; i++) {
            var tank = tanks[i];
            input.child("t" + i).ifPresent(tank::deserialize);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
        output.putInt("plugs", plugs);
        for (int i = 0; i < tanks.length; i++) tanks[i].serialize(output.child("t" + i));
    }

    private static byte decodePlugs(int value) {
        if ((value & ~15) != 0)
            throw new IllegalArgumentException("Invalid cyclotron plug bits: " + value);
        return (byte) value;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 63L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeInt(progress);
            case 2 -> output.writeByte(plugs);
            case 3 -> water.packetSerialize(output);
            case 4 -> steam.packetSerialize(output);
            case 5 -> amat.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$CyclotronBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$CyclotronBlockEntity$progress(input.readInt());
            case 2 -> hbm$syncSet$CyclotronBlockEntity$plugs(decodePlugs(input.readUnsignedByte()));
            case 3, 4, 5 -> tanks[unit - 3].packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$CyclotronBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$CyclotronBlockEntity$progress(int next) {
        if (this.progress != next) {
            this.progress = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$CyclotronBlockEntity$plugs(byte next) {
        if (this.plugs != next) {
            this.plugs = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(CyclotronBlockEntity.class, "water", 3, 1L << 3);
        com.hbm.packet.SyncBindings.field(CyclotronBlockEntity.class, "steam", 3, 1L << 4);
        com.hbm.packet.SyncBindings.field(CyclotronBlockEntity.class, "amat", 3, 1L << 5);
        com.hbm.inventory.container.SyncedData.fields(
                CyclotronBlockEntity.class, "power", "progress");
    }
}
