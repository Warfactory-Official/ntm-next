// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachineShredder;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.machine.ItemBlades;
import com.hbm.tileentity.Audible;
import com.hbm.tileentity.BlockEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
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

public class BlockEntityMachineShredder extends BlockEntityMachineBase
        implements Audible, IEnergyHandlerMK2, MenuProvider {

    public static final int SLOT_INPUT_START = 0;
    public static final int SLOT_INPUT_END = 9;
    public static final int SLOT_OUTPUT_START = 9;
    public static final int SLOT_OUTPUT_END = 27;
    public static final int SLOT_BLADE_LEFT = 27;
    public static final int SLOT_BLADE_RIGHT = 28;
    public static final int SLOT_BATTERY = 29;
    public static final int SLOT_COUNT = 30;

    public static final long MAX_POWER = 10_000L;
    public static final int PROCESSING_SPEED = 60;
    public static final long POWER_PER_TICK = 5L;

    public static final int SOUND_CYCLE = 50;

    private static final int[] ACCESSIBLE_SLOTS = accessibleSlots();

    @ContainerSync public long power;
    @ContainerSync public int progress;
    public int soundCycle;

    public BlockEntityMachineShredder(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_SHREDDER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected double interactionRangeSq() {
        return 64;
    }

    private static int[] accessibleSlots() {
        int[] slots = new int[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) slots[i] = i;
        return slots;
    }

    public static int gearOf(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemBlades)) return 0;
        if (stack.getMaxDamage() == 0) return 1;
        if (stack.getDamageValue() < stack.getMaxDamage() / 2) return 1;
        return stack.getDamageValue() != stack.getMaxDamage() ? 2 : 3;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = p;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    public int getGearLeft() {
        return gearOf(inventory.get(SLOT_BLADE_LEFT));
    }

    public int getGearRight() {
        return gearOf(inventory.get(SLOT_BLADE_RIGHT));
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < SLOT_INPUT_END) return !(stack.getItem() instanceof ItemBlades);
        if (slot == SLOT_BATTERY) return IBatteryItem.isBattery(stack);
        if (slot == SLOT_BLADE_LEFT || slot == SLOT_BLADE_RIGHT)
            return stack.getItem() instanceof ItemBlades;
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot >= SLOT_INPUT_END && slot != SLOT_BLADE_LEFT && slot != SLOT_BLADE_RIGHT)
            return false;
        if (!canPlaceItem(slot, stack)) return false;

        ItemStack target = inventory.get(slot);
        if (target.isEmpty()) return true;

        int size = target.getCount();
        for (int i = SLOT_INPUT_START; i < SLOT_INPUT_END; i++) {
            ItemStack other = inventory.get(i);
            if (other.isEmpty()) return false;
            if (ItemStack.isSameItemSameComponents(other, stack) && other.getCount() < size)
                return false;
        }

        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot >= SLOT_OUTPUT_START && slot < SLOT_OUTPUT_END) return true;

        return (slot == SLOT_BLADE_LEFT || slot == SLOT_BLADE_RIGHT)
                && stack.getMaxDamage() > 0
                && stack.getDamageValue() == stack.getMaxDamage();
    }

    public boolean hasPower() {
        return power > 0;
    }

    public boolean isProcessing() {
        return progress > 0;
    }

    @Override
    public void tickServer() {
        long prevPower = power;
        int prevProgress = progress;
        boolean crafted = false;

        if (progress == 0) soundCycle = 0;

        if (hasPower() && canProcess()) {
            progress++;
            power = Math.max(0L, power - POWER_PER_TICK);

            if (progress == PROCESSING_SPEED) {
                damageBlades();
                progress = 0;
                processItem();
                crafted = true;
            }

            if (soundCycle == 0 && level != null) {
                level.playSound(
                        null,
                        worldPosition,
                        SoundEvents.MINECART_RIDING,
                        SoundSource.BLOCKS,
                        getVolume(1.0F),
                        0.75F);
            }
            soundCycle++;
            if (soundCycle >= SOUND_CYCLE) soundCycle = 0;
        } else {
            progress = 0;
        }

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);

        if (crafted || power != prevPower || progress != prevProgress) setChanged();
    }

    private void damageBlades() {
        for (int slot = SLOT_BLADE_LEFT; slot <= SLOT_BLADE_RIGHT; slot++) {
            ItemStack blade = inventory.get(slot);
            if (blade.getMaxDamage() > 0) blade.setDamageValue(blade.getDamageValue() + 1);
        }
    }

    public void processItem() {
        for (int inputSlot = SLOT_INPUT_START; inputSlot < SLOT_INPUT_END; inputSlot++) {
            ItemStack input = inventory.get(inputSlot);
            if (input.isEmpty() || !hasSpace(input)) continue;

            ItemStack result = ShredderRecipes.getShredderResult(input);
            boolean merged = false;

            for (int outputSlot = SLOT_OUTPUT_START; outputSlot < SLOT_OUTPUT_END; outputSlot++) {
                ItemStack output = inventory.get(outputSlot);
                if (!output.isEmpty()
                        && ItemStack.isSameItemSameComponents(output, result)
                        && output.getCount() + result.getCount() <= result.getMaxStackSize()) {
                    output.grow(result.getCount());
                    input.shrink(1);
                    merged = true;
                    break;
                }
            }

            if (!merged) {
                for (int outputSlot = SLOT_OUTPUT_START;
                        outputSlot < SLOT_OUTPUT_END;
                        outputSlot++) {
                    if (inventory.get(outputSlot).isEmpty()) {
                        inventory.set(outputSlot, result.copy());
                        input.shrink(1);
                        break;
                    }
                }
            }
        }
    }

    public boolean canProcess() {
        int left = getGearLeft();
        int right = getGearRight();
        if (left <= 0 || left >= 3 || right <= 0 || right >= 3) return false;

        for (int slot = SLOT_INPUT_START; slot < SLOT_INPUT_END; slot++) {
            ItemStack input = inventory.get(slot);
            if (!input.isEmpty() && hasSpace(input)) return true;
        }

        return false;
    }

    public boolean hasSpace(ItemStack stack) {
        ItemStack result = ShredderRecipes.getShredderResult(stack);
        if (result.isEmpty()) return false;

        for (int slot = SLOT_OUTPUT_START; slot < SLOT_OUTPUT_END; slot++) {
            ItemStack output = inventory.get(slot);
            if (output.isEmpty()) return true;

            if (output.getItem() == result.getItem()
                    && output.getCount() + result.getCount() <= result.getMaxStackSize()) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineShredder");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineShredder(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("powerTime", 0L);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("powerTime", power);
    }
}
