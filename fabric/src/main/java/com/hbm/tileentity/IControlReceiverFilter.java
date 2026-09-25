// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.api.control.IControlReceiver;
import com.hbm.interfaces.ICopiable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public interface IControlReceiverFilter extends IControlReceiver, ICopiable {

    static void writeStack(CompoundTag nbt, ItemStack stack) {
        nbt.put("stack", ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).getOrThrow());
    }

    static ItemStack readFilterStack(CompoundTag nbt) {
        Tag stack = nbt.get("stack");
        if (stack == null) return ItemStack.EMPTY;
        return ItemStack.CODEC.parse(NbtOps.INSTANCE, stack).result().orElse(ItemStack.EMPTY);
    }

    void nextMode(int i);

    int[] getFilterSlots();

    @Override
    default void receiveControl(CompoundTag data) {
        if (data.contains("slot")) setFilterContents(data);
    }

    default boolean acceptsFilter(ItemStack stack) {
        return true;
    }

    default void setFilterContents(CompoundTag nbt) {
        Container inventory = (Container) this;
        int slot = nbt.getIntOr("slot", 0);

        if (slot < getFilterSlots()[0] || slot >= getFilterSlots()[1]) return;
        ItemStack item = readFilterStack(nbt);
        if (!acceptsFilter(item)) return;
        inventory.setItem(slot, item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(1));
        nextMode(slot);
        ((BlockEntity) this).setChanged();
    }

    @Override
    default @Nullable CompoundTag getSettings(Level level, BlockPos pos) {
        Container inventory = (Container) this;
        CompoundTag nbt = new CompoundTag();
        ListTag tags = new ListTag();
        int count = 0;
        for (int i = getFilterSlots()[0]; i < getFilterSlots()[1]; i++) {
            if (!inventory.getItem(i).isEmpty()) {
                CompoundTag slotNbt = new CompoundTag();
                slotNbt.putByte("slot", (byte) count);
                writeStack(slotNbt, inventory.getItem(i));
                tags.add(slotNbt);
            }
            count++;
        }
        nbt.put("items", tags);
        return nbt;
    }

    @Override
    default void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        Container inventory = (Container) this;
        ListTag items = nbt.getListOrEmpty("items");
        int listSize = items.size();
        if (listSize == 0) return;

        int count = 0;
        for (int i = getFilterSlots()[0]; i < getFilterSlots()[1]; i++) {

            if (i < listSize) {
                CompoundTag slotNbt = items.getCompoundOrEmpty(count);
                byte slot = slotNbt.getByteOr("slot", (byte) 0);
                ItemStack loaded = readFilterStack(slotNbt);

                if (!loaded.isEmpty()
                        && acceptsFilter(loaded)
                        && index < listSize
                        && slot >= 0
                        && slot < getFilterSlots()[1] - getFilterSlots()[0]) {
                    inventory.setItem(slot + getFilterSlots()[0], loaded);
                    nextMode(slot);
                    ((BlockEntity) this).setChanged();
                }
            }
            count++;
        }
    }

    @Override
    default String[] infoForDisplay(Level level, BlockPos pos) {
        return new String[] {"copytool.filter"};
    }
}
