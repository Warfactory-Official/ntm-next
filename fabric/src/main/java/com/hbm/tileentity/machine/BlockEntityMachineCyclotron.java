// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.port.IPortHost;
import com.hbm.capability.port.ItemPort;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachineCyclotron;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CyclotronRecipe;
import com.hbm.inventory.recipes.CyclotronRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.IUpgradeInfoProvider;
import io.netty.buffer.ByteBuf;
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

public class BlockEntityMachineCyclotron extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                IUpgradeInfoProvider,
                IPortHost,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_PARTICLE_START = 0;
    public static final int SLOT_TARGET_START = 3;
    public static final int SLOT_OUTPUT_START = 6;
    public static final int SLOT_BATTERY = 9;
    public static final int SLOT_UPGRADE_START = 10;
    public static final int SLOT_UPGRADE_END = 11;
    public static final int SLOT_COUNT = 12;
    public static final int LINES = 3;

    public static final long MAX_POWER = 100_000_000L;
    public static final int CONSUMPTION = 1_000_000;
    public static final int DURATION = 690;
    public static final int WATER_CAPACITY = 32_000;
    public static final int STEAM_CAPACITY = 32_000;
    public static final int AMAT_CAPACITY = 8_000;

    public static final int BASE_COOLANT = 500;
    public static final int PLUGS = 4;

    private static final int[][] SLOTS_BY_LINE = {
        {
            SLOT_PARTICLE_START,
            SLOT_TARGET_START,
            SLOT_OUTPUT_START,
            SLOT_OUTPUT_START + 1,
            SLOT_OUTPUT_START + 2
        },
        {
            SLOT_PARTICLE_START + 1,
            SLOT_TARGET_START + 1,
            SLOT_OUTPUT_START,
            SLOT_OUTPUT_START + 1,
            SLOT_OUTPUT_START + 2
        },
        {
            SLOT_PARTICLE_START + 2,
            SLOT_TARGET_START + 2,
            SLOT_OUTPUT_START,
            SLOT_OUTPUT_START + 1,
            SLOT_OUTPUT_START + 2
        },
    };

    private static final int RING = 2;
    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(
                    UpgradeType.SPEED, 3, UpgradeType.POWER, 3, UpgradeType.EFFECT, 3);

    @SyncField(units = 1L << 3)
    public final FluidTankNTM water = new FluidTankNTM(NTMFluids.WATER, WATER_CAPACITY);

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    @SyncField(units = 1L << 4)
    public final FluidTankNTM steam = new FluidTankNTM(NTMFluids.SPENTSTEAM, STEAM_CAPACITY);

    @SyncField(units = 1L << 5)
    public final FluidTankNTM amat = new FluidTankNTM(NTMFluids.AMAT, AMAT_CAPACITY);

    private final UpgradeManager upgradeManager = new UpgradeManager(this);

    @SyncField(units = 1L << 0)
    @ContainerSync
    public long power;

    @SyncField(units = 1L << 1)
    @ContainerSync
    public int progress;

    @SyncField(units = 1L << 2)
    public byte plugs;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityMachineCyclotron(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CYCLOTRON.get(), pos, state, SLOT_COUNT);
        receiving = new FluidTankNTM[] {water};
        sending = new FluidTankNTM[] {steam, amat};
    }

    public static @Nullable Item itemForPlug(int index) {
        return switch (index) {
            case 0 -> ModItems.POWDER_BALEFIRE.get();
            case 1 -> ModItems.BOOK_OF.get();
            case 2 -> ModItems.DIAMOND_GAVEL.get();
            case 3 -> ModItems.COIN_MASKMAN.get();
            default -> null;
        };
    }

    @Override
    public void tickServer() {
        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);
        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);

        if (canProcess()) {
            progress += getSpeed();
            power -= getConsumption();

            int convert = getCoolantConsumption();
            water.setFill(water.getFill() - convert);
            steam.setFill(steam.getFill() + convert);

            if (progress >= DURATION) {
                process();
                progress = 0;
                setChanged();
            }
        } else {
            progress = 0;
        }

        flush.provide((ServerLevel) level, this);

        networkPackNT(25);
    }

    public boolean canProcess() {
        if (power < getConsumption()) return false;

        int convert = getCoolantConsumption();
        if (water.getFill() < convert) return false;
        if (steam.getFill() + convert > steam.getMaxFill()) return false;

        for (int i = 0; i < LINES; i++) {
            CyclotronRecipe recipe = recipeFor(i);
            if (recipe == null) continue;

            ItemStack out = recipe.output();
            if (out.isEmpty()) continue;

            ItemStack slot = inventory.get(SLOT_OUTPUT_START + i);
            if (slot.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(slot, out)
                    && slot.getCount() < out.getMaxStackSize()) {
                return true;
            }
        }

        return false;
    }

    private @Nullable CyclotronRecipe recipeFor(int line) {
        return CyclotronRecipes.getOutput(
                inventory.get(SLOT_TARGET_START + line), inventory.get(SLOT_PARTICLE_START + line));
    }

    public void process() {
        for (int i = 0; i < LINES; i++) {
            CyclotronRecipe recipe = recipeFor(i);
            if (recipe == null) continue;

            ItemStack out = recipe.output();
            if (out.isEmpty()) continue;

            ItemStack slot = inventory.get(SLOT_OUTPUT_START + i);

            if (slot.isEmpty()) {
                removeItem(SLOT_PARTICLE_START + i, 1);
                removeItem(SLOT_TARGET_START + i, 1);
                inventory.set(SLOT_OUTPUT_START + i, out.copy());
                amat.setFill(amat.getFill() + recipe.amat);
            } else if (ItemStack.isSameItemSameComponents(slot, out)
                    && slot.getCount() < out.getMaxStackSize()) {
                removeItem(SLOT_PARTICLE_START + i, 1);
                removeItem(SLOT_TARGET_START + i, 1);
                slot.grow(1);
                amat.setFill(amat.getFill() + recipe.amat);
            }
        }

        if (amat.getFill() > amat.getMaxFill()) amat.setFill(amat.getMaxFill());
    }

    public int getSpeed() {
        return upgradeManager.getLevel(UpgradeType.SPEED) + 1;
    }

    public int getConsumption() {
        return CONSUMPTION - 100_000 * upgradeManager.getLevel(UpgradeType.POWER);
    }

    public int getCoolantConsumption() {
        return BASE_COOLANT / (upgradeManager.getLevel(UpgradeType.EFFECT) + 1) * getSpeed();
    }

    public void setPlug(int index) {
        plugs |= (byte) (1 << index);
        setChanged();
    }

    public boolean getPlug(int index) {
        return (plugs & (1 << index)) > 0;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, MAX_POWER));
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return IBatteryItem.isBattery(stack);
        if (slot >= SLOT_UPGRADE_START && slot <= SLOT_UPGRADE_END)
            return ItemMachineUpgrade.isUpgrade(stack);

        if (slot < SLOT_TARGET_START) return CyclotronRecipes.isParticle(stack);
        if (slot < SLOT_OUTPUT_START) return CyclotronRecipes.isTarget(stack);
        return false;
    }

    @Override
    public @Nullable ItemPort itemAccess(BlockPos cell, Direction side) {
        int line = lineAt(cell);
        if (line < 0) return ItemPort.none();
        return new ItemPort(
                SLOTS_BY_LINE[line],
                this::canPlaceItem,
                (slot, stack) -> slot >= SLOT_OUTPUT_START && slot < SLOT_BATTERY);
    }

    private int lineAt(BlockPos cell) {
        if (cell.getY() != worldPosition.getY()) return -1;
        int dx = cell.getX() - worldPosition.getX();
        int dz = cell.getZ() - worldPosition.getZ();

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            Direction rot = dir.getClockWise();
            int bx = dir.getStepX() * RING;
            int bz = dir.getStepZ() * RING;
            if (dx == bx + rot.getStepX() && dz == bz + rot.getStepZ()) return 0;
            if (dx == bx && dz == bz) return 1;
            if (dx == bx - rot.getStepX() && dz == bz - rot.getStepZ()) return 2;
        }
        return -1;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_OUTPUT_START && slot < SLOT_BATTERY;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.cyclotron");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineCyclotron(containerId, playerInventory, this);
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {water, steam, amat};
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        progress = input.getIntOr("progress", 0);
        plugs = (byte) input.getIntOr("plugs", 0);
        input.child("t0").ifPresent(water::deserialize);
        input.child("t1").ifPresent(steam::deserialize);
        input.child("t2").ifPresent(amat::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
        output.putInt("plugs", plugs);
        water.serialize(output.child("t0"));
        steam.serialize(output.child("t1"));
        amat.serialize(output.child("t2"));
    }

    @Override
    public long syncUnitMask() {
        return 0x3fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeInt(this.progress);
            case 2 -> output.writeByte(this.plugs);
            case 3 -> this.water.packetSerialize(output);
            case 4 -> this.steam.packetSerialize(output);
            case 5 -> this.amat.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.progress = input.readInt();
            case 2 -> this.plugs = input.readByte();
            case 3 -> this.water.packetDeserialize(input);
            case 4 -> this.steam.packetDeserialize(input);
            case 5 -> this.amat.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
