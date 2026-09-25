// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuMachineEPress;
import com.hbm.inventory.recipes.PressRecipe;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.items.machine.ItemStamp.StampType;
import com.hbm.items.machine.ItemStamp;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSlots;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IUpgradeInfoProvider;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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

@SyncSlots(
        value = {BlockEntityMachineEPress.SLOT_INPUT},
        units = 1L << 2,
        components = false)
public class BlockEntityMachineEPress extends BlockEntityMachineBase
        implements IEnergyHandlerMK2, MenuProvider, IUpgradeInfoProvider, SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_STAMP = 1;
    public static final int SLOT_INPUT = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int SLOT_UPGRADE = 4;
    public static final int SLOT_COUNT = 5;

    public static final long MAX_POWER = 50_000L;
    public static final long POWER_PER_TICK = 100L;
    public static final int MAX_PROGRESS = 200;
    public static final int STAMP_SPEED = 45;
    public static final int RETRACT_SPEED = 20;
    public static final int DELAY = 5;

    private static final int[] ACCESSIBLE_SLOTS = {SLOT_STAMP, SLOT_INPUT, SLOT_OUTPUT};
    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(UpgradeType.SPEED, 3);
    private final UpgradeManager upgradeManager = new UpgradeManager(this);

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 1)
    public int progress;

    public double renderPress;
    public double lastPress;
    private double syncPress;
    private int turnProgress;
    public boolean isRetracting;
    private int delay;

    public BlockEntityMachineEPress(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_PRESS.get(), pos, state, SLOT_COUNT);
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
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_STAMP -> stack.getItem() instanceof ItemStamp;
            case SLOT_INPUT -> !(stack.getItem() instanceof ItemStamp);
            case SLOT_OUTPUT -> false;
            default -> true;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {

        return (slot == SLOT_STAMP || slot == SLOT_INPUT) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public void tickServer() {
        long prevPower = power;
        int prevProgress = progress;

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);

        upgradeManager.scan(this, SLOT_UPGRADE, SLOT_UPGRADE);
        int speed = 1 + upgradeManager.getLevel(UpgradeType.SPEED);
        double scale = 1.0D + speed / 4.0D;

        boolean canProcess = canProcess();

        if ((canProcess || isRetracting || delay > 0) && power >= POWER_PER_TICK) {
            power -= POWER_PER_TICK;

            if (delay <= 0) {
                if (isRetracting) {

                    progress -= (int) (RETRACT_SPEED * scale);
                    if (progress <= 0) {
                        isRetracting = false;
                        delay = DELAY - speed + 1;
                    }
                } else if (canProcess) {
                    progress += (int) (STAMP_SPEED * scale);
                    if (progress >= MAX_PROGRESS) {
                        progress = MAX_PROGRESS;
                        if (level != null) {
                            level.playSound(
                                    null,
                                    worldPosition,
                                    ModSounds.PRESS_OPERATE.get(),
                                    SoundSource.BLOCKS,
                                    1.5F,
                                    1.0F);
                        }
                        craftItem();
                        isRetracting = true;
                        delay = DELAY - speed + 1;
                        setChanged();
                    }
                } else if (progress > 0) {

                    isRetracting = true;
                }
            } else {
                delay--;
            }
        }

        if (power != prevPower || progress != prevProgress) setChanged();
        networkPackNT(50);
    }

    private boolean canProcess() {
        ItemStack output = currentOutput();
        if (output.isEmpty()) return false;
        ItemStack outSlot = inventory.get(SLOT_OUTPUT);
        if (outSlot.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(outSlot, output)) return false;
        return outSlot.getCount() + output.getCount() <= outSlot.getMaxStackSize();
    }

    private @Nullable PressRecipe currentRecipe() {
        StampType stampType = ItemStamp.typeOf(inventory.get(SLOT_STAMP));
        if (stampType == null) return null;
        return PressRecipes.INSTANCE.getRecipe(inventory.get(SLOT_INPUT), stampType);
    }

    private ItemStack currentOutput() {
        PressRecipe recipe = currentRecipe();
        return recipe == null ? ItemStack.EMPTY : recipe.output().copy();
    }

    private void craftItem() {
        PressRecipe recipe = currentRecipe();
        if (recipe == null) return;
        ItemStack output = recipe.output().copy();
        if (output.isEmpty()) return;

        ItemStack outSlot = inventory.get(SLOT_OUTPUT);
        if (outSlot.isEmpty()) inventory.set(SLOT_OUTPUT, output);
        else outSlot.grow(output.getCount());

        inventory.get(SLOT_INPUT).shrink(1);
        damageStamp();
    }

    private void damageStamp() {
        ItemStack stamp = inventory.get(SLOT_STAMP);
        if (!stamp.isDamageableItem()) return;
        stamp.setDamageValue(stamp.getDamageValue() + 1);
        if (stamp.getDamageValue() >= stamp.getMaxDamage())
            inventory.set(SLOT_STAMP, ItemStack.EMPTY);
    }

    private void readProgress(ByteBuf input) {
        progress = input.readInt();
        syncPress = progress;
        turnProgress = 2;
    }

    private void writePreview(ByteBuf output) {
        ItemStack input = inventory.get(SLOT_INPUT);
        output.writeInt(input.isEmpty() ? -1 : BuiltInRegistries.ITEM.getId(input.getItem()));
        output.writeByte(input.getCount());
    }

    @Override
    public void tickClient() {

        lastPress = renderPress;

        if (turnProgress > 0) {
            renderPress += (syncPress - renderPress) / turnProgress;
            turnProgress--;
        } else {
            renderPress = syncPress;
        }
    }

    private void readPreview(ByteBuf input) {
        int inputId = input.readInt();
        int inputCount = input.readByte();
        inventory.set(
                SLOT_INPUT,
                inputId == -1
                        ? ItemStack.EMPTY
                        : new ItemStack(BuiltInRegistries.ITEM.byId(inputId), inputCount));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.epress");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineEPress(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        power = input.getLongOr("power", 0L);
        progress = input.getIntOr("progress", progress);
        isRetracting = input.getBooleanOr("isRetracting", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
        output.putBoolean("isRetracting", isRetracting);
    }

    @Override
    public long syncUnitMask() {
        return 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeInt(this.progress);
            case 2 -> writePreview(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> readProgress(input);
            case 2 -> readPreview(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
