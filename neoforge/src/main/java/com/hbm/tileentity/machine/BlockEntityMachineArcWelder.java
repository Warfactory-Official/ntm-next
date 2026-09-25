// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.port.IPortHost;
import com.hbm.capability.port.ItemPort;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachineArcWelder;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ArcWelderRecipe;
import com.hbm.inventory.recipes.ArcWelderRecipes;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.toclient.EffectNTPayload;
import com.hbm.particle.HbmEffectNT;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.platform.Services;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineArcWelder extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                IUpgradeInfoProvider,
                IPortHost,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_INPUT_START = 0;
    public static final int SLOT_OUTPUT = 3;
    public static final int SLOT_BATTERY = 4;
    public static final int SLOT_FLUID_ID = 5;
    public static final int SLOT_UPGRADE_START = 6;
    public static final int SLOT_UPGRADE_END = 7;
    public static final int SLOT_COUNT = 8;

    public static final int TANK_CAPACITY = 24_000;
    private static final long DEFAULT_MAX_POWER = 2_000L;
    private static final long IDLE_CONSUMPTION = 100L;

    private static final int[] ACCESSIBLE_SLOTS = {SLOT_INPUT_START + 1, SLOT_OUTPUT};

    private static final int[] SLOTS_RED = {SLOT_INPUT_START, SLOT_OUTPUT};
    private static final int[] SLOTS_YELLOW = {SLOT_INPUT_START + 1, SLOT_OUTPUT};
    private static final int[] SLOTS_GREEN = {SLOT_INPUT_START + 2, SLOT_OUTPUT};

    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(
                    UpgradeType.SPEED, 3, UpgradeType.POWER, 3, UpgradeType.OVERDRIVE, 3);

    @SyncField(units = 1L << 5)
    public final FluidTankNTM tank;

    private final FluidTankNTM[] receiving;
    private final UpgradeManager upgradeManager = new UpgradeManager(this);

    @SyncField(units = 1L << 0)
    @ContainerSync
    public long power;

    @SyncField(units = 1L << 1)
    @ContainerSync
    public long maxPower = DEFAULT_MAX_POWER;

    @SyncField(units = 1L << 2)
    @ContainerSync
    public long consumption;

    @SyncField(units = 1L << 3)
    @ContainerSync
    public int progress;

    @SyncField(units = 1L << 4)
    @ContainerSync
    public int processTime = 1;

    public ItemStack display = ItemStack.EMPTY;

    @SyncField(units = 1L << 6)
    private @Nullable ArcWelderRecipe recipe;

    public BlockEntityMachineArcWelder(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARC_WELDER.get(), pos, state, SLOT_COUNT);
        this.tank = new FluidTankNTM(TANK_CAPACITY);
        receiving = new FluidTankNTM[] {tank};
    }

    @Override
    public void tickServer() {
        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, maxPower - power, false);
        tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);

        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);
        int overdriveLevel = upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        this.recipe =
                ArcWelderRecipes.getRecipe(inventory.get(0), inventory.get(1), inventory.get(2));
        long intendedMaxPower;

        if (recipe != null) {
            processTime =
                    recipe.duration
                            - recipe.duration * speedLevel / 6
                            + recipe.duration * powerLevel / 3;
            consumption = recipe.power + recipe.power * speedLevel - recipe.power * powerLevel / 6;
            consumption *= (long) Math.pow(2, overdriveLevel);
            intendedMaxPower = consumption * 20L;

            if (canProcess(recipe)) {
                progress += 1 + overdriveLevel;
                power -= consumption;
                if (progress >= processTime) {
                    progress = 0;
                    consumeAndProduce(recipe);
                }

                boolean burst = TickPhase.every(this, 20);
                if (burst || TickPhase.every(this, 2)) {
                    ServerLevel server = (ServerLevel) level;
                    Direction dir = BlockMultiblockCore.coreFacing(getBlockState());
                    double px = worldPosition.getX() + 0.5 - dir.getStepX() * 0.5;
                    double py = worldPosition.getY() + 1.25;
                    double pz = worldPosition.getZ() + 0.5 - dir.getStepZ() * 0.5;
                    if (burst) {
                        ParticleCreators.sparks(server, px, py, pz, 5, false, 25);
                    } else {
                        Services.NETWORK.sendToAllAround(
                                new EffectNTPayload(HbmEffectNT.Hadron, px, py, pz),
                                new TargetPoint(server, px, py, pz, 25));
                    }
                }
            } else {
                progress = 0;
            }
        } else {
            progress = 0;
            consumption = IDLE_CONSUMPTION;
            intendedMaxPower = DEFAULT_MAX_POWER;
        }

        maxPower = Math.max(intendedMaxPower, power);
        networkPackNT(25);
    }

    private boolean canProcess(ArcWelderRecipe recipe) {
        if (power < consumption) return false;
        if (recipe.fluid() != null && !recipe.fluid().matches(tank)) return false;

        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (!out.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(out, recipe.output())) return false;
            return out.getCount() + recipe.output().getCount() <= out.getMaxStackSize();
        }
        return true;
    }

    private void consumeAndProduce(ArcWelderRecipe recipe) {
        for (var ing : recipe.inputItem) {
            for (int i = SLOT_INPUT_START; i < SLOT_OUTPUT; i++) {
                ItemStack stack = inventory.get(i);
                if (ing.test(stack)) {
                    stack.shrink(ing.count());
                    break;
                }
            }
        }
        if (recipe.fluid() != null) recipe.fluid().drainFrom(tank);

        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (out.isEmpty()) {
            inventory.set(SLOT_OUTPUT, recipe.output().copy());
        } else {
            out.grow(recipe.output().getCount());
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return IBatteryItem.isBattery(stack);
        if (slot == SLOT_UPGRADE_START || slot == SLOT_UPGRADE_END)
            return ItemMachineUpgrade.isUpgrade(stack);

        return slot >= SLOT_INPUT_START && slot < SLOT_OUTPUT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot != SLOT_OUTPUT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public @Nullable ItemPort itemAccess(BlockPos cell, Direction side) {
        if (cell.equals(worldPosition)) return null;

        Direction dir = BlockMultiblockCore.coreFacing(getBlockState());
        Direction rot = dir.getClockWise();
        BlockPos back = worldPosition.relative(dir.getOpposite());

        int[] slots;
        if (cell.equals(worldPosition.relative(rot))
                || cell.equals(back.relative(rot.getOpposite()))) {
            slots = SLOTS_RED;
        } else if (cell.equals(back)) {
            slots = SLOTS_YELLOW;
        } else if (cell.equals(worldPosition.relative(rot.getOpposite()))
                || cell.equals(back.relative(rot))) {
            slots = SLOTS_GREEN;
        } else {
            return ItemPort.none();
        }

        return new ItemPort(
                slots,
                (slot, stack) -> slot < SLOT_OUTPUT && canPlaceItem(slot, stack),
                (slot, stack) -> slot == SLOT_OUTPUT);
    }

    @Override
    public long getPower() {
        return Math.max(0L, Math.min(power, maxPower));
    }

    @Override
    public void setPower(long p) {
        power = p;
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
        return receiving;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tank;
    }

    private void writeDisplay(ByteBuf output) {
        boolean hasRecipe = recipe != null;
        output.writeBoolean(hasRecipe);
        if (hasRecipe) output.writeInt(BuiltInRegistries.ITEM.getId(recipe.output().getItem()));
    }

    private void readDisplay(ByteBuf input) {
        display =
                input.readBoolean()
                        ? new ItemStack(BuiltInRegistries.ITEM.byId(input.readInt()))
                        : ItemStack.EMPTY;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineArcWelder");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineArcWelder(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        input.getLong("power").ifPresent(v -> power = v);
        input.getLong("maxPower").ifPresent(v -> maxPower = v);
        input.getLong("consumption").ifPresent(v -> consumption = v);
        input.getInt("progress").ifPresent(v -> progress = v);
        input.getInt("processTime").ifPresent(v -> processTime = v);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        output.putLong("consumption", consumption);
        output.putInt("progress", progress);
        output.putInt("processTime", processTime);
        tank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return 0x7fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeLong(this.maxPower);
            case 2 -> output.writeLong(this.consumption);
            case 3 -> output.writeInt(this.progress);
            case 4 -> output.writeInt(this.processTime);
            case 5 -> this.tank.packetSerialize(output);
            case 6 -> writeDisplay(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.maxPower = input.readLong();
            case 2 -> this.consumption = input.readLong();
            case 3 -> this.progress = input.readInt();
            case 4 -> this.processTime = input.readInt();
            case 5 -> this.tank.packetDeserialize(input);
            case 6 -> readDisplay(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
