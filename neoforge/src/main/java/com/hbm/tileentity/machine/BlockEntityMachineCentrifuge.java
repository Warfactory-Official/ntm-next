// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachineCentrifuge;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IUpgradeInfoProvider;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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

public class BlockEntityMachineCentrifuge extends BlockEntityMachineBase
        implements AudioLoop,
                IEnergyHandlerMK2,
                MenuProvider,
                IUpgradeInfoProvider,
                SyncUnitSchema {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_BATTERY = 1;
    public static final int SLOT_OUTPUT_START = 2;
    public static final int SLOT_OUTPUT_END = 5;
    public static final int SLOT_UPGRADE_START = 6;
    public static final int SLOT_UPGRADE_END = 7;
    public static final int SLOT_COUNT = 8;

    private static final int[] ACCESSIBLE_SLOTS = {0, 2, 3, 4, 5};
    private static final int[] OUTPUT_SLOTS = {2, 3, 4, 5};
    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(
                    UpgradeType.SPEED, 3, UpgradeType.POWER, 3, UpgradeType.OVERDRIVE, 3);

    public static Consumer<BlockEntityMachineCentrifuge> CLIENT_SOUND = be -> {};
    private final UpgradeManager upgradeManager = new UpgradeManager(this);
    @ContainerSync public long power;
    @ContainerSync public int progress;

    @SyncField(units = 1L << 0)
    public boolean isProgressing;

    public int audioDuration;

    public BlockEntityMachineCentrifuge(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CENTRIFUGE.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, getMaxPower()));
    }

    @Override
    public long getMaxPower() {
        return MachineData.CENTRIFUGE_MAX_POWER.get();
    }

    @Override
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_INPUT)
            return CentrifugeRecipes.INSTANCE.matchFor(stack, getLevel()) != null;
        return slot == SLOT_BATTERY && IBatteryItem.isBattery(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == SLOT_INPUT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_OUTPUT_START && slot <= SLOT_OUTPUT_END;
    }

    @Override
    public void tickServer() {
        long prevPower = power;
        int prevProgress = progress;

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, getMaxPower() - power, false);

        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);
        int overdriveLevel = upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        long baseConsumption = MachineData.CENTRIFUGE_CONSUMPTION.get();
        long consumption = baseConsumption;
        int speed = 1 + speedLevel;
        consumption += (long) speedLevel * baseConsumption;
        speed *= (1 + overdriveLevel * 5);
        consumption += (long) overdriveLevel * baseConsumption * 50L;
        consumption /= (1 + powerLevel);

        if (power > 0 && progress > 0) power = Math.max(0L, power - consumption);

        isProgressing = power > 0 && canProcess();

        if (isProgressing) {
            progress += speed;
            if (progress >= MachineData.CENTRIFUGE_PROCESS_TIME.get()) {
                progress = 0;
                processItem();
            }
        } else {
            progress = 0;
        }

        if (power != prevPower || progress != prevProgress) setChanged();
        networkPackNT(50);
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.CENTRIFUGE_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1.0F,
                10F,
                1.0F,
                20);
    }

    private boolean canProcess() {
        ItemStack[] out =
                CentrifugeRecipes.INSTANCE.getOutputs(inventory.get(SLOT_INPUT), getLevel());
        if (out == null) return false;
        for (int i = 0; i < out.length && i <= SLOT_OUTPUT_END - SLOT_OUTPUT_START; i++) {
            ItemStack produced = out[i];
            if (produced.isEmpty()) continue;
            ItemStack slot = inventory.get(SLOT_OUTPUT_START + i);
            if (slot.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(slot, produced)) return false;
            if (slot.getCount() + produced.getCount() > slot.getMaxStackSize()) return false;
        }
        return true;
    }

    private void processItem() {
        ItemStack input = inventory.get(SLOT_INPUT);
        ItemStack[] out = CentrifugeRecipes.INSTANCE.getOutputs(input, getLevel());
        if (out == null) return;
        for (int i = 0; i < out.length && i <= SLOT_OUTPUT_END - SLOT_OUTPUT_START; i++) {
            ItemStack produced = out[i];
            if (produced.isEmpty()) continue;
            int slotIdx = SLOT_OUTPUT_START + i;
            ItemStack slot = inventory.get(slotIdx);
            if (slot.isEmpty()) inventory.set(slotIdx, produced);
            else slot.grow(produced.getCount());
        }
        input.shrink(1);
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.centrifuge");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineCentrifuge(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        progress = input.getIntOr("progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 0;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.isProgressing);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.isProgressing = input.readBoolean();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
