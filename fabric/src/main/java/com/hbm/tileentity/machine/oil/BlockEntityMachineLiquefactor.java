// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.oil;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuMachineLiquefactor;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.LiquefactionRecipes;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineLiquefactor extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                IUpgradeInfoProvider,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_BATTERY = 1;
    public static final int SLOT_UPGRADE_START = 2;
    public static final int SLOT_UPGRADE_END = 3;
    public static final int SLOT_COUNT = 4;

    public static final long MAX_POWER = 100_000L;
    public static final int USAGE_BASE = 250;
    public static final int PROCESS_TIME_BASE = 60;

    private static final int[] ACCESSIBLE_SLOTS = {SLOT_INPUT};
    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(UpgradeType.SPEED, 3, UpgradeType.POWER, 3);

    @SyncField(units = 1L << 4)
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.NONE, 24_000);

    private final FluidTankNTM[] sending;
    private final UpgradeManager upgradeManager = new UpgradeManager(this);

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 2)
    public int usage;

    @SyncField(units = 1L << 1)
    public int progress;

    @SyncField(units = 1L << 3)
    public int processTime;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityMachineLiquefactor(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIQUEFACTOR.get(), pos, state, SLOT_COUNT);
        sending = new FluidTankNTM[] {tank};
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineLiquefactor");
    }

    @Override
    public void tickServer() {
        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);

        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int speed = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);

        this.processTime = PROCESS_TIME_BASE - (PROCESS_TIME_BASE / 4) * speed;
        this.usage = (USAGE_BASE + (USAGE_BASE * speed)) / (powerLevel + 1);

        if (canProcess()) process();
        else if (progress != 0) {
            progress = 0;
            setChanged();
        }

        flush.provide((ServerLevel) level, this);

        networkPackNT(50);
    }

    public boolean canProcess() {
        if (power < usage) return false;
        if (inventory.get(SLOT_INPUT).isEmpty()) return false;

        FluidStackNTM out = LiquefactionRecipes.INSTANCE.getOutput(inventory.get(SLOT_INPUT));
        if (out == null) return false;
        if (out.type() != tank.getTankType() && tank.getFill() > 0) return false;
        return out.amount() + tank.getFill() <= tank.getMaxFill();
    }

    private void process() {
        power -= usage;
        progress++;

        if (progress >= processTime) {
            FluidStackNTM out = LiquefactionRecipes.INSTANCE.getOutput(inventory.get(SLOT_INPUT));
            tank.setTankType(out.type());
            tank.setFill(tank.getFill() + (int) out.amount());
            inventory.get(SLOT_INPUT).shrink(1);
            progress = 0;
        }
        setChanged();
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        long updated = Math.max(0L, Math.min(p, MAX_POWER));
        if (power == updated) return;
        power = updated;
        setChanged();
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_INPUT -> LiquefactionRecipes.INSTANCE.getOutput(stack) != null;
            case SLOT_BATTERY -> IBatteryItem.isBattery(stack);
            case SLOT_UPGRADE_START, SLOT_UPGRADE_END -> ItemMachineUpgrade.isUpgrade(stack);
            default -> false;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == SLOT_INPUT && LiquefactionRecipes.INSTANCE.getOutput(stack) != null;
    }

    @Override
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineLiquefactor(containerId, playerInventory, this);
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {tank};
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        progress = input.getIntOr("progress", progress);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
        tank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeInt(this.progress);
            case 2 -> output.writeInt(this.usage);
            case 3 -> output.writeInt(this.processTime);
            case 4 -> this.tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.progress = input.readInt();
            case 2 -> this.usage = input.readInt();
            case 3 -> this.processTime = input.readInt();
            case 4 -> this.tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
