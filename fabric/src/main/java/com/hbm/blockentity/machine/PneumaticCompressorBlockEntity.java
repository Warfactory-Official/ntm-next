/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
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

package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.fluidmk2.PlainFluidProvider;
import com.hbm.api.fluidmk2.PlainFluidReceiver;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.MachineSoundProvider;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.CompressorMenu;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CompressorRecipe;
import com.hbm.inventory.recipes.CompressorRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class PneumaticCompressorBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                PlainFluidReceiver,
                PlainFluidProvider,
                FluidFlushSender,
                FluidSettingsHandler,
                UpgradeInfoProvider,
                MenuProvider,
                SyncUnitSchema,
                SoundEmitter,
                MachineSoundProvider {

    public static final int SLOT_FLUID_ID = 0;
    public static final int SLOT_BATTERY = 1;
    public static final int SLOT_UPGRADE_START = 2;
    public static final int SLOT_UPGRADE_END = 3;
    public static final int SLOT_COUNT = 4;
    public static final long maxPower = 100_000L;

    private static final int[] GENERIC_DURATION = {100, 60, 20, 10};
    private static final UpgradeLimits UPGRADES =
            UpgradeLimits.of(UpgradeType.SPEED, 3)
                    .and(UpgradeType.POWER, 3)
                    .and(UpgradeType.OVERDRIVE, 9);
    private static final FlushFaces OUTPUT_FACES = FlushFaces.activePlane();

    public int progress;
    public int processTime;

    {
        hbm$syncSet$PneumaticCompressorBlockEntity$processTime(100);
    }

    public int powerRequirement;
    public long power;
    public final FluidTankNTM[] tanks = {
        new FluidTankNTM(16_000), new FluidTankNTM(16_000).withPressure(1)
    };
    public boolean canProcess;

    public float fanSpin;
    public float prevFanSpin;
    public float piston;
    public float prevPiston;

    private boolean extending = true;
    private float returnStep = 0.1F;
    private final FluidTankNTM[] sending = {tanks[1]};
    private final UpgradeManager upgrades = new UpgradeManager(this);
    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public PneumaticCompressorBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.COMPRESSOR.get(), pos, state);
    }

    protected PneumaticCompressorBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        long charged = ItemEnergyTransfer.extract(this, SLOT_BATTERY, maxPower - power, false);
        if (charged != 0) {
            hbm$syncSet$PneumaticCompressorBlockEntity$power((long) (power + (charged)));
            setChanged();
        }

        var selector = inventory.get(SLOT_FLUID_ID);
        if (selector.getItem() instanceof FluidIdentifierItem) {
            var fluid =
                    selector.getOrDefault(
                                    ModDataComponents.FLUID_IDENTIFIER.get(),
                                    FluidIdentifierData.EMPTY)
                            .primary();
            Fluid exact = fluid == Fluids.EMPTY ? null : fluid;
            if (tanks[0].getDeclaredFluid() != exact || tanks[0].isStrict() != (exact != null)) {
                tanks[0].setTankTypeByIdentifier(exact);
                setChanged();
            }
        }

        var recipe =
                CompressorRecipes.INSTANCE.find(tanks[0].getTankType(), tanks[0].getPressure());
        if (configureOutput(recipe)) setChanged();
        upgrades.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int speed = upgrades.getLevel(UpgradeType.SPEED);
        int overdrive = upgrades.getLevel(UpgradeType.OVERDRIVE);
        int duration = recipe == null ? GENERIC_DURATION[speed] : recipe.duration / (speed + 1);
        hbm$syncSet$PneumaticCompressorBlockEntity$processTime(
                Math.max(1, duration / (overdrive + 1)));
        hbm$syncSet$PneumaticCompressorBlockEntity$powerRequirement(
                2500 / (upgrades.getLevel(UpgradeType.POWER) + 1) * (2 * overdrive + 1));

        long inputAmount = recipe == null ? 1000 : recipe.inputFluid[0].amount();
        long outputAmount = recipe == null ? 1000 : recipe.outputFluid[0].amount();
        boolean admitted =
                power > powerRequirement
                        && tanks[0].getFill() >= inputAmount
                        && tanks[1].getMaxFill() - tanks[1].getFill() >= outputAmount
                        && (recipe != null || tanks[1].accepts(tanks[0].getFluid()));
        hbm$syncSet$PneumaticCompressorBlockEntity$canProcess(admitted);
        if (admitted) {
            hbm$syncSet$PneumaticCompressorBlockEntity$power((long) (power - (powerRequirement)));
            hbm$syncSet$PneumaticCompressorBlockEntity$progress((int) (progress + 1));
            if (progress >= processTime) {
                Fluid produced = tanks[0].getFluid();
                tanks[0].setFill(tanks[0].getFill() - inputAmount);
                if (recipe == null) tanks[1].receive(produced, (int) outputAmount);
                else recipe.outputFluid[0].fillInto(tanks[1]);
                hbm$syncSet$PneumaticCompressorBlockEntity$progress(0);
            }
            setChanged();
        } else if (progress != 0) {
            hbm$syncSet$PneumaticCompressorBlockEntity$progress(0);
            setChanged();
        }

        flush.provide((ServerLevel) level, this);
        networkPackNT(100);
    }

    private boolean configureOutput(@Nullable CompressorRecipe recipe) {
        var output = tanks[1];
        Fluid previousType = output.getTankType();
        int previousPressure = output.getPressure();
        if (recipe == null) {
            output.setTankType(tanks[0].getTankType());
            output.withPressure(tanks[0].getPressure() + 1);
        } else {
            output.conform(recipe.outputFluid[0]);
        }
        return previousType != output.getTankType() || previousPressure != output.getPressure();
    }

    @Override
    public void tickClient() {
        prevPiston = piston;
        advanceFan(15F);
        if (!canProcess) return;
        if (extending) {
            piston += 0.05F;
            if (piston >= 1F) {
                extending = false;
                returnStep = 0.085F + level.getRandom().nextFloat() * 0.03F;
            }
        } else {
            piston -= returnStep;
            if (piston <= 0F) {
                extending = true;
                AudioSystem.playMachineSound(
                        level,
                        worldPosition,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5,
                        ModSounds.COMPRESSOR_PISTON.get(),
                        SoundSource.BLOCKS,
                        getVolume(0.5F),
                        0.75F);
            }
        }
        piston = Math.clamp(piston, 0F, 1F);
    }

    protected final void advanceFan(float amount) {
        prevFanSpin = fanSpin;
        if (!canProcess) return;
        fanSpin += amount;
        if (fanSpin >= 360F) {
            fanSpin -= 360F;
            prevFanSpin -= 360F;
        }
    }

    @Override
    public boolean tickSound() {
        return canProcess;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$PneumaticCompressorBlockEntity$power(stored);
    }

    @Override
    public long getMaxPower() {
        return maxPower;
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
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        visitor.accept(tanks[0]);
        visitor.accept(tanks[1]);
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(tanks[1], OUTPUT_FACES);
    }

    @Override
    public boolean canReceiveFluid(Fluid type) {
        return tanks[0].joins(type);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        return pressure == tanks[0].getPressure() && tanks[0].accepts(type)
                ? tanks[0].getMaxFill() - tanks[0].getFill()
                : 0;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != tanks[0].getPressure() || !tanks[0].accepts(type)) return amount;
        int moved = tanks[0].receive(type, (int) Math.min(amount, Integer.MAX_VALUE));
        if (moved != 0) setChanged();
        return amount - moved;
    }

    @Override
    public int receivingPressureMask(Fluid type) {
        return tanks[0].accepts(type) ? 1 << tanks[0].getPressure() : 0;
    }

    @Override
    public long getReceiverSpeed(Fluid type, int pressure) {
        return FluidTankHandler.REQUEST_LIMIT;
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        return pressure == tanks[1].getPressure() && tanks[1].provides(type)
                ? tanks[1].getFill()
                : 0;
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        int moved = (int) Math.min(amount, getFluidAvailable(type, pressure));
        if (moved == 0) return;
        tanks[1].setFill(tanks[1].getFill() - moved);
        setChanged();
    }

    @Override
    public int providingPressureMask(Fluid type) {
        return tanks[1].provides(type) ? 1 << tanks[1].getPressure() : 0;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_BATTERY -> EnergyItem.matches(stack);
            case SLOT_UPGRADE_START, SLOT_UPGRADE_END -> MachineUpgradeItem.isUpgrade(stack);
            default -> false;
        };
    }

    public void setControlCompression(int pressure) {
        if (tanks[0].getPressure() == pressure) return;
        tanks[0].withPressure(pressure);
        configureOutput(CompressorRecipes.INSTANCE.find(tanks[0].getTankType(), pressure));
        setChanged();
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM pasteTank() {
        return tanks[0];
    }

    @Override
    public CompoundTag copySettings(Level level, BlockPos pos) {
        var settings = FluidSettingsHandler.super.copySettings(level, pos);
        settings.putInt("compression", tanks[0].getPressure());
        return settings;
    }

    @Override
    public void pasteSettings(
            CompoundTag settings, int option, Level level, Player player, BlockPos pos) {
        settings.getInt("compression").ifPresent(this::setControlCompression);
        FluidSettingsHandler.super.pasteSettings(settings, option, level, player, pos);
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return UPGRADES;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int level, List<Component> tooltip) {
        if (type == UpgradeType.SPEED) {
            UpgradeInfo.percent(
                    tooltip,
                    "tooltip.hbm.upgrade.compression",
                    GENERIC_DURATION[level] - 100,
                    true);
            UpgradeInfo.percent(
                    tooltip, "tooltip.hbm.upgrade.recipe", UpgradeInfo.reduction(level + 1), true);
        } else if (type == UpgradeType.POWER) {
            UpgradeInfo.percent(
                    tooltip, "upgrade.consumption", UpgradeInfo.reduction(level + 1), true);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineCompressor");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CompressorMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
        tanks[0].serialize(output.child("0"));
        tanks[1].serialize(output.child("1"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$PneumaticCompressorBlockEntity$power(input.getLongOr("power", 0L));
        hbm$syncSet$PneumaticCompressorBlockEntity$progress(input.getIntOr("progress", 0));
        input.child("0").ifPresent(tanks[0]::deserialize);
        input.child("1").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected boolean syncMuffled() {
        return true;
    }

    @Override
    public long syncUnitMask() {
        return 0x3fL | super.syncUnitMask();
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(progress);
            case 1 -> output.writeInt(processTime);
            case 2 -> output.writeInt(powerRequirement);
            case 3 -> output.writeLong(power);
            case 4 -> {
                tanks[0].packetSerialize(output);
                tanks[1].packetSerialize(output);
            }
            case 5 -> output.writeBoolean(canProcess);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$PneumaticCompressorBlockEntity$progress(input.readInt());
            case 1 -> hbm$syncSet$PneumaticCompressorBlockEntity$processTime(input.readInt());
            case 2 -> hbm$syncSet$PneumaticCompressorBlockEntity$powerRequirement(input.readInt());
            case 3 -> hbm$syncSet$PneumaticCompressorBlockEntity$power(input.readLong());
            case 4 -> {
                tanks[0].packetDeserialize(input);
                tanks[1].packetDeserialize(input);
            }
            case 5 -> hbm$syncSet$PneumaticCompressorBlockEntity$canProcess(input.readBoolean());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$PneumaticCompressorBlockEntity$progress(int next) {
        if (this.progress != next) {
            this.progress = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$PneumaticCompressorBlockEntity$processTime(int next) {
        if (this.processTime != next) {
            this.processTime = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$PneumaticCompressorBlockEntity$powerRequirement(int next) {
        if (this.powerRequirement != next) {
            this.powerRequirement = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$PneumaticCompressorBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final FluidTankNTM hbm$syncArraySet$PneumaticCompressorBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 1L << 4);
            }
        }
        return next;
    }

    public final void hbm$syncSet$PneumaticCompressorBlockEntity$canProcess(boolean next) {
        if (this.canProcess != next) {
            this.canProcess = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(
                PneumaticCompressorBlockEntity.class, "tanks", 3, 1L << 4);
    }

    private boolean hbm$soundActive;

    public boolean soundStarted(boolean active) {
        boolean started = active && !hbm$soundActive;
        hbm$soundActive = active;
        return started;
    }
}
