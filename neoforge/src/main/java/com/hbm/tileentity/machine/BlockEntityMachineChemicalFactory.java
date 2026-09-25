// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.port.FluidPort;
import com.hbm.capability.port.IPortHost;
import com.hbm.capability.port.ItemPort;
import com.hbm.inventory.container.MenuMachineChemicalFactory;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.modules.machine.ModuleMachineBase;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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

public class BlockEntityMachineChemicalFactory extends BlockEntityMachineBase
        implements AudioLoop,
                IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                IUpgradeInfoProvider,
                IControlReceiver,
                IPortHost,
                IRORValueProvider,
                SyncUnitSchema {

    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "progress1",
                PREFIX_VALUE + "progress2",
                PREFIX_VALUE + "progress3",
                PREFIX_VALUE + "progress4",
                PREFIX_VALUE + "recipe1",
                PREFIX_VALUE + "recipe2",
                PREFIX_VALUE + "recipe3",
                PREFIX_VALUE + "recipe4",
                PREFIX_VALUE + "anyactive",
                PREFIX_VALUE + "active1",
                PREFIX_VALUE + "active2",
                PREFIX_VALUE + "active3",
                PREFIX_VALUE + "active4",
            };
    public static final int MODULES = 4;
    public static final int SLOTS_PER_MODULE = 7;

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_UPGRADE_START = 1;
    public static final int SLOT_UPGRADE_END = 3;
    public static final int SLOT_COUNT = 32;
    public static final int OUTPUT_OFFSET = 8;
    public static final int OUTPUTS_PER_MODULE = 3;

    public static final long MAX_POWER = 1_000_000L;
    public static final int TANK_COUNT = 3;
    public static final int TANK_CAPACITY = 24_000;
    public static final int COOLANT_CAPACITY = 4_000;
    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(
                    UpgradeType.SPEED, 3, UpgradeType.POWER, 3, UpgradeType.OVERDRIVE, 3);

    private static final int OFF_RING = Integer.MIN_VALUE;
    private static final int BIAS = 32;
    private static final int[] ACCESSIBLE_SLOTS = {
        5, 6, 7, 8, 9, 10,
        12, 13, 14, 15, 16, 17,
        19, 20, 21, 22, 23, 24,
        26, 27, 28, 29, 30, 31
    };

    public static Consumer<BlockEntityMachineChemicalFactory> CLIENT_SOUND = be -> {};

    @SyncField(units = 1L << 4)
    public final FluidTankNTM[] inputTanks = new FluidTankNTM[MODULES * TANK_COUNT];

    @SyncField(units = 1L << 5)
    public final FluidTankNTM[] outputTanks = new FluidTankNTM[MODULES * TANK_COUNT];

    @SyncField(units = 1L << 6)
    public final FluidTankNTM[] coolant = {
        new FluidTankNTM(NTMFluids.WATER, COOLANT_CAPACITY),
        new FluidTankNTM(NTMFluids.SPENTSTEAM, COOLANT_CAPACITY),
    };

    @SyncField(units = 1L << 2)
    public final boolean[] didProcess = new boolean[MODULES];

    private final FluidTankNTM[][] moduleIn = new FluidTankNTM[MODULES][];
    private final FluidTankNTM[][] moduleOut = new FluidTankNTM[MODULES][];

    @SyncField(units = 1L << 3)
    public final ModuleMachineBase[] module = new ModuleMachineBase[MODULES];

    private final FluidPort coolantPort = FluidPort.of(coolant, new int[] {0}, new int[] {1});
    private final ItemPort[] ioPorts = new ItemPort[MODULES];
    private final UpgradeManager upgradeManager = new UpgradeManager(this);

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 1)
    public long maxPower = MAX_POWER;

    public int anim;
    public int prevAnim;
    public boolean frame;
    private boolean dirtyThisTick;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityMachineChemicalFactory(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEMICALFACTORY.get(), pos, state, SLOT_COUNT);
        for (int i = 0; i < inputTanks.length; i++) {
            inputTanks[i] = new FluidTankNTM(TANK_CAPACITY);
            outputTanks[i] = new FluidTankNTM(TANK_CAPACITY);
        }
        for (int i = 0; i < MODULES; i++) {
            moduleIn[i] =
                    new FluidTankNTM[] {
                        inputTanks[i * 3], inputTanks[i * 3 + 1], inputTanks[i * 3 + 2]
                    };
            moduleOut[i] =
                    new FluidTankNTM[] {
                        outputTanks[i * 3], outputTanks[i * 3 + 1], outputTanks[i * 3 + 2]
                    };
            int base = i * SLOTS_PER_MODULE;
            int[] in = {5 + base, 6 + base, 7 + base};
            int[] out = {OUTPUT_OFFSET + base, OUTPUT_OFFSET + 1 + base, OUTPUT_OFFSET + 2 + base};
            module[i] =
                    new ModuleMachineBase(
                            i,
                            this,
                            ChemicalPlantRecipes.INSTANCE,
                            this,
                            in,
                            out,
                            moduleIn[i],
                            moduleOut[i]);

            int[] reach = new int[3 + MODULES * 3];
            System.arraycopy(in, 0, reach, 0, 3);
            for (int m = 0; m < MODULES; m++) {
                int outBase = OUTPUT_OFFSET + m * SLOTS_PER_MODULE;
                reach[3 + m * 3] = outBase;
                reach[4 + m * 3] = outBase + 1;
                reach[5 + m * 3] = outBase + 2;
            }
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

    @Override
    public void tickServer() {
        long prevPower = power;
        dirtyThisTick = false;

        long nextMaxPower = 0;
        for (int i = 0; i < MODULES; i++) {
            GenericRecipe rec = module[i].getRecipe();
            if (rec != null) nextMaxPower += rec.power * 100L;
        }
        maxPower = Math.max(power, Math.max(nextMaxPower, MAX_POWER));

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, maxPower - power, false);
        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);

        flush.provide((ServerLevel) level, this);

        int speedLevel = Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
        int powerLevel = Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3);
        int overdriveLevel = Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);

        double speed = 1D + speedLevel / 3D + overdriveLevel;
        double pow = 1D - powerLevel * 0.25D + speedLevel + overdriveLevel * 10D / 3D;

        boolean cool = canCool();
        for (int i = 0; i < MODULES; i++) {

            module[i].update(speed * 2D, pow * 2D, cool, inventory.get(4 + i * SLOTS_PER_MODULE));
            didProcess[i] = module[i].didProcess;
            dirtyThisTick |= module[i].markDirty;
            if (didProcess[i]) {
                water().setFill(water().getFill() - 100);
                lps().setFill(lps().getFill() + 100);
            }
        }

        for (FluidTankNTM in : inputTanks) {
            if (in.getTankType() == null) continue;
            for (FluidTankNTM out : outputTanks) {
                if (!in.accepts(out.getFluid())) continue;
                if (out.getPressure() != in.getPressure()) continue;
                int toMove = Math.min(Math.min(in.getMaxFill() - in.getFill(), out.getFill()), 50);
                if (toMove > 0) {
                    int moved = in.receive(out.getFluid(), toMove);
                    out.setFill(out.getFill() - moved);
                }
            }
        }

        if (dirtyThisTick || power != prevPower) setChanged();

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

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);
        prevAnim = anim;
        if (didProcess[0] || didProcess[1] || didProcess[2] || didProcess[3]) anim++;

        if (TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {
            frame = !level.getBlockState(worldPosition.above(3)).isAir();
        }
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.CHEMICAL_PLANT_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1F,
                15F,
                1.0F,
                20);
    }

    public boolean anyProcessing() {
        return didProcess[0] || didProcess[1] || didProcess[2] || didProcess[3];
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, maxPower));
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
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
        Direction dir = getBlockState().getValue(BlockMultiblockCore.FACING);
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
            return stack.getItem() instanceof ItemMachineUpgrade;
        for (int i = 0; i < MODULES; i++) {
            int base = i * SLOTS_PER_MODULE;
            if (slot == 4 + base) return stack.getItem() instanceof ItemBlueprints;
            if (slot >= OUTPUT_OFFSET + base && slot < OUTPUT_OFFSET + OUTPUTS_PER_MODULE + base)
                return false;
            if (slot >= 5 + base && slot <= 7 + base) {
                return module[i].isItemValid(slot, stack);
            }
        }
        return true;
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
        for (int i = 0; i < MODULES; i++) {
            int base = i * SLOTS_PER_MODULE;
            if (slot >= 5 + base && slot <= 7 + base) return canPlaceItem(slot, stack);
        }
        return false;
    }

    private boolean canExtractSlot(int slot, ItemStack stack) {
        for (int i = 0; i < MODULES; i++) {
            int base = i * SLOTS_PER_MODULE;
            if (slot >= OUTPUT_OFFSET + base && slot < OUTPUT_OFFSET + OUTPUTS_PER_MODULE + base)
                return true;
        }
        for (int i = 0; i < MODULES; i++) {
            if (module[i].isSlotClogged(slot)) return true;
        }
        return false;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("index") && data.contains("selection")) {
            int index = data.getIntOr("index", 0);
            String selection = data.getStringOr("selection", "");
            if (index >= 0 && index < MODULES) {

                module[index].setRecipe(selection);
                setChanged();
            }
        }
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "anyactive").equals(name))
            return ""
                    + ((didProcess[0] || didProcess[1] || didProcess[2] || didProcess[3]) ? 1 : 0);
        for (int i = 0; i < MODULES; i++) {
            if ((PREFIX_VALUE + "progress" + (i + 1)).equals(name))
                return "" + (int) Math.round(module[i].progress * 100);
            if ((PREFIX_VALUE + "recipe" + (i + 1)).equals(name))
                return module[i].legacyRecipeName();
            if ((PREFIX_VALUE + "active" + (i + 1)).equals(name))
                return "" + (didProcess[i] ? 1 : 0);
        }
        return null;
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    private void writeDidProcess(ByteBuf output) {
        for (boolean value : didProcess) output.writeBoolean(value);
    }

    private void readDidProcess(ByteBuf input) {
        for (int i = 0; i < didProcess.length; i++) didProcess[i] = input.readBoolean();
    }

    private void writeModules(ByteBuf output) {
        for (ModuleMachineBase value : module) value.serialize(output);
    }

    private void readModules(ByteBuf input) {
        for (ModuleMachineBase value : module) value.deserialize(input);
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

        input.getLong("power").ifPresent(v -> power = v);
        input.getLong("maxPower").ifPresent(v -> maxPower = v);
        for (int i = 0; i < MODULES; i++) {
            module[i].load(input);
            input.child("i" + i + "a").ifPresent(inputTanks[i * 3]::deserialize);
            input.child("i" + i + "b").ifPresent(inputTanks[i * 3 + 1]::deserialize);
            input.child("i" + i + "c").ifPresent(inputTanks[i * 3 + 2]::deserialize);
            input.child("o" + i + "a").ifPresent(outputTanks[i * 3]::deserialize);
            input.child("o" + i + "b").ifPresent(outputTanks[i * 3 + 1]::deserialize);
            input.child("o" + i + "c").ifPresent(outputTanks[i * 3 + 2]::deserialize);
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
            inputTanks[i * 3].serialize(output.child("i" + i + "a"));
            inputTanks[i * 3 + 1].serialize(output.child("i" + i + "b"));
            inputTanks[i * 3 + 2].serialize(output.child("i" + i + "c"));
            outputTanks[i * 3].serialize(output.child("o" + i + "a"));
            outputTanks[i * 3 + 1].serialize(output.child("o" + i + "b"));
            outputTanks[i * 3 + 2].serialize(output.child("o" + i + "c"));
        }
        water().serialize(output.child("w"));
        lps().serialize(output.child("s"));
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
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineChemicalFactory(containerId, playerInventory, this);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x7fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeLong(this.maxPower);
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
            case 0 -> this.power = input.readLong();
            case 1 -> this.maxPower = input.readLong();
            case 2 -> readDidProcess(input);
            case 3 -> readModules(input);
            case 4 -> readInputTanks(input);
            case 5 -> readOutputTanks(input);
            case 6 -> readCoolant(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
