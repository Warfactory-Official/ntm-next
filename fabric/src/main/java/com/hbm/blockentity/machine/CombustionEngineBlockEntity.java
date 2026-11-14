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

package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.HatchSwing;
import com.hbm.blockentity.PollutingMachineBlockEntity;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.CombustionEngineMenu;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.CombustibleTrait;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.items.PistonsItem.EnumPistonType;
import com.hbm.items.PistonsItem;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import static com.hbm.api.redstoneoverradio.RadioCommandParser.FUN;
import static com.hbm.api.redstoneoverradio.RadioCommandParser.NAME_SEP;
import static com.hbm.api.redstoneoverradio.RadioCommandParser.VAL;

public class CombustionEngineBlockEntity extends PollutingMachineBlockEntity
        implements SoundEmitter,
                FluidTankAccess,
                AudioLoop,
                EnergyHandler,
                FluidTankHandler,
                MenuProvider,
                FluidSettingsHandler,
                RadioValueProvider,
                RadioControlHandler,
                SyncUnitSchema {

    public static final String[] ROR =
            new String[] {
                VAL + "state",
                VAL + "throttle",
                VAL + "power",
                VAL + "fuel",
                VAL + "efficiency",
                FUN + "setstate" + NAME_SEP + "state",
                FUN + "setthrottle" + NAME_SEP + "throttle"
            };
    public static final int SLOT_FLUID_IN = 0;
    public static final int SLOT_CONTAINER_OUT = 1;
    public static final int SLOT_PISTON = 2;
    public static final int SLOT_BATTERY = 3;
    public static final int SLOT_FLUID_ID = 4;
    public static final int SLOT_COUNT = 5;

    public static final long maxPower = 2_500_000L;
    public static final int FLUID_CAP = 24_000;
    public static @Nullable Consumer<CombustionEngineBlockEntity> CLIENT_SOUND;
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.DIESEL, FLUID_CAP);
    private final FluidTankNTM[] receiving;
    public long power;
    public boolean isOn;

    {
        hbm$syncSet$CombustionEngineBlockEntity$isOn(false);
    }

    public int setting;

    {
        hbm$syncSet$CombustionEngineBlockEntity$setting(0);
    }

    public boolean wasOn;

    {
        hbm$syncSet$CombustionEngineBlockEntity$wasOn(false);
    }

    public int tenth = 0;
    public int playersUsing;

    {
        hbm$syncSet$CombustionEngineBlockEntity$playersUsing(0);
    }

    public float doorAngle = 0;
    public float prevDoorAngle = 0;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public CombustionEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMBUSTION_ENGINE.get(), pos, state, SLOT_COUNT, 50);
        receiving = new FluidTankNTM[] {tank};
    }

    @Override
    public void tickServer() {
        if (tank.loadTank(SLOT_FLUID_IN, SLOT_CONTAINER_OUT, inventory)) setChanged();
        if (tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory)) {
            tenth = 0;
            setChanged();
        }

        hbm$syncSet$CombustionEngineBlockEntity$wasOn(false);

        int fill = tank.getFill() * 10 + tenth;
        ItemStack pistonStack = inventory.get(SLOT_PISTON);
        CombustibleTrait trait =
                NTMFluidProperties.getTrait(tank.getTankType(), CombustibleTrait.class);

        if (isOn
                && setting > 0
                && pistonStack.getItem() instanceof PistonsItem pistons
                && fill > 0
                && trait != null) {
            EnumPistonType piston = pistons.type;
            double eff = piston.eff[trait.grade().ordinal()];

            if (eff > 0 && breatheAir(TickPhase.every(this, 5) ? setting : 0)) {
                int speed = setting * 2;
                int toBurn = Math.min(fill, speed);
                this.hbm$syncSet$CombustionEngineBlockEntity$power(
                        (long) (this.power + (toBurn * (trait.energyPerBucket() / 10_000D) * eff)));
                fill -= toBurn;

                if (TickPhase.every(this, 5) && toBurn > 0) {
                    pollute(tank.getTankType(), FluidReleaseType.BURN, toBurn * 0.5F);
                }

                if (toBurn > 0) hbm$syncSet$CombustionEngineBlockEntity$wasOn(true);

                tank.setFill(fill / 10);
                tenth = fill % 10;
                if (toBurn > 0) markChanged();
            }
        }

        this.hbm$syncSet$CombustionEngineBlockEntity$power(
                (long)
                        (this.power
                                - (ItemEnergyTransfer.insert(this, SLOT_BATTERY, power, false))));
        flush.provide((ServerLevel) level, this);

        if (power > maxPower) hbm$syncSet$CombustionEngineBlockEntity$power(maxPower);

        networkPackNT(50);
    }

    @Override
    public void tickClient() {
        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);

        prevDoorAngle = doorAngle;
        doorAngle = HatchSwing.next(doorAngle, playersUsing > 0);
    }

    public void openInventory() {
        if (level != null && !level.isClientSide()) {
            hbm$syncSet$CombustionEngineBlockEntity$playersUsing((int) (playersUsing + 1));
            setChanged();
        }
    }

    public void closeInventory() {
        if (level != null && !level.isClientSide()) {
            hbm$syncSet$CombustionEngineBlockEntity$playersUsing((int) (playersUsing - 1));
            setChanged();
        }
    }

    public double getEfficiency() {
        ItemStack pistonStack = inventory.get(SLOT_PISTON);
        CombustibleTrait trait =
                NTMFluidProperties.getTrait(tank.getTankType(), CombustibleTrait.class);
        if (!(pistonStack.getItem() instanceof PistonsItem pistons) || trait == null) return 0;
        return pistons.type.eff[trait.grade().ordinal()];
    }

    @Override
    public SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.IGENERATOR_OPERATE.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1.0F,
                10F,
                1.0F,
                20);
    }

    public void setControlSettings(boolean togglePower, boolean settingPresent, int setting) {
        if (togglePower) hbm$syncSet$CombustionEngineBlockEntity$isOn(!isOn);
        if (settingPresent)
            this.hbm$syncSet$CombustionEngineBlockEntity$setting(Mth.clamp(setting, 0, 30));
        if (togglePower || settingPresent) setChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String name) {
        if ((VAL + "state").equals(name)) return "" + (isOn ? 1 : 0);
        if ((VAL + "throttle").equals(name)) return "" + setting;
        if ((VAL + "power").equals(name)) return "" + power;
        if ((VAL + "fuel").equals(name)) return "" + tank.getFill();
        if ((VAL + "efficiency").equals(name)) return "" + (int) Math.round(getEfficiency() * 100);
        return null;
    }

    @Override
    public void runRadioFunction(String name, String[] params) {
        if ((FUN + "setstate").equals(name) && params.length > 0) {
            try {
                hbm$syncSet$CombustionEngineBlockEntity$isOn(Integer.parseInt(params[0]) == 1);
                setChanged();
            } catch (NumberFormatException ignored) {
            }
            return;
        }
        if ((FUN + "setthrottle").equals(name) && params.length > 0) {
            try {
                hbm$syncSet$CombustionEngineBlockEntity$setting(
                        Mth.clamp(Integer.parseInt(params[0]), 0, 30));
                setChanged();
            } catch (NumberFormatException ignored) {
            }
            return;
        }
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        hbm$syncSet$CombustionEngineBlockEntity$power(Math.max(0L, Math.min(p, maxPower)));
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return getSmokeTanks();
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (FluidTankNTM tank : getSendingTanks()) out.add(tank, FlushFaces.own());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUID_IN -> FluidTankNTM.isFluidContainer(stack);
            case SLOT_PISTON -> stack.getItem() instanceof PistonsItem;
            case SLOT_BATTERY -> EnergyItem.matches(stack);
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

    @Override
    public int tankCount() {
        return 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        if (index != 0) throw new IndexOutOfBoundsException(index);
        return tank;
    }

    @Override
    public FluidTankNTM pasteTank() {
        return tank;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new CombustionEngineMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getInt("setting").ifPresent(v -> hbm$syncSet$CombustionEngineBlockEntity$setting(v));
        input.getLong("power").ifPresent(v -> hbm$syncSet$CombustionEngineBlockEntity$power(v));
        hbm$syncSet$CombustionEngineBlockEntity$isOn(input.getBooleanOr("isOn", isOn));
        input.getInt("tenth").ifPresent(v -> tenth = v);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("setting", setting);
        output.putLong("power", power);
        output.putBoolean("isOn", isOn);
        output.putInt("tenth", tenth);
        tank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.playersUsing);
            case 1 -> output.writeInt(this.setting);
            case 2 -> output.writeLong(this.power);
            case 3 -> output.writeBoolean(this.isOn);
            case 4 -> output.writeBoolean(this.wasOn);
            case 5 -> this.tank.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.hbm$syncSet$CombustionEngineBlockEntity$playersUsing(input.readInt());
            case 1 -> this.hbm$syncSet$CombustionEngineBlockEntity$setting(input.readInt());
            case 2 -> this.hbm$syncSet$CombustionEngineBlockEntity$power(input.readLong());
            case 3 -> this.hbm$syncSet$CombustionEngineBlockEntity$isOn(input.readBoolean());
            case 4 -> this.hbm$syncSet$CombustionEngineBlockEntity$wasOn(input.readBoolean());
            case 5 -> this.tank.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public boolean tickSound() {
        return wasOn;
    }

    public final void hbm$syncSet$CombustionEngineBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$CombustionEngineBlockEntity$isOn(boolean next) {
        if (this.isOn != next) {
            this.isOn = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$CombustionEngineBlockEntity$setting(int next) {
        if (this.setting != next) {
            this.setting = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$CombustionEngineBlockEntity$wasOn(boolean next) {
        if (this.wasOn != next) {
            this.wasOn = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$CombustionEngineBlockEntity$playersUsing(int next) {
        if (this.playersUsing != next) {
            this.playersUsing = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(CombustionEngineBlockEntity.class, "tank", 3, 1L << 5);
    }

    private boolean hbm$soundActive;

    public boolean soundStarted(boolean active) {
        boolean started = active && !hbm$soundActive;
        hbm$soundActive = active;
        return started;
    }

    private SoundHandle hbm$audio;

    public void audioLoop(boolean running, float volume) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, 0F, false);
    }

    public void audioLoop(boolean running, float volume, float pitch) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, pitch, true);
    }

    private void hbm$audioRemove() {
        audioLoop(false, 0F);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        hbm$audioRemove();
    }
}
