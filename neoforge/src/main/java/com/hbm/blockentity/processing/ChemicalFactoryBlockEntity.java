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

package com.hbm.blockentity.processing;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.port.CapabilityPortProvider;
import com.hbm.capability.port.FluidPort;
import com.hbm.capability.port.ItemPort;
import com.hbm.inventory.container.ChemicalFactoryMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.items.machine.BlueprintsItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.modules.machine.ModuleMachineBase;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
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

public class ChemicalFactoryBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                FluidTankHandler,
                FluidSettingsHandler,
                UpgradeInfoProvider,
                MenuProvider,
                AudioLoop,
                SyncUnitSchema,
                RadioValueProvider,
                CapabilityPortProvider {

    public static final int MODULES = 4;
    public static final int SLOTS_PER_MODULE = 7;
    public static final int OUTPUTS_PER_MODULE = 3;
    public static final int SLOT_COUNT = 32;
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_UPGRADE_START = 1;
    public static final int SLOT_UPGRADE_END = 3;
    public static final int BLUEPRINT_OFFSET = 4;
    public static final int INPUT_OFFSET = 5;
    public static final int OUTPUT_OFFSET = 8;
    public static final int TANK_COUNT = 12;
    public static final long MAX_POWER = 1_000_000L;

    private static final int[] AUTOMATION_SLOTS = automationSlots();
    private static final String[] RADIO = {
        "VAL:progress1",
        "VAL:progress2",
        "VAL:progress3",
        "VAL:progress4",
        "VAL:recipe1",
        "VAL:recipe2",
        "VAL:recipe3",
        "VAL:recipe4",
        "VAL:anyactive",
        "VAL:active1",
        "VAL:active2",
        "VAL:active3",
        "VAL:active4"
    };

    public static Consumer<ChemicalFactoryBlockEntity> CLIENT_SOUND =
            _ -> {};

    public long power;
    public long maxPower;

    {
        hbm$syncSet$ChemicalFactoryBlockEntity$maxPower(MAX_POWER);
    }

    public final boolean[] isProgressing = new boolean[MODULES];
    public final ModuleMachineBase[] module = new ModuleMachineBase[MODULES];
    public final FluidTankNTM[] inputTanks = new FluidTankNTM[TANK_COUNT];
    public final FluidTankNTM[] outputTanks = new FluidTankNTM[TANK_COUNT];
    private final FluidTankNTM[] coolantTanks = {
        new FluidTankNTM(NTMFluids.WATER, 4_000), new FluidTankNTM(NTMFluids.SPENTSTEAM, 4_000)
    };
    public final UpgradeManager upgradeManager = new UpgradeManager(this);

    public int anim;
    public int prevAnim;
    public boolean frame;
    private final ItemPort[] itemPorts = new ItemPort[MODULES];
    private final FluidPort coolantPort;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FlushFaces recipeFaces =
            FlushFaces.activePlane((cell, _) -> !isCoolantCell(cell));
    private final FlushFaces coolantFaces =
            FlushFaces.activePlane((cell, _) -> isCoolantCell(cell));

    public ChemicalFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEMICALFACTORY.get(), pos, state, SLOT_COUNT);
        for (int m = 0; m < MODULES; m++) {
            int base = BLUEPRINT_OFFSET + m * SLOTS_PER_MODULE;
            var inputs = new FluidTankNTM[3];
            var outputs = new FluidTankNTM[3];
            for (int j = 0; j < 3; j++) {
                int tankIndex = m * 3 + j;
                inputs[j] = new FluidTankNTM(24_000);
                outputs[j] = new FluidTankNTM(24_000);
                inputs[j].bindOwner(this);
                outputs[j].bindOwner(this);
                hbm$syncArraySet$ChemicalFactoryBlockEntity$inputTanks(
                        inputTanks, tankIndex, inputs[j]);
                hbm$syncArraySet$ChemicalFactoryBlockEntity$outputTanks(
                        outputTanks, tankIndex, outputs[j]);
            }
            hbm$syncArraySet$ChemicalFactoryBlockEntity$module(
                    module,
                    m,
                    new ModuleMachineBase(
                            m,
                            this,
                            ChemicalPlantRecipes.INSTANCE,
                            this,
                            new int[] {base + 1, base + 2, base + 3},
                            new int[] {base + 4, base + 5, base + 6},
                            inputs,
                            outputs));
            itemPorts[m] = createItemPort(m);
        }
        for (var tank : coolantTanks) tank.bindOwner(this);
        coolantPort = FluidPort.of(coolantTanks, 1, 2, this::setChanged);
    }

    private static int[] automationSlots() {
        var slots = new int[MODULES * 6];
        for (int m = 0; m < MODULES; m++) {
            for (int j = 0; j < 6; j++) slots[m * 6 + j] = INPUT_OFFSET + m * SLOTS_PER_MODULE + j;
        }
        return slots;
    }

    private ItemPort createItemPort(int index) {
        var slots = new int[3 + TANK_COUNT];
        int firstInput = INPUT_OFFSET + index * SLOTS_PER_MODULE;
        for (int j = 0; j < 3; j++) slots[j] = firstInput + j;
        for (int m = 0; m < MODULES; m++) {
            for (int j = 0; j < 3; j++)
                slots[3 + m * 3 + j] = OUTPUT_OFFSET + m * SLOTS_PER_MODULE + j;
        }
        return new ItemPort(
                slots,
                (slot, stack) ->
                        slot >= firstInput
                                && slot < firstInput + 3
                                && module[index].isItemValid(slot, stack),
                (slot, _) ->
                        outputSlot(slot) || module[index].isSlotClogged(slot));
    }

    private static boolean outputSlot(int slot) {
        return slot >= OUTPUT_OFFSET
                && slot < SLOT_COUNT
                && (slot - BLUEPRINT_OFFSET) % SLOTS_PER_MODULE >= 4;
    }

    @Override
    public void tickServer() {
        long selectedPower = 0L;
        for (var engine : module) {
            var recipe = engine.getRecipe();
            if (recipe != null) selectedPower += recipe.power;
        }
        long ceiling = Math.max(power, Math.max(MAX_POWER, selectedPower * 100L));
        if (maxPower != ceiling) {
            hbm$syncSet$ChemicalFactoryBlockEntity$maxPower(ceiling);
            setChanged();
        }
        setPower(power + ItemEnergyTransfer.extract(this, SLOT_BATTERY, maxPower - power, false));
        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        double speed = 2D * SelectedProcessorSupport.speed(upgradeManager);
        double consumption = 2D * SelectedProcessorSupport.power(upgradeManager);
        flush.provide((ServerLevel) level, this);
        boolean changed = false;
        for (int m = 0; m < MODULES; m++) {
            var engine = module[m];
            engine.update(
                    speed,
                    consumption,
                    canCool(),
                    getItem(BLUEPRINT_OFFSET + m * SLOTS_PER_MODULE));
            hbm$syncArraySet$ChemicalFactoryBlockEntity$isProgressing(
                    isProgressing, m, engine.didProcess);
            if (engine.didProcess) {
                water().setFill(water().getFill() - 100);
                lps().setFill(lps().getFill() + 100);
            }
            changed |= engine.didProcess || engine.markDirty;
        }
        if (changed) setChanged();
        exchangeFluids();
        networkPackNT(100);
    }

    private void exchangeFluids() {
        boolean changed = false;
        for (var destination : inputTanks) {
            if (destination.getDeclaredFluid() == null
                    || destination.getFill() == destination.getMaxFill()) continue;
            for (var source : outputTanks) {
                if (source.getFill() == 0 || source.getPressure() != destination.getPressure())
                    continue;
                var fluid = source.getFluid();
                var components = source.getComponents();
                if (!destination.accepts(fluid, components)) continue;
                int moved = destination.receive(fluid, components, Math.min(50, source.getFill()));
                source.setFill(source.getFill() - moved);
                changed |= moved != 0;
                if (destination.getFill() == destination.getMaxFill()) break;
            }
        }
        if (changed) setChanged();
    }

    public FluidTankNTM water() {
        return coolantTanks[0];
    }

    public FluidTankNTM lps() {
        return coolantTanks[1];
    }

    public boolean canCool() {
        return water().getFill() >= 100 && lps().getMaxFill() - lps().getFill() >= 100;
    }

    public boolean anyProcessing() {
        for (boolean active : isProgressing) if (active) return true;
        return false;
    }

    public boolean isProcessing(int index) {
        return isProgressing[index];
    }

    @Override
    public void tickClient() {
        if (TickPhase.every(this, 20)) frame = !level.getBlockState(worldPosition.above(3)).isAir();
        prevAnim = anim;
        if (anyProcessing()) anim++;
        tickSound();
    }

    public void tickSound() {
        CLIENT_SOUND.accept(this);
    }

    @Override
    public @Nullable SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.CHEMICAL_PLANT_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX() + 0.5F,
                worldPosition.getY() + 0.5F,
                worldPosition.getZ() + 0.5F,
                getVolume(1F),
                15F,
                1F,
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
        hbm$syncSet$ChemicalFactoryBlockEntity$power(next);
        setChanged();
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return inputTanks;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return outputTanks;
    }

    @Override
    public int tankCount() {
        return 2 * TANK_COUNT + coolantTanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        if (index < TANK_COUNT) return inputTanks[index];
        if (index < 2 * TANK_COUNT) return outputTanks[index - TANK_COUNT];
        return coolantTanks[index - 2 * TANK_COUNT];
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        for (var tank : inputTanks) visitor.accept(tank);
        for (var tank : outputTanks) visitor.accept(tank);
        for (var tank : coolantTanks) visitor.accept(tank);
    }

    private boolean isCoolantCell(BlockPos cell) {
        if (cell.getY() != worldPosition.getY()) return false;
        var facing = getBlockState().getValue(MultiblockCoreBlock.FACING);
        var clockwise = facing.getClockWise();
        int x = cell.getX() - worldPosition.getX();
        int z = cell.getZ() - worldPosition.getZ();
        int forward = x * facing.getStepX() + z * facing.getStepZ();
        int lateral = x * clockwise.getStepX() + z * clockwise.getStepZ();
        return Math.abs(forward) == 2 && Math.abs(lateral) == 1;
    }

    @Override
    public @Nullable FluidPort fluidAccess(BlockPos cell, Direction side) {
        return isCoolantCell(cell) ? coolantPort : null;
    }

    @Override
    public @Nullable ItemPort itemAccess(BlockPos cell, Direction side) {
        if (cell.getY() != worldPosition.getY()) return null;
        var facing = getBlockState().getValue(MultiblockCoreBlock.FACING);
        var clockwise = facing.getClockWise();
        int x = cell.getX() - worldPosition.getX();
        int z = cell.getZ() - worldPosition.getZ();
        int forward = x * facing.getStepX() + z * facing.getStepZ();
        int lateral = x * clockwise.getStepX() + z * clockwise.getStepZ();
        if (Math.abs(forward) != 1 || Math.abs(lateral) != 2) return null;
        return itemPorts[(lateral > 0 ? 0 : 2) + (forward > 0 ? 0 : 1)];
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (var tank : outputTanks) out.add(tank, recipeFaces);
        out.add(coolantPort, lps(), coolantFaces);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return true;
        if (slot >= SLOT_UPGRADE_START && slot <= SLOT_UPGRADE_END)
            return stack.getItem() instanceof MachineUpgradeItem;
        if (slot < BLUEPRINT_OFFSET || slot >= SLOT_COUNT) return true;
        int offset = (slot - BLUEPRINT_OFFSET) % SLOTS_PER_MODULE;
        if (offset == 0) return stack.getItem() instanceof BlueprintsItem;
        return offset <= 3
                && module[(slot - BLUEPRINT_OFFSET) / SLOTS_PER_MODULE].isItemValid(slot, stack);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot < INPUT_OFFSET || slot >= SLOT_COUNT) return false;
        int offset = (slot - BLUEPRINT_OFFSET) % SLOTS_PER_MODULE;
        return offset >= 1 && offset <= 3 && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (outputSlot(slot)) return true;
        if (slot < INPUT_OFFSET || slot >= SLOT_COUNT) return false;
        return module[(slot - BLUEPRINT_OFFSET) / SLOTS_PER_MODULE].isSlotClogged(slot);
    }

    public void selectRecipe(int index, String recipe) {
        if (SelectedProcessorSupport.select(module[index], recipe, false)) setChanged();
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
        int index =
                switch (key) {
                    case "VAL:progress1", "VAL:recipe1", "VAL:active1" -> 0;
                    case "VAL:progress2", "VAL:recipe2", "VAL:active2" -> 1;
                    case "VAL:progress3", "VAL:recipe3", "VAL:active3" -> 2;
                    case "VAL:progress4", "VAL:recipe4", "VAL:active4" -> 3;
                    default -> -1;
                };
        if (index < 0) return key.equals("VAL:anyactive") ? anyProcessing() ? "1" : "0" : null;
        return switch (key.charAt(4)) {
            case 'p' -> Long.toString(Math.round(module[index].progress * 100D));
            case 'r' -> module[index].legacyRecipeName();
            default -> isProgressing[index] ? "1" : "0";
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineChemicalFactory");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ChemicalFactoryMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        for (var engine : module) engine.save(output);
        for (int m = 0; m < MODULES; m++) {
            for (int j = 0; j < 3; j++) {
                var suffix = Integer.toString(m) + (char) ('a' + j);
                inputTanks[m * 3 + j].serialize(output.child("i" + suffix));
                outputTanks[m * 3 + j].serialize(output.child("o" + suffix));
            }
        }
        water().serialize(output.child("w"));
        lps().serialize(output.child("s"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$ChemicalFactoryBlockEntity$power(input.getLongOr("power", 0L));
        hbm$syncSet$ChemicalFactoryBlockEntity$maxPower(input.getLongOr("maxPower", MAX_POWER));
        for (var engine : module) engine.load(input);
        for (int m = 0; m < MODULES; m++) {
            for (int j = 0; j < 3; j++) {
                var suffix = Integer.toString(m) + (char) ('a' + j);
                input.child("i" + suffix).ifPresent(inputTanks[m * 3 + j]::deserialize);
                input.child("o" + suffix).ifPresent(outputTanks[m * 3 + j]::deserialize);
            }
        }
        input.child("w").ifPresent(water()::deserialize);
        input.child("s").ifPresent(lps()::deserialize);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 127L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeLong(maxPower);
            case 2 -> {
                for (boolean active : isProgressing) output.writeBoolean(active);
            }
            case 3 -> {
                for (var engine : module) engine.serialize(output);
            }
            case 4 -> {
                for (var tank : inputTanks) tank.packetSerialize(output);
            }
            case 5 -> {
                for (var tank : outputTanks) tank.packetSerialize(output);
            }
            case 6 -> {
                water().packetSerialize(output);
                lps().packetSerialize(output);
            }
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$ChemicalFactoryBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$ChemicalFactoryBlockEntity$maxPower(input.readLong());
            case 2 -> {
                for (int i = 0; i < MODULES; i++)
                    hbm$syncArraySet$ChemicalFactoryBlockEntity$isProgressing(
                            isProgressing, i, input.readBoolean());
            }
            case 3 -> {
                for (var engine : module) engine.deserialize(input);
            }
            case 4 -> {
                for (var tank : inputTanks) tank.packetDeserialize(input);
            }
            case 5 -> {
                for (var tank : outputTanks) tank.packetDeserialize(input);
            }
            case 6 -> {
                water().packetDeserialize(input);
                lps().packetDeserialize(input);
            }
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$ChemicalFactoryBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$ChemicalFactoryBlockEntity$maxPower(long next) {
        if (this.maxPower != next) {
            this.maxPower = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final boolean hbm$syncArraySet$ChemicalFactoryBlockEntity$isProgressing(
            boolean[] array, int index, boolean next) {
        if (array[index] != next) {
            array[index] = next;
            if (array == this.isProgressing) {
                syncArrayChanged(array, index, 3, 4);
            }
        }
        return next;
    }

    public final ModuleMachineBase hbm$syncArraySet$ChemicalFactoryBlockEntity$module(
            ModuleMachineBase[] array, int index, ModuleMachineBase next) {
        if (array[index] != next) {
            if (array == this.module && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.module) {
                syncArrayChanged(array, index, 3, 8);
            }
        }
        return next;
    }

    public final FluidTankNTM hbm$syncArraySet$ChemicalFactoryBlockEntity$inputTanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.inputTanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.inputTanks) {
                syncArrayChanged(array, index, 3, 16);
            }
        }
        return next;
    }

    public final FluidTankNTM hbm$syncArraySet$ChemicalFactoryBlockEntity$outputTanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.outputTanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.outputTanks) {
                syncArrayChanged(array, index, 3, 32);
            }
        }
        return next;
    }

    public final FluidTankNTM hbm$syncArraySet$ChemicalFactoryBlockEntity$coolantTanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.coolantTanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.coolantTanks) {
                syncArrayChanged(array, index, 3, 64);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(ChemicalFactoryBlockEntity.class, "isProgressing", 3, 4);
        com.hbm.packet.SyncBindings.field(ChemicalFactoryBlockEntity.class, "module", 3, 8);
        com.hbm.packet.SyncBindings.field(ChemicalFactoryBlockEntity.class, "inputTanks", 3, 16);
        com.hbm.packet.SyncBindings.field(ChemicalFactoryBlockEntity.class, "outputTanks", 3, 32);
        com.hbm.packet.SyncBindings.field(ChemicalFactoryBlockEntity.class, "coolantTanks", 3, 64);
        com.hbm.packet.SyncBindings.field(
                ChemicalFactoryBlockEntity.class, "upgradeManager", 3, 0L);
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
