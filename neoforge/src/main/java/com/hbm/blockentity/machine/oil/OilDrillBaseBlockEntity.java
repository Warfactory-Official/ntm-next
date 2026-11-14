/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.machine.oil;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.fluidmk2.PlainFluidProvider;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.PersistentDrop;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blockentity.machine.oil.DrillContents;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.FluidReserve;
import com.hbm.inventory.container.OilWellMenu;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.Facing;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class OilDrillBaseBlockEntity extends MachineBlockEntity
        implements FluidTankAccess,
                EnergyHandler,
                FluidFlushSender,
                PlainFluidProvider,
                UpgradeInfoProvider,
                MenuProvider,
                PersistentDrop,
                FluidSettingsHandler,
                SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_OIL_IN = 1;
    public static final int SLOT_OIL_OUT = 2;
    public static final int SLOT_GAS_IN = 3;
    public static final int SLOT_GAS_OUT = 4;
    public static final int SLOT_UPGRADE_START = 5;
    public static final int SLOT_UPGRADE_END = 7;
    public static final int SLOT_COUNT = 8;
    public static final int TANK_CAPACITY = 64_000;
    private static final UpgradeLimits VALID_UPGRADES =
            UpgradeLimits.of(UpgradeType.SPEED, 3)
                    .and(UpgradeType.POWER, 3)
                    .and(UpgradeType.AFTERBURN, 3)
                    .and(UpgradeType.OVERDRIVE, 3);
    private static final String[] PERSISTENT_KEYS = {"power", "oil", "gas"};
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];
    protected final UpgradeManager upgradeManager = new UpgradeManager(this);
    private final HashSet<Long> trace = new HashSet<>();
    public int indicator;

    {
        hbm$syncSet$OilDrillBaseBlockEntity$indicator(0);
    }

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    public long power;
    protected int speedLevel;
    protected int energyLevel;
    protected int overLevel;

    protected OilDrillBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOT_COUNT);
        hbm$syncArraySet$OilDrillBaseBlockEntity$tanks(
                tanks, 0, new FluidTankNTM(NTMFluids.OIL, TANK_CAPACITY));
        hbm$syncArraySet$OilDrillBaseBlockEntity$tanks(
                tanks, 1, new FluidTankNTM(NTMFluids.GAS, TANK_CAPACITY));
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return tanks;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (FluidTankNTM tank : getSendingTanks()) out.add(tank, FlushFaces.own());
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (level == null || level.isClientSide()) return;
        if (slot >= SLOT_UPGRADE_START
                && slot <= SLOT_UPGRADE_END
                && MachineUpgradeItem.isUpgrade(stack)) {
            WorldSounds.machine(
                    level,
                    null,
                    worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 1.5D,
                    worldPosition.getZ() + 0.5D,
                    ModSounds.UPGRADE_PLUG.get(),
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
        }
    }

    public abstract long getMaxPower();

    public abstract long getPowerReq();

    public abstract int getDelay();

    protected void onDrill(BlockPos pos) {}

    protected abstract void onSuck(BlockPos pos);

    protected int getDrillDepth() {
        return level.getMinY() + 5;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new OilWellMenu(containerId, playerInventory, this);
    }

    @Override
    public void tickServer() {
        long priorPower = power;

        boolean unloaded = tanks[0].unloadTank(SLOT_OIL_IN, SLOT_OIL_OUT, inventory);
        unloaded |= tanks[1].unloadTank(SLOT_GAS_IN, SLOT_GAS_OUT, inventory);
        if (unloaded) setChanged();

        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        hbm$syncSet$OilDrillBaseBlockEntity$speedLevel(upgradeManager.getLevel(UpgradeType.SPEED));
        energyLevel = upgradeManager.getLevel(UpgradeType.POWER);
        hbm$syncSet$OilDrillBaseBlockEntity$overLevel(
                upgradeManager.getLevel(UpgradeType.OVERDRIVE) + 1);
        int abLevel = upgradeManager.getLevel(UpgradeType.AFTERBURN);

        int toBurn = Math.min(tanks[1].getFill(), abLevel * 10);
        if (toBurn > 0) {
            tanks[1].setFill(tanks[1].getFill() - toBurn);
            hbm$syncSet$OilDrillBaseBlockEntity$power(Math.min(power + toBurn * 5L, getMaxPower()));
            setChanged();
        }

        hbm$syncSet$OilDrillBaseBlockEntity$power(
                (long)
                        (power
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, getMaxPower() - power, false))));

        flush.provide((ServerLevel) level, this);

        if (power >= getPowerReqEff()
                && tanks[0].getFill() < tanks[0].getMaxFill()
                && tanks[1].getFill() < tanks[1].getMaxFill()) {
            hbm$syncSet$OilDrillBaseBlockEntity$power((long) (power - (getPowerReqEff())));

            if (TickPhase.every(this, getDelayEff())) {
                hbm$syncSet$OilDrillBaseBlockEntity$indicator(0);
                boolean foundFloor = false;

                for (int y = worldPosition.getY() - 1; y >= getDrillDepth(); y--) {
                    BlockPos pos = new BlockPos(worldPosition.getX(), y, worldPosition.getZ());

                    if (!level.getBlockState(pos).is(ModBlocks.OIL_PIPE.get())) {
                        if (trySuck(pos)) {
                            break;
                        } else {
                            tryDrill(pos);
                            break;
                        }
                    }
                    if (y == getDrillDepth()) foundFloor = true;
                }
                if (foundFloor) hbm$syncSet$OilDrillBaseBlockEntity$indicator(1);
            }
        } else {
            hbm$syncSet$OilDrillBaseBlockEntity$indicator(2);
        }

        if (power != priorPower) markChanged();
        networkPackNT(25);
    }

    private long getPowerReqEff() {
        long req = getPowerReq();
        return (req + (req / 4 * speedLevel) - (req / 4 * energyLevel)) * overLevel;
    }

    private long getDelayEff() {
        long delay = getDelay();
        return Math.max(
                (delay - (delay / 4 * speedLevel) + (delay / 10 * energyLevel)) / overLevel, 1);
    }

    private void tryDrill(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock().getExplosionResistance() < 1000F) {
            onDrill(pos);
            level.setBlock(pos, ModBlocks.OIL_PIPE.get().defaultBlockState(), 3);
        } else {
            hbm$syncSet$OilDrillBaseBlockEntity$indicator(2);
        }
    }

    private boolean trySuck(BlockPos pos) {
        Block b = level.getBlockState(pos).getBlock();
        if (!canSuckBlock(b)) return false;
        if (!canPump()) return true;

        trace.clear();
        return suckRec(pos, 0);
    }

    protected boolean canSuckBlock(Block b) {
        FluidReserve reserve = FluidReserve.of(b);
        return b == ModBlocks.ORE_OIL.get()
                || b == ModBlocks.ORE_OIL_EMPTY.get()
                || reserve != null && !reserve.requiresFracking()
                || b instanceof FluidReserve.Drained;
    }

    protected boolean canPump() {
        return true;
    }

    public @Nullable FluidTankNTM extraTank() {
        return null;
    }

    private boolean suckRec(BlockPos pos, int layer) {
        long key = pos.asLong();
        if (!trace.add(key)) return false;
        if (layer > 64) return false;

        Block b = level.getBlockState(pos).getBlock();
        FluidReserve reserve = FluidReserve.of(b);
        if (reserve != null) {
            onReserveSuck(pos, reserve);
            return true;
        }
        if (b == ModBlocks.ORE_OIL.get() || b == ModBlocks.ORE_BEDROCK_OIL.get()) {
            doSuck(pos);
            return true;
        }

        if (b == ModBlocks.ORE_OIL_EMPTY.get() || b instanceof FluidReserve.Drained) {
            for (Direction dir : Facing.shuffled()) {
                if (suckRec(pos.relative(dir), layer + 1)) return true;
            }
        }
        return false;
    }

    protected void doSuck(BlockPos pos) {
        tanks[0].setTankType(NTMFluids.OIL);
        tanks[1].setTankType(NTMFluids.GAS);
        if (level.getBlockState(pos).is(ModBlocks.ORE_OIL.get())) {
            onSuck(pos);
        }
    }

    protected void onReserveSuck(BlockPos pos, FluidReserve reserve) {
        tanks[0].setTankType(reserve.primaryFluid());
        tanks[1].setTankType(reserve.secondaryFluid());
        tanks[0].setFill(
                Math.min(tanks[0].getFill() + reservePrimary(reserve), tanks[0].getMaxFill()));
        if (tanks[1].getTankType() != null) {
            tanks[1].setFill(
                    Math.min(
                            tanks[1].getFill() + reserveSecondary(reserve), tanks[1].getMaxFill()));
        }
        reserve.drain(level, pos, level.getRandom(), reserveDrainMultiplier());
        setChanged();
    }

    protected int reservePrimary(FluidReserve reserve) {
        return reserve.primaryAmount();
    }

    protected int reserveSecondary(FluidReserve reserve) {
        return reserve.secondaryAmount(level.getRandom());
    }

    protected double reserveDrainMultiplier() {
        return 1.0D;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        hbm$syncSet$OilDrillBaseBlockEntity$power(Math.max(0L, Math.min(p, getMaxPower())));
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        for (FluidTankNTM tank : tanks) {
            if (tank.provides(type) && pressure == tank.getPressure()) return tank.getFill();
        }
        return 0L;
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        for (FluidTankNTM tank : tanks) {
            if (tank.provides(type) && pressure == tank.getPressure()) {
                int drained = tank.drain((int) Math.min(amount, Integer.MAX_VALUE), true);
                if (drained > 0) setChanged();
                return;
            }
        }
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int level, List<Component> tooltip) {
        if (type == UpgradeType.SPEED) UpgradeInfo.speedAndPower(tooltip, -level * 25, level * 25);
        else if (type == UpgradeType.POWER)
            UpgradeInfo.powerAndDelay(tooltip, -level * 25, level * 10);
        else
            tooltip.add(
                    Component.translatable("upgrade.burn", level * 10, level * 50)
                            .withStyle(ChatFormatting.GREEN));
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return VALID_UPGRADES;
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$OilDrillBaseBlockEntity$power(input.getLongOr("power", 0L));
        input.child("oil").ifPresent(tanks[0]::deserialize);
        input.child("gas").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        tanks[0].serialize(output.child("oil"));
        tanks[1].serialize(output.child("gas"));
    }

    protected FluidTankNTM[] drillTanks() {
        return tanks;
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        FluidTankNTM[] persistent = drillTanks();
        boolean empty = power == 0;
        for (FluidTankNTM tank : persistent)
            if (tank.getFill() > 0) {
                empty = false;
                break;
            }
        if (empty) return;
        components.set(
                ModDataComponents.DRILL_CONTENTS.get(),
                new DrillContents(power, FluidStackNTM.snapshot(persistent)));
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        DrillContents contents = components.get(ModDataComponents.DRILL_CONTENTS.get());
        if (contents == null) return;
        hbm$syncSet$OilDrillBaseBlockEntity$power(contents.power());
        FluidStackNTM.restore(contents.tanks(), drillTanks());
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 2; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 2; i++) tanks[i].packetDeserialize(input);
    }

    private void writeDrillState(ByteBuf output) {
        output.writeInt(indicator);
    }

    private void readDrillState(ByteBuf input) {
        hbm$syncSet$OilDrillBaseBlockEntity$indicator(input.readInt());
    }

    @Override
    public long syncUnitMask() {
        return 0x23L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> writeTanks(output);
            case 5 -> writeDrillState(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$OilDrillBaseBlockEntity$power(input.readLong());
            case 1 -> readTanks(input);
            case 5 -> readDrillState(input);
            default -> throw new IllegalArgumentException();
        }
    }

    public final FluidTankNTM hbm$syncArraySet$OilDrillBaseBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 1L << 1);
            }
        }
        return next;
    }

    public final void hbm$syncSet$OilDrillBaseBlockEntity$indicator(int next) {
        if (this.indicator != next) {
            this.indicator = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    public final void hbm$syncSet$OilDrillBaseBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$OilDrillBaseBlockEntity$speedLevel(int next) {
        if (this.speedLevel != next) {
            this.speedLevel = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    public final void hbm$syncSet$OilDrillBaseBlockEntity$overLevel(int next) {
        if (this.overLevel != next) {
            this.overLevel = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(OilDrillBaseBlockEntity.class, "tanks", 3, 1L << 1);
    }

    public void stripPersistent(ValueOutput output) {
        for (String key : persistentKeys()) output.discard(key);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        writePersistent(components);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        readPersistent(components);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        stripPersistent(output);
    }
}
