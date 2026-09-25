// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.blocks.network.CraneBlockBase;
import com.hbm.interfaces.ICopiable;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IControlReceiverFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public abstract class BlockEntityCraneBase extends BlockEntityMachineBase implements ICopiable {

    private static final int PASTE_ORIENTATION = 1;

    protected BlockEntityCraneBase(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    protected int[] droppedBand() {
        return new int[] {0, getContainerSize()};
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null) return;

        int[] band = droppedBand();
        for (int i = band[0]; i < band[1]; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        clearContent();
    }

    public Direction getInputSide() {
        return CraneBlockBase.inputSide(getBlockState());
    }

    public Direction getOutputSide() {
        return CraneBlockBase.outputSide(getBlockState());
    }

    @Override
    public @Nullable CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("inputSide", getInputSide().ordinal());
        nbt.putInt("outputSide", getOutputSide().ordinal());

        if (this instanceof IControlReceiverFilter filter) {
            ListTag tags = new ListTag();
            int count = 0;
            for (int i = filter.getFilterSlots()[0]; i < filter.getFilterSlots()[1]; i++) {
                if (!getItem(i).isEmpty()) {
                    CompoundTag slotNbt = new CompoundTag();
                    slotNbt.putByte("slot", (byte) count);
                    IControlReceiverFilter.writeStack(slotNbt, getItem(i));
                    tags.add(slotNbt);
                }
                count++;
            }
            nbt.put("items", tags);
        }

        return nbt;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        if (index == PASTE_ORIENTATION) {
            BlockState state = getBlockState();
            if (nbt.contains("inputSide")) {
                state =
                        state.setValue(
                                CraneBlockBase.INPUT,
                                Direction.from3DDataValue(nbt.getIntOr("inputSide", 0)));
            }
            if (nbt.contains("outputSide")) {
                state =
                        state.setValue(
                                CraneBlockBase.OUTPUT,
                                Direction.from3DDataValue(nbt.getIntOr("outputSide", 1)));
            }
            level.setBlockAndUpdate(pos, state);
            return;
        }

        if (!(this instanceof IControlReceiverFilter filter)) return;

        ListTag items = nbt.getListOrEmpty("items");
        int listSize = items.size();
        if (listSize == 0) return;

        int count = 0;
        for (int i = filter.getFilterSlots()[0]; i < filter.getFilterSlots()[1]; i++) {
            if (i < listSize) {
                CompoundTag slotNbt = items.getCompoundOrEmpty(count);
                byte slot = slotNbt.getByteOr("slot", (byte) 0);
                ItemStack loaded = IControlReceiverFilter.readFilterStack(slotNbt);
                if (!loaded.isEmpty() && slot < filter.getFilterSlots()[1]) {
                    setItem(slot + filter.getFilterSlots()[0], loaded);
                    filter.nextMode(slot);
                    setChanged();
                }
            }
            count++;
        }
    }

    @Override
    public String[] infoForDisplay(Level level, BlockPos pos) {
        return new String[] {"copytool.filter", "copytool.orientation"};
    }
}
