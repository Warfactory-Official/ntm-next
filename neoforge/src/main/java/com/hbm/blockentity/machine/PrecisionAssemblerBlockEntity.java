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
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blockentity.processing.SelectedProcessorSupport;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.PrecAssMenu;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.BlueprintsItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.modules.machine.ModuleMachineAssembler;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ObjDoubleConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public final class PrecisionAssemblerBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                FluidTankHandler,
                FluidTankAccess,
                MenuProvider,
                UpgradeInfoProvider,
                AudioLoop,
                SyncUnitSchema {
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_BLUEPRINT = 1;
    public static final int SLOT_UPGRADE_START = 2;
    public static final int SLOT_UPGRADE_END = 3;
    public static final int SLOT_INPUT_START = 4;
    public static final int SLOT_INPUT_END = 12;
    public static final int SLOT_OUTPUT_START = 13;
    public static final int SLOT_OUTPUT_END = 21;
    public static final int SLOT_COUNT = 22;
    public static final long MIN_POWER = 100_000L;
    private static final double[] WORKING = {45D, -15D, -5D};
    private static final double[] IDLE = {45D, -30D, 45D};
    private static final double JOINT_STEP = 15D;
    private static final double IMPACT = -0.75D;
    private static final double RETRACTION = 0.5D;
    private static final int SHORT_DELAY = 2;
    private static final int LONG_DELAY = 10;
    private static final int LONG_DELAY_CHOICES = 3;
    private static final double RING_STEP = 45D;
    private static final double RING_SPEED = 10D;
    private static final double RING_SPEED_SPREAD = 5D;
    private static final int DWELL = 100;
    private static final int DWELL_CHOICES = 21;
    private static final int[] INPUTS = {4, 5, 6, 7, 8, 9, 10, 11, 12};
    private static final int[] OUTPUTS = {13, 14, 15, 16, 17, 18, 19, 20, 21};
    private static final int[] ACCESSIBLE = {
        4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21
    };
    private static final FlushFaces FLUSH = FlushFaces.own();
    public static Consumer<PrecisionAssemblerBlockEntity> CLIENT_SOUND;
    public static ObjDoubleConsumer<PrecisionAssemblerBlockEntity> CLIENT_SOUND_START;
    public static Consumer<PrecisionAssemblerBlockEntity> CLIENT_SOUND_STRIKE;
    public static ObjDoubleConsumer<PrecisionAssemblerBlockEntity> CLIENT_SOUND_STOP;

    public long power;
    public long maxPower = MIN_POWER;
    public boolean isProgressing;
    public final ModuleMachineAssembler module;
    public final FluidTankNTM inputTank = new FluidTankNTM(4_000);
    public final FluidTankNTM outputTank = new FluidTankNTM(4_000);
    private final FluidTankNTM[] inputs = {inputTank};
    private final FluidTankNTM[] outputs = {outputTank};
    private final FluidTankNTM[] tanks = {inputTank, outputTank};
    private final UpgradeManager upgrades = new UpgradeManager(this);
    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public final double[] armAngles = WORKING.clone();
    public final double[] prevArmAngles = WORKING.clone();
    public final double[] strikers = new double[4];
    public final double[] prevStrikers = new double[4];
    public double ring;
    public double prevRing;
    public boolean frame;
    private double ringTarget;
    private double ringSpeed;
    private int ringDwell;
    private int strikerDelay;
    private int nextStriker;
    private int pendingImpact = -1;

    public PrecisionAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRECASS.get(), pos, state, SLOT_COUNT);
        module =
                new ModuleMachineAssembler(
                        0, this, PrecAssRecipes.INSTANCE, this, INPUTS, OUTPUTS, inputs, outputs);
        inputTank.bindOwner(this);
        outputTank.bindOwner(this);
    }

    @Override
    public void tickServer() {
        long capacity = SelectedProcessorSupport.capacity(module, maxPower, power, MIN_POWER);
        if (maxPower != capacity) {
            maxPower = capacity;
            setChanged();
        }
        setPower(power + ItemEnergyTransfer.extract(this, SLOT_BATTERY, maxPower - power, false));
        upgrades.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        module.update(
                SelectedProcessorSupport.speed(upgrades),
                SelectedProcessorSupport.power(upgrades),
                true,
                getItem(SLOT_BLUEPRINT));
        hbm$syncSet$PrecisionAssemblerBlockEntity$isProgressing(module.didProcess);
        if (module.markDirty || isProgressing) setChanged();
        flush.provide((ServerLevel) level, this);
        networkPackNT(100);
    }

    @Override
    public void tickClient() {
        System.arraycopy(armAngles, 0, prevArmAngles, 0, armAngles.length);
        System.arraycopy(strikers, 0, prevStrikers, 0, strikers.length);
        prevRing = ring;
        if (TickPhase.every(this, 20)) frame = !level.isEmptyBlock(worldPosition.above(3));

        boolean retracted = true;
        for (int arm = 0; arm < strikers.length; arm++) {
            if (arm == pendingImpact) {
                strikers[arm] = IMPACT;
                CLIENT_SOUND_STRIKE.accept(this);
            } else {
                strikers[arm] = Math.min(0D, strikers[arm] + RETRACTION);
            }
            retracted &= strikers[arm] == 0D;
        }
        pendingImpact = -1;

        var random = level.getRandom();
        if (ring != ringTarget) {
            ring = approach(ring, ringTarget, ringSpeed);
            if (ring == ringTarget) {
                double offset = ringTarget >= 360D ? -360D : 360D;
                ring += offset;
                prevRing += offset;
                ringTarget += offset;
                ringDwell = DWELL + random.nextInt(DWELL_CHOICES);
            }
        }
        if (isProgressing && ring == ringTarget) {
            if (ringDwell > 0) ringDwell--;
            else {
                ringTarget += random.nextBoolean() ? RING_STEP : -RING_STEP;
                ringSpeed = RING_SPEED + random.nextDouble() * RING_SPEED_SPREAD;
                CLIENT_SOUND_START.accept(this, 1.25F + random.nextFloat() * 0.25F);
            }
        }

        boolean wasWorking = working(prevArmAngles);
        if (retracted) {
            var target = isProgressing ? WORKING : IDLE;
            for (int joint = 0; joint < armAngles.length; joint++) {
                armAngles[joint] = approach(armAngles[joint], target[joint], JOINT_STEP);
            }
        }
        boolean atWork = working(armAngles);
        if (isProgressing && atWork) {
            if (strikerDelay > 0) strikerDelay--;
            if (strikerDelay == 0) {
                pendingImpact = nextStriker;
                nextStriker = (nextStriker + 1) % strikers.length;
                strikerDelay =
                        nextStriker == 3
                                ? LONG_DELAY + random.nextInt(LONG_DELAY_CHOICES)
                                : SHORT_DELAY;
            }
        }
        if (wasWorking && !atWork)
            CLIENT_SOUND_STOP.accept(this, 1.25F + random.nextFloat() * 0.25F);
        CLIENT_SOUND.accept(this);
    }

    private static double approach(double value, double target, double step) {
        return value < target ? Math.min(value + step, target) : Math.max(value - step, target);
    }

    private static boolean working(double[] joints) {
        for (int joint = 0; joint < joints.length; joint++) {
            if (joints[joint] != WORKING[joint]) return false;
        }
        return true;
    }

    public void armPositions(int arm, float partialTick, double[] output) {
        JointedArm.interpolate(prevArmAngles, armAngles, partialTick, output);
        output[3] = Mth.lerp(partialTick, prevStrikers[arm], strikers[arm]);
    }

    public @Nullable GenericRecipe selectedRecipe() {
        return module.getRecipe();
    }

    public void selectRecipe(String recipe) {
        if (SelectedProcessorSupport.select(module, recipe, false)) setChanged();
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public void setPower(long stored) {
        long next = Math.clamp(stored, 0L, maxPower);
        if (next != power) {
            power = next;
            setChanged();
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return EnergyItem.matches(stack);
        if (slot == SLOT_BLUEPRINT) return stack.getItem() instanceof BlueprintsItem;
        if (slot >= SLOT_UPGRADE_START && slot <= SLOT_UPGRADE_END)
            return MachineUpgradeItem.isUpgrade(stack);
        return module.isItemValid(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_OUTPUT_START && slot <= SLOT_OUTPUT_END || module.isSlotClogged(slot);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot >= SLOT_INPUT_START
                && slot <= SLOT_INPUT_END
                && module.isItemValid(slot, stack);
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return inputs;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return outputs;
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
    public void declareFlush(FlushLanes out) {
        out.add(outputTank, FLUSH);
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return SelectedProcessorSupport.UPGRADES;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int tier, List<Component> tooltip) {
        UpgradeInfo.processor(type, tier, tooltip);
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable("container.machinePrecAss");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PrecAssMenu(id, inventory, this);
    }

    @Override
    public @Nullable SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.ASSEMBLER_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                0.5F,
                15F,
                0.75F,
                20);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        inputTank.serialize(output.child("tankIn"));
        outputTank.serialize(output.child("tankOut"));
        module.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", power);
        maxPower = input.getLongOr("maxPower", maxPower);
        input.child("tankIn").ifPresent(inputTank::deserialize);
        input.child("tankOut").ifPresent(outputTank::deserialize);
        module.load(input);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(isProgressing);
            case 1 -> module.serialize(output);
            case 2 -> inputTank.packetSerialize(output);
            case 3 -> outputTank.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$PrecisionAssemblerBlockEntity$isProgressing(input.readBoolean());
            case 1 -> module.deserialize(input);
            case 2 -> inputTank.packetDeserialize(input);
            case 3 -> outputTank.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$PrecisionAssemblerBlockEntity$isProgressing(boolean next) {
        if (this.isProgressing != next) {
            this.isProgressing = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(PrecisionAssemblerBlockEntity.class, "module", 3, 2);
        com.hbm.packet.SyncBindings.field(PrecisionAssemblerBlockEntity.class, "inputTank", 3, 4);
        com.hbm.packet.SyncBindings.field(PrecisionAssemblerBlockEntity.class, "outputTank", 3, 8);
        com.hbm.inventory.container.SyncedData.fields(
                PrecisionAssemblerBlockEntity.class, "maxPower", "power");
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
