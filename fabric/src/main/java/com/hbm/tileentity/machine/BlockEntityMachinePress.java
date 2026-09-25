// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.handler.FuelHandler;
import com.hbm.inventory.container.MenuMachinePress;
import com.hbm.inventory.recipes.PressRecipe;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.items.machine.ItemStamp.StampType;
import com.hbm.items.machine.ItemStamp;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSlots;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
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
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

@SyncSlots(
        value = {BlockEntityMachinePress.SLOT_INPUT},
        units = 1L << 3,
        components = false)
public class BlockEntityMachinePress extends BlockEntityMachineBase
        implements MenuProvider, SyncUnitSchema {

    public static final int SLOT_FUEL = 0;
    public static final int SLOT_STAMP = 1;
    public static final int SLOT_INPUT = 2;
    public static final int SLOT_OUTPUT = 3;

    public static final int SLOT_STORAGE_START = 4;
    public static final int SLOT_STORAGE_COUNT = 9;
    public static final int SLOT_COUNT = SLOT_STORAGE_START + SLOT_STORAGE_COUNT;

    public static final int MAX_SPEED = 400;
    public static final int PROGRESS_AT_MAX = 25;
    public static final int MAX_PROGRESS = 200;
    public static final int FUEL_PER_OP = 200;
    public static final int DELAY = 5;

    private static final int[] ACCESSIBLE_SLOTS = {SLOT_FUEL, SLOT_STAMP, SLOT_INPUT, SLOT_OUTPUT};

    @SyncField(units = 1L << 1)
    public int speed;

    @SyncField(units = 1L << 2)
    public int burnTime;

    @SyncField(units = 1L << 0)
    public int progress;

    public boolean isRetracting;
    public int delay;
    private boolean preheated;

    public double renderPress;
    public double lastPress;
    private double syncPress;
    private int turnProgress;

    public BlockEntityMachinePress(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRESS.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FUEL -> level != null && FuelHandler.getBurnTime(level, stack) > 0;
            case SLOT_STAMP -> stack.getItem() instanceof ItemStamp;
            case SLOT_INPUT -> !(stack.getItem() instanceof ItemStamp);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {

        return (slot == SLOT_FUEL || slot == SLOT_STAMP || slot == SLOT_INPUT)
                && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public void tickServer() {
        int prevProgress = progress;
        int prevSpeed = speed;

        boolean canProcess = canProcess();

        if ((canProcess || isRetracting) && burnTime >= FUEL_PER_OP) {
            speed = Math.min(MAX_SPEED, speed + (preheated ? 4 : 1));
        } else {
            speed = Math.max(0, speed - 1);
        }

        if (delay <= 0) {
            int stampSpeed = speed * PROGRESS_AT_MAX / MAX_SPEED;

            if (isRetracting) {

                progress -= stampSpeed;
                if (progress <= 0) {
                    isRetracting = false;
                    delay = DELAY;
                }
            } else if (canProcess) {
                progress += stampSpeed;
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
                    delay = DELAY;
                    if (burnTime >= FUEL_PER_OP) burnTime -= FUEL_PER_OP;
                    setChanged();
                }
            }
        } else {
            delay--;
        }

        if (!canProcess && !isRetracting && progress > 0) isRetracting = true;

        consumeFuel();

        if (progress != prevProgress || speed != prevSpeed) setChanged();
        networkPackNT(50);
    }

    public void refreshPreheater() {
        preheated = false;
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(worldPosition.relative(dir))
                    .is(ModBlocks.PRESS_PREHEATER.get())) {
                preheated = true;
                break;
            }
        }
    }

    private void consumeFuel() {
        ItemStack fuel = inventory.get(SLOT_FUEL);
        if (fuel.isEmpty() || burnTime >= FUEL_PER_OP || level == null) return;
        int duration = FuelHandler.getBurnTime(level, fuel);
        if (duration <= 0) return;
        burnTime += duration;
        ItemStackTemplate remainder = fuel.getCraftingRemainder();
        fuel.shrink(1);
        if (fuel.isEmpty())
            inventory.set(SLOT_FUEL, remainder != null ? remainder.create() : ItemStack.EMPTY);
        setChanged();
    }

    private boolean canProcess() {
        if (burnTime < FUEL_PER_OP) return false;
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
        return Component.translatable("container.press");
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

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachinePress(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        burnTime = input.getIntOr("burnTime", burnTime);
        progress = input.getIntOr("progress", progress);
        speed = input.getIntOr("speed", speed);
        isRetracting = input.getBooleanOr("isRetracting", false);
        delay = input.getIntOr("delay", 0);
        preheated = input.getBooleanOr("preheated", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("burnTime", burnTime);
        output.putInt("progress", progress);
        output.putInt("speed", speed);
        output.putBoolean("isRetracting", isRetracting);
        output.putInt("delay", delay);
        output.putBoolean("preheated", preheated);
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.progress);
            case 1 -> output.writeInt(this.speed);
            case 2 -> output.writeInt(this.burnTime);
            case 3 -> writePreview(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readProgress(input);
            case 1 -> this.speed = input.readInt();
            case 2 -> this.burnTime = input.readInt();
            case 3 -> readPreview(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
