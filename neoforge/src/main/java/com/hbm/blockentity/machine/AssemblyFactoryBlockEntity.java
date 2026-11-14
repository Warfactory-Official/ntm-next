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
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blockentity.machine.JointedArm;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.port.CapabilityPortProvider;
import com.hbm.capability.port.FluidPort;
import com.hbm.capability.port.ItemPort;
import com.hbm.inventory.container.AssemblyFactoryMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
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
import java.util.Random;
import java.util.function.Consumer;
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

import static com.hbm.api.redstoneoverradio.RadioCommandParser.VAL;

public class AssemblyFactoryBlockEntity extends MachineBlockEntity
        implements SoundEmitter,
                AudioLoop,
                EnergyHandler,
                FluidTankHandler,
                MenuProvider,
                UpgradeInfoProvider,
                CapabilityPortProvider,
                RadioValueProvider,
                SyncUnitSchema {

    public static final String[] ROR =
            new String[] {
                VAL + "progress1",
                VAL + "progress2",
                VAL + "progress3",
                VAL + "progress4",
                VAL + "recipe1",
                VAL + "recipe2",
                VAL + "recipe3",
                VAL + "recipe4",
                VAL + "anyactive",
                VAL + "active1",
                VAL + "active2",
                VAL + "active3",
                VAL + "active4",
            };
    public static final int MODULES = 4;
    public static final int SLOTS_PER_MODULE = 14;

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_UPGRADE_START = 1;
    public static final int SLOT_UPGRADE_END = 3;
    public static final int SLOT_COUNT = 4 + MODULES * SLOTS_PER_MODULE;

    public static final int TEMPLATE_OFFSET = 4;
    public static final int INPUT_OFFSET = 5;
    public static final int INPUT_COUNT = 12;
    public static final int OUTPUT_OFFSET = 17;
    public static final int OUTPUTS_PER_MODULE = 1;

    public static final long MAX_POWER = 1_000_000L;
    public static final int TANK_CAPACITY = 4_000;
    public static final int COOLANT_CAPACITY = 4_000;
    private static final UpgradeLimits VALID_UPGRADES =
            UpgradeLimits.of(UpgradeType.SPEED, 3)
                    .and(UpgradeType.POWER, 3)
                    .and(UpgradeType.OVERDRIVE, 3);
    private static final int OFF_RING = Integer.MIN_VALUE;
    private static final int BIAS = 32;
    private static final int[] ACCESSIBLE_SLOTS = buildAccessibleSlots();
    public static Consumer<AssemblyFactoryBlockEntity> CLIENT_SOUND = be -> {};
    public static Consumer<AssemblyFactoryBlockEntity> CLIENT_SOUND_START = be -> {};
    public static Consumer<AssemblyFactoryBlockEntity> CLIENT_SOUND_STRIKE = be -> {};
    public static Consumer<AssemblyFactoryBlockEntity> CLIENT_SOUND_CUT = be -> {};
    public final FluidTankNTM[] inputTanks = new FluidTankNTM[MODULES];
    public final FluidTankNTM[] outputTanks = new FluidTankNTM[MODULES];
    public final FluidTankNTM[] coolant = {
        new FluidTankNTM(NTMFluids.WATER, COOLANT_CAPACITY),
        new FluidTankNTM(NTMFluids.SPENTSTEAM, COOLANT_CAPACITY),
    };
    public final boolean[] didProcess = new boolean[MODULES];
    public final Carriage[] carriages = {new Carriage(0), new Carriage(1)};
    private final FluidTankNTM[][] moduleIn = new FluidTankNTM[MODULES][];
    private final FluidTankNTM[][] moduleOut = new FluidTankNTM[MODULES][];
    public final ModuleMachineAssembler[] module = new ModuleMachineAssembler[MODULES];
    private final FluidPort coolantPort = FluidPort.of(coolant, 1, 1 << 1, this::markChanged);
    private final ItemPort[] ioPorts = new ItemPort[MODULES];
    private final UpgradeManager upgradeManager = new UpgradeManager(this);
    public long power;
    public long maxPower;

    {
        hbm$syncSet$AssemblyFactoryBlockEntity$maxPower(MAX_POWER);
    }

    public boolean frame;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private boolean dirtyThisTick;

    public AssemblyFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASSEMBLYFACTORY.get(), pos, state, SLOT_COUNT);
        for (int i = 0; i < MODULES; i++) {
            hbm$syncArraySet$AssemblyFactoryBlockEntity$inputTanks(
                    inputTanks, i, new FluidTankNTM(TANK_CAPACITY));
            hbm$syncArraySet$AssemblyFactoryBlockEntity$outputTanks(
                    outputTanks, i, new FluidTankNTM(TANK_CAPACITY));
            moduleIn[i] = new FluidTankNTM[] {inputTanks[i]};
            moduleOut[i] = new FluidTankNTM[] {outputTanks[i]};
            int base = i * SLOTS_PER_MODULE;
            int[] in = new int[INPUT_COUNT];
            for (int k = 0; k < INPUT_COUNT; k++) in[k] = base + INPUT_OFFSET + k;
            int[] out = {base + OUTPUT_OFFSET};
            hbm$syncArraySet$AssemblyFactoryBlockEntity$module(
                    module,
                    i,
                    new ModuleMachineAssembler(
                            i,
                            this,
                            AssemblyMachineRecipes.INSTANCE,
                            this,
                            in,
                            out,
                            moduleIn[i],
                            moduleOut[i]));

            int[] reach = new int[INPUT_COUNT + MODULES];
            System.arraycopy(in, 0, reach, 0, INPUT_COUNT);
            for (int m = 0; m < MODULES; m++)
                reach[INPUT_COUNT + m] = m * SLOTS_PER_MODULE + OUTPUT_OFFSET;
            ioPorts[i] = new ItemPort(reach, this::canInsertSlot, this::canExtractSlot);
        }
    }

    private static int forward(int local) {
        return (local >> 8) - BIAS;
    }

    private static int lateral(int local) {
        return (local & 0xFF) - BIAS;
    }

    private static boolean isCoolantCell(int local) {
        return local != OFF_RING && Math.abs(forward(local)) == 2 && Math.abs(lateral(local)) == 1;
    }

    private static boolean isIoCell(int local) {
        return local != OFF_RING && Math.abs(forward(local)) == 1 && Math.abs(lateral(local)) == 2;
    }

    private static int ioModule(int local) {
        return (lateral(local) > 0 ? 0 : 2) + (forward(local) > 0 ? 0 : 1);
    }

    private static int[] buildAccessibleSlots() {
        int[] slots = new int[MODULES * (INPUT_COUNT + 1)];
        int n = 0;
        for (int i = 0; i < MODULES; i++) {
            int base = i * SLOTS_PER_MODULE;
            for (int k = 0; k < INPUT_COUNT; k++) slots[n++] = base + INPUT_OFFSET + k;
            slots[n++] = base + OUTPUT_OFFSET;
        }
        return slots;
    }

    @Override
    public void tickServer() {
        long prevPower = power;
        long prevMaxPower = maxPower;
        boolean changed = false;
        dirtyThisTick = false;

        long nextMaxPower = 0;
        for (int i = 0; i < MODULES; i++) {
            GenericRecipe rec = module[i].getRecipe();
            if (rec != null) nextMaxPower += rec.power * 100L;
        }
        hbm$syncSet$AssemblyFactoryBlockEntity$maxPower(
                Math.max(power, Math.max(nextMaxPower, MAX_POWER)));

        hbm$syncSet$AssemblyFactoryBlockEntity$power(
                (long)
                        (power
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, maxPower - power, false))));
        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);

        flush.provide((ServerLevel) level, this);

        int speedLevel = Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
        int powerLevel = Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3);
        int overdriveLevel = Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);

        double speed = 1D + speedLevel / 3D + overdriveLevel;
        double pow = 1D - powerLevel * 0.25D + speedLevel + overdriveLevel * 10D / 3D;

        boolean cool = canCool();
        for (int i = 0; i < MODULES; i++) {
            double prevProgress = module[i].progress;
            module[i].update(speed * 2D, pow * 2D, cool, inventory.get(4 + i * SLOTS_PER_MODULE));
            hbm$syncArraySet$AssemblyFactoryBlockEntity$didProcess(
                    didProcess, i, module[i].didProcess);
            dirtyThisTick |= module[i].markDirty;
            changed |= module[i].progress != prevProgress;
            if (didProcess[i]) {
                water().setFill(water().getFill() - 100);
                lps().setFill(lps().getFill() + 100);
                changed = true;
            }
        }

        if (dirtyThisTick || power != prevPower) setChanged();
        else if (changed || maxPower != prevMaxPower) markChanged();

        networkPackNT(100);
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (FluidTankNTM tank : outputTanks) {
            out.add(tank, FlushFaces.activePlane((cell, side) -> !isCoolantCell(localCell(cell))));
        }
        out.add(
                coolantPort,
                lps(),
                FlushFaces.activePlane((cell, side) -> isCoolantCell(localCell(cell))));
    }

    public FluidTankNTM water() {
        return coolant[0];
    }

    public FluidTankNTM lps() {
        return coolant[1];
    }

    public boolean canCool() {
        return water().getFill() >= 100 && lps().getFill() <= lps().getMaxFill() - 100;
    }

    public boolean anyProcessing() {
        return didProcess[0] || didProcess[1] || didProcess[2] || didProcess[3];
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);
        for (var carriage : carriages) carriage.update();

        if (TickPhase.every(this, 20)) {
            frame = !level.getBlockState(worldPosition.above(3)).isAir();
        }
    }

    @Override
    public SoundHandle createAudioLoop() {
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
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        hbm$syncSet$AssemblyFactoryBlockEntity$power(Math.max(0L, Math.min(p, maxPower)));
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int level, List<Component> tooltip) {
        UpgradeInfo.processor(type, level, tooltip);
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return VALID_UPGRADES;
    }

    @Override
    public int tankCount() {
        return inputTanks.length + outputTanks.length + coolant.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        if (index < inputTanks.length) return inputTanks[index];
        index -= inputTanks.length;
        if (index < outputTanks.length) return outputTanks[index];
        index -= outputTanks.length;
        return coolant[index];
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        FluidTankHandler.super.forEachTank(visitor);
        for (FluidTankNTM tank : coolant) visitor.accept(tank);
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
    public @Nullable FluidPort fluidAccess(BlockPos cell, Direction side) {
        int local = localCell(cell);
        return isCoolantCell(local) ? coolantPort : null;
    }

    @Override
    public @Nullable ItemPort itemAccess(BlockPos cell, Direction side) {
        int local = localCell(cell);
        return isIoCell(local) ? ioPorts[ioModule(local)] : null;
    }

    private int localCell(BlockPos cell) {
        if (cell.getY() != worldPosition.getY()) return OFF_RING;
        Direction dir = getBlockState().getValue(MultiblockCoreBlock.FACING);
        Direction rot = dir.getClockWise();
        int dx = cell.getX() - worldPosition.getX();
        int dz = cell.getZ() - worldPosition.getZ();
        int forward = dx * dir.getStepX() + dz * dir.getStepZ();
        int lateral = dx * rot.getStepX() + dz * rot.getStepZ();
        if (Math.abs(forward) > BIAS || Math.abs(lateral) > BIAS) return OFF_RING;
        return ((forward + BIAS) << 8) | (lateral + BIAS);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return true;
        if (slot >= SLOT_UPGRADE_START && slot <= SLOT_UPGRADE_END)
            return stack.getItem() instanceof MachineUpgradeItem;
        if (slot < TEMPLATE_OFFSET || slot >= SLOT_COUNT) return false;
        var index = moduleIndex(slot);
        var local = slot - index * SLOTS_PER_MODULE;
        if (local == TEMPLATE_OFFSET) return stack.getItem() instanceof BlueprintsItem;
        return local < OUTPUT_OFFSET && module[index].isItemValid(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canInsertSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canExtractSlot(slot, stack);
    }

    private boolean canInsertSlot(int slot, ItemStack stack) {
        if (slot < INPUT_OFFSET || slot >= SLOT_COUNT) return false;
        var index = moduleIndex(slot);
        var local = slot - index * SLOTS_PER_MODULE;
        return local >= INPUT_OFFSET
                && local < OUTPUT_OFFSET
                && module[index].isItemValid(slot, stack);
    }

    private boolean canExtractSlot(int slot, ItemStack stack) {
        if (slot < INPUT_OFFSET || slot >= SLOT_COUNT) return false;
        var index = moduleIndex(slot);
        return slot - index * SLOTS_PER_MODULE == OUTPUT_OFFSET
                || module[index].isSlotClogged(slot);
    }

    private static int moduleIndex(int slot) {
        return (slot - TEMPLATE_OFFSET) / SLOTS_PER_MODULE;
    }

    public void selectRecipe(int index, String recipe) {
        if (index < 0 || index >= MODULES) return;
        module[index].setRecipe(recipe);
        setChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String name) {
        if ((VAL + "anyactive").equals(name))
            return ""
                    + ((didProcess[0] || didProcess[1] || didProcess[2] || didProcess[3]) ? 1 : 0);
        for (int i = 0; i < MODULES; i++) {
            if ((VAL + "progress" + (i + 1)).equals(name))
                return "" + (int) Math.round(module[i].progress * 100);
            if ((VAL + "recipe" + (i + 1)).equals(name)) return module[i].legacyRecipeName();
            if ((VAL + "active" + (i + 1)).equals(name)) return "" + (didProcess[i] ? 1 : 0);
        }
        return null;
    }

    private void writeDidProcess(ByteBuf output) {
        for (boolean value : didProcess) output.writeBoolean(value);
    }

    private void readDidProcess(ByteBuf input) {
        for (int i = 0; i < didProcess.length; i++)
            hbm$syncArraySet$AssemblyFactoryBlockEntity$didProcess(
                    didProcess, i, input.readBoolean());
    }

    private void writeModules(ByteBuf output) {
        for (ModuleMachineAssembler value : module) value.serialize(output);
    }

    private void readModules(ByteBuf input) {
        for (ModuleMachineAssembler value : module) value.deserialize(input);
    }

    private void writeInputTanks(ByteBuf output) {
        for (FluidTankNTM tank : inputTanks) tank.packetSerialize(output);
    }

    private void readInputTanks(ByteBuf input) {
        for (FluidTankNTM tank : inputTanks) tank.packetDeserialize(input);
    }

    private void writeOutputTanks(ByteBuf output) {
        for (FluidTankNTM tank : outputTanks) tank.packetSerialize(output);
    }

    private void readOutputTanks(ByteBuf input) {
        for (FluidTankNTM tank : outputTanks) tank.packetDeserialize(input);
    }

    private void writeCoolant(ByteBuf output) {
        water().packetSerialize(output);
        lps().packetSerialize(output);
    }

    private void readCoolant(ByteBuf input) {
        water().packetDeserialize(input);
        lps().packetDeserialize(input);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> hbm$syncSet$AssemblyFactoryBlockEntity$power(v));
        input.getLong("maxPower")
                .ifPresent(v -> hbm$syncSet$AssemblyFactoryBlockEntity$maxPower(v));
        for (int i = 0; i < MODULES; i++) {
            module[i].load(input);
            input.child("i" + i).ifPresent(inputTanks[i]::deserialize);
            input.child("o" + i).ifPresent(outputTanks[i]::deserialize);
        }
        input.child("w").ifPresent(coolant[0]::deserialize);
        input.child("s").ifPresent(coolant[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        for (int i = 0; i < MODULES; i++) {
            module[i].save(output);
            inputTanks[i].serialize(output.child("i" + i));
            outputTanks[i].serialize(output.child("o" + i));
        }
        water().serialize(output.child("w"));
        lps().serialize(output.child("s"));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineAssemblyFactory");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new AssemblyFactoryMenu(containerId, playerInventory, this);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x7fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeLong(maxPower);
            case 2 -> writeDidProcess(output);
            case 3 -> writeModules(output);
            case 4 -> writeInputTanks(output);
            case 5 -> writeOutputTanks(output);
            case 6 -> writeCoolant(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$AssemblyFactoryBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$AssemblyFactoryBlockEntity$maxPower(input.readLong());
            case 2 -> readDidProcess(input);
            case 3 -> readModules(input);
            case 4 -> readInputTanks(input);
            case 5 -> readOutputTanks(input);
            case 6 -> readCoolant(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public boolean tickSound() {
        return anyProcessing();
    }

    private enum Travel {
        WORKING,
        RETIRING,
        SLIDING
    }

    private enum Stage {
        REPOSITION,
        EXTEND,
        CUT,
        RETRACT,
        RETIRE,
        WAIT
    }

    public final class Carriage {

        public final CarriageArm striker;
        public final CarriageArm saw;
        private final Random rand = new Random();
        public double slider;
        public double prevSlider;
        private Travel travel = Travel.WORKING;
        private boolean outbound;
        private int dwell;

        private Carriage(int group) {
            striker = new CarriageArm(group == 0 ? 0 : 3, false);
            saw = new CarriageArm(group == 0 ? 1 : 2, true);
            dwell = nextDwell();
        }

        private int nextDwell() {
            return 140 + rand.nextInt(161);
        }

        private void update() {
            prevSlider = slider;
            if (didProcess[striker.pedestal] || didProcess[saw.pedestal]) {
                switch (travel) {
                    case WORKING -> {
                        if (--dwell <= 0) travel = Travel.RETIRING;
                    }
                    case RETIRING -> {
                        if (striker.stage == Stage.WAIT && saw.stage == Stage.WAIT) depart();
                    }
                    case SLIDING -> slide();
                }
            }
            striker.tick();
            saw.tick();
        }

        private void depart() {
            travel = Travel.SLIDING;
            outbound = !outbound;
            if (!isMuffled()) CLIENT_SOUND_START.accept(AssemblyFactoryBlockEntity.this);
        }

        private void slide() {
            double end = outbound ? 1D : 0D;
            slider = outbound ? Math.min(slider + 0.1D, end) : Math.max(slider - 0.1D, end);
            if (slider == end) {
                travel = Travel.WORKING;
                dwell = nextDwell();
            }
        }

        public double getSlider(float interp) {
            return Mth.lerp(interp, prevSlider, slider);
        }

        public final class CarriageArm extends JointedArm {

            private static final int BLADE = STRIKER + 1;
            private static final double[][] STRIKER_POSES = {
                {10, 10, -10},
                {15, 15, -15},
                {25, 10, -15},
                {30, 0, -10},
                {-10, 10, 0},
                {-20, 30, -15},
            };
            private static final double[][] SAW_POSES = {
                {-15, 15, -10},
                {-15, 15, -15},
                {-15, 15, 10},
                {-15, 15, 15},
                {-15, 15, 2},
                {-15, 15, -2},
            };
            private final int pedestal;
            private final boolean isSaw;
            private final double[][] poses;
            private final double plunge;
            private final double depth;
            private Stage stage = Stage.REPOSITION;
            private int hold;

            private CarriageArm(int pedestal, boolean isSaw) {
                super(BLADE + 1);
                this.pedestal = pedestal;
                this.isSaw = isSaw;
                poses = isSaw ? SAW_POSES : STRIKER_POSES;
                plunge = isSaw ? 0.125D : 0.5D;
                depth = isSaw ? -0.375D : -0.75D;
                aimAt(poses, rand);
            }

            private void tick() {
                snapshot();
                if (!didProcess[slider > 0.5 ? pedestal ^ 1 : pedestal]) stage = Stage.RETIRE;
                if (stage == Stage.EXTEND || stage == Stage.CUT) angles[BLADE] += 45D;

                if (hold > 0) {
                    hold--;
                } else if (stage == Stage.WAIT) {
                    if (travel == Travel.WORKING) stage = Stage.REPOSITION;
                } else {
                    if (stage == Stage.RETIRE) home();
                    if (settle(
                            15, stage == Stage.CUT ? Math.abs(targets[PISTON] / 20D) : 15, plunge))
                        advance();
                }
            }

            private void advance() {
                switch (stage) {
                    case REPOSITION -> {
                        hold = 2;
                        targets[STRIKER] = depth;
                        stage = Stage.EXTEND;
                    }
                    case EXTEND -> {
                        if (isSaw) {
                            targets[PISTON] = -targets[PISTON];
                            stage = Stage.CUT;
                            if (!isMuffled())
                                CLIENT_SOUND_CUT.accept(AssemblyFactoryBlockEntity.this);
                        } else {
                            retract();
                            if (!isMuffled())
                                CLIENT_SOUND_STRIKE.accept(AssemblyFactoryBlockEntity.this);
                        }
                    }
                    case CUT -> retract();
                    case RETRACT ->
                            rest(travel == Travel.RETIRING ? Stage.RETIRE : Stage.REPOSITION);
                    case RETIRE -> rest(Stage.WAIT);
                }
            }

            private void retract() {
                targets[STRIKER] = 0D;
                stage = Stage.RETRACT;
            }

            private void rest(Stage next) {
                hold = 2 + rand.nextInt(5);
                aimAt(poses, rand);
                stage = next;
            }
        }
    }

    public final FluidTankNTM hbm$syncArraySet$AssemblyFactoryBlockEntity$inputTanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.inputTanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.inputTanks) {
                syncArrayChanged(array, index, 3, 1L << 4);
            }
        }
        return next;
    }

    public final FluidTankNTM hbm$syncArraySet$AssemblyFactoryBlockEntity$outputTanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.outputTanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.outputTanks) {
                syncArrayChanged(array, index, 3, 1L << 5);
            }
        }
        return next;
    }

    public final FluidTankNTM hbm$syncArraySet$AssemblyFactoryBlockEntity$coolant(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.coolant && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.coolant) {
                syncArrayChanged(array, index, 3, 1L << 6);
            }
        }
        return next;
    }

    public final boolean hbm$syncArraySet$AssemblyFactoryBlockEntity$didProcess(
            boolean[] array, int index, boolean next) {
        if (array[index] != next) {
            array[index] = next;
            if (array == this.didProcess) {
                syncArrayChanged(array, index, 3, 1L << 2);
            }
        }
        return next;
    }

    public final ModuleMachineAssembler hbm$syncArraySet$AssemblyFactoryBlockEntity$module(
            ModuleMachineAssembler[] array, int index, ModuleMachineAssembler next) {
        if (array[index] != next) {
            if (array == this.module && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.module) {
                syncArrayChanged(array, index, 3, 1L << 3);
            }
        }
        return next;
    }

    public final void hbm$syncSet$AssemblyFactoryBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$AssemblyFactoryBlockEntity$maxPower(long next) {
        if (this.maxPower != next) {
            this.maxPower = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(
                AssemblyFactoryBlockEntity.class, "inputTanks", 3, 1L << 4);
        com.hbm.packet.SyncBindings.field(
                AssemblyFactoryBlockEntity.class, "outputTanks", 3, 1L << 5);
        com.hbm.packet.SyncBindings.field(AssemblyFactoryBlockEntity.class, "coolant", 3, 1L << 6);
        com.hbm.packet.SyncBindings.field(
                AssemblyFactoryBlockEntity.class, "didProcess", 3, 1L << 2);
        com.hbm.packet.SyncBindings.field(AssemblyFactoryBlockEntity.class, "module", 3, 1L << 3);
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
