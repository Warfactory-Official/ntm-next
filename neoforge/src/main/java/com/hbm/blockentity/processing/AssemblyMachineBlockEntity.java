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

package com.hbm.blockentity.processing;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blockentity.machine.JointedArm;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.AssemblyMachineMenu;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.items.tool.SwordMeteoriteItem;
import com.hbm.modules.machine.ModuleMachineAssembler;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class AssemblyMachineBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                FluidTankHandler,
                FluidSettingsHandler,
                UpgradeInfoProvider,
                MenuProvider,
                AudioLoop,
                SyncUnitSchema,
                RadioValueProvider,
                RadioControlHandler {

    public static final int SLOT_COUNT = 17;
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_BLUEPRINT = 1;
    public static final int SLOT_UPGRADE_START = 2;
    public static final int SLOT_UPGRADE_END = 3;
    public static final int SLOT_INPUT_START = 4;
    public static final int SLOT_INPUT_END = 15;
    public static final int SLOT_OUTPUT = 16;
    public static final long MAX_POWER = 100_000L;

    private static final int[] INPUT_SLOTS = {4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] OUTPUT_SLOTS = {16};
    private static final int[] AUTOMATION_SLOTS = {4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    private static final String[] RADIO = {
        "VAL:progress", "VAL:recipe", "VAL:active", "FUN:setrecipe!name"
    };
    private static final FlushFaces OUTPUT_FACES = FlushFaces.activePlane();

    public static Consumer<AssemblyMachineBlockEntity> CLIENT_SOUND =
            _ -> {};
    public static Consumer<AssemblyMachineBlockEntity> CLIENT_SOUND_START =
            _ -> {};
    public static Consumer<AssemblyMachineBlockEntity> CLIENT_SOUND_STRIKE =
            _ -> {};
    public static Consumer<AssemblyMachineBlockEntity> CLIENT_SOUND_STOP =
            _ -> {};

    public long power;
    public long maxPower = MAX_POWER;
    public boolean isProgressing;
    public final FluidTankNTM inputTank = new FluidTankNTM(4_000);
    public final FluidTankNTM outputTank = new FluidTankNTM(4_000);
    private final FluidTankNTM[] inputs = {inputTank};
    private final FluidTankNTM[] outputs = {outputTank};
    public final ModuleMachineAssembler recipeModule;
    public final UpgradeManager upgradeManager = new UpgradeManager(this);

    public final StrikerArm[] arms = {new StrikerArm(), new StrikerArm()};
    public double ring;
    public double prevRing;
    public boolean frame;
    private final Random motionRandom = new Random();
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private double ringTarget;
    private double ringSpeed;
    private int ringDelay;
    private boolean clientRunning;

    public AssemblyMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASSEMBLYMACHINE.get(), pos, state, SLOT_COUNT);
        inputTank.bindOwner(this);
        outputTank.bindOwner(this);
        recipeModule =
                new ModuleMachineAssembler(
                        0,
                        this,
                        AssemblyMachineRecipes.INSTANCE,
                        this,
                        INPUT_SLOTS,
                        OUTPUT_SLOTS,
                        inputs,
                        outputs);
    }

    @Override
    public void tickServer() {
        var recipe = recipeModule.getRecipe();
        long ceiling =
                Math.max(
                        power,
                        Math.max(MAX_POWER, recipe == null ? maxPower : recipe.power * 100L));
        if (maxPower != ceiling) {
            maxPower = ceiling;
            setChanged();
        }
        setPower(power + ItemEnergyTransfer.extract(this, SLOT_BATTERY, maxPower - power, false));
        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        flush.provide((ServerLevel) level, this);
        recipeModule.update(
                SelectedProcessorSupport.speed(upgradeManager),
                SelectedProcessorSupport.power(upgradeManager),
                true,
                getItem(SLOT_BLUEPRINT));
        hbm$syncSet$AssemblyMachineBlockEntity$isProgressing(recipeModule.didProcess);
        if (isProgressing) {
            SwordMeteoriteItem.upgrade(
                    inventory,
                    SLOT_BATTERY,
                    ModItems.METEORITE_SWORD_ALLOYED,
                    ModItems.METEORITE_SWORD_MACHINED);
        }
        if (isProgressing || recipeModule.markDirty) setChanged();
        networkPackNT(100);
    }

    @Override
    public void tickClient() {
        if (TickPhase.every(this, 20)) frame = !level.getBlockState(worldPosition.above(3)).isAir();
        if (clientRunning != isProgressing) {
            if (isProgressing) CLIENT_SOUND_START.accept(this);
            else CLIENT_SOUND_STOP.accept(this);
            clientRunning = isProgressing;
        }
        for (var arm : arms) {
            if (arm.tick(isProgressing, motionRandom)) CLIENT_SOUND_STRIKE.accept(this);
        }
        prevRing = ring;
        if (isProgressing) {
            if (ringDelay > 0) {
                ringDelay--;
            } else {
                if (ring == ringTarget) {
                    ringTarget = ring + motionRandom.nextDouble() * 270D - 135D;
                    ringSpeed = 10D + motionRandom.nextDouble() * 5D;
                }
                ring += Math.clamp(ringTarget - ring, -ringSpeed, ringSpeed);
                if (ring == ringTarget) ringDelay = 20 + motionRandom.nextInt(21);
            }
            if (Math.abs(ring) >= 360D) {
                double turns = Math.floor(ring / 360D) * 360D;
                ring -= turns;
                prevRing -= turns;
                ringTarget -= turns;
            }
        }
        tickSound();
    }

    public void tickSound() {
        CLIENT_SOUND.accept(this);
    }

    @Override
    public @Nullable SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.ASSEMBLER_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX() + 0.5F,
                worldPosition.getY() + 0.5F,
                worldPosition.getZ() + 0.5F,
                getVolume(0.5F),
                15F,
                0.75F,
                20);
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
        if (next == power) return;
        power = next;
        setChanged();
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
        return 2;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return index == 0 ? inputTank : outputs[index - 1];
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(outputTank, OUTPUT_FACES);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= SLOT_INPUT_START
                && slot < SLOT_OUTPUT
                && recipeModule.isItemValid(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT || recipeModule.isSlotClogged(slot);
    }

    public void selectRecipe(String recipe) {
        if (SelectedProcessorSupport.select(recipeModule, recipe, false)) setChanged();
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
    public String[] getFunctionInfo() {
        return RADIO;
    }

    @Override
    public @Nullable String getRadioValue(String key) {
        return switch (key) {
            case "VAL:progress" -> Long.toString(Math.round(recipeModule.progress * 100D));
            case "VAL:recipe" -> recipeModule.legacyRecipeName();
            case "VAL:active" -> isProgressing ? "1" : "0";
            default -> null;
        };
    }

    @Override
    public void runRadioFunction(String key, String[] params) {
        if (key.equals("FUN:setrecipe")
                && params.length == 1
                && SelectedProcessorSupport.select(recipeModule, params[0], true)) {
            setChanged();
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineAssemblyMachine");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AssemblyMachineMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        recipeModule.save(output);
        inputTank.serialize(output.child("tankIn"));
        outputTank.serialize(output.child("tankOut"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        recipeModule.load(input);
        input.child("tankIn").ifPresent(inputTank::deserialize);
        input.child("tankOut").ifPresent(outputTank::deserialize);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 15L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(isProgressing);
            case 1 -> recipeModule.serialize(output);
            case 2 -> inputTank.packetSerialize(output);
            case 3 -> outputTank.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$AssemblyMachineBlockEntity$isProgressing(input.readBoolean());
            case 1 -> recipeModule.deserialize(input);
            case 2 -> inputTank.packetDeserialize(input);
            case 3 -> outputTank.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public static final class StrikerArm extends JointedArm {
        private static final double[][] POSES = {
            {45D, -15D, -5D}, {15D, 15D, -15D}, {25D, 10D, -15D}, {30D, 0D, -10D}, {70D, -10D, -25D}
        };
        private int phase;
        private int hold;

        public StrikerArm() {
            super(4);
        }

        public boolean tick(boolean working, Random random) {
            snapshot();
            if (!working) {
                home();
                settle(3D, 3D, 0.25D);
                phase = 0;
                return false;
            }
            double before = angles[STRIKER];
            boolean settled = settle(15D, 15D, 0.5D);
            boolean strike = before != -0.75D && angles[STRIKER] == -0.75D;
            if (settled) {
                switch (phase) {
                    case 0 -> {
                        hold = 2 + random.nextInt(5);
                        phase = 1;
                    }
                    case 1 -> {
                        if (--hold == 0) {
                            aimAt(POSES, random);
                            phase = 2;
                        }
                    }
                    case 2 -> {
                        hold = 2;
                        phase = 3;
                    }
                    case 3 -> {
                        if (--hold == 0) {
                            targets[STRIKER] = -0.75D;
                            phase = 4;
                        }
                    }
                    case 4 -> {
                        targets[STRIKER] = 0D;
                        phase = 0;
                    }
                }
            }
            return strike;
        }
    }

    public final void hbm$syncSet$AssemblyMachineBlockEntity$isProgressing(boolean next) {
        if (this.isProgressing != next) {
            this.isProgressing = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(AssemblyMachineBlockEntity.class, "inputTank", 3, 4);
        com.hbm.packet.SyncBindings.field(AssemblyMachineBlockEntity.class, "outputTank", 3, 8);
        com.hbm.packet.SyncBindings.field(AssemblyMachineBlockEntity.class, "recipeModule", 3, 2);
        com.hbm.packet.SyncBindings.field(
                AssemblyMachineBlockEntity.class, "upgradeManager", 3, 0L);
        com.hbm.inventory.container.SyncedData.fields(
                AssemblyMachineBlockEntity.class, "maxPower", "power");
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
