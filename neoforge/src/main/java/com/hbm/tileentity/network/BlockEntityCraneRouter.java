// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuCraneRouter;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IControlReceiverFilter;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityCraneRouter extends BlockEntityMachineBase
        implements IGUIProvider, IControlReceiverFilter, SyncUnitSchema {

    public static final int SIDES = 6;
    public static final int FILTERS_PER_SIDE = 5;
    public static final int SLOT_COUNT = SIDES * FILTERS_PER_SIDE;

    public static final int MODE_NONE = 0;
    public static final int MODE_WHITELIST = 1;
    public static final int MODE_BLACKLIST = 2;
    public static final int MODE_WILDCARD = 3;
    public static final int MODES = 4;

    @SyncField(units = 1L)
    public final ModulePatternMatcher[] patterns = new ModulePatternMatcher[SIDES];

    @SyncField(units = 1L << 1)
    public int[] modes = new int[SIDES];

    public BlockEntityCraneRouter(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_ROUTER.get(), pos, state, SLOT_COUNT);
        for (int i = 0; i < patterns.length; i++)
            patterns[i] = new ModulePatternMatcher(FILTERS_PER_SIDE);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.craneRouter");
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {}

    @Override
    public void nextMode(int index) {
        patterns[index / FILTERS_PER_SIDE].nextMode(
                level, getItem(index), index % FILTERS_PER_SIDE);
        networkPackNT(15);
    }

    public void initPattern(ItemStack stack, int index) {
        patterns[index / FILTERS_PER_SIDE].initPatternSmart(level, stack, index % FILTERS_PER_SIDE);
        networkPackNT(15);
    }

    @Override
    public int[] getFilterSlots() {
        return new int[] {0, SLOT_COUNT};
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuCraneRouter(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < patterns.length; i++) {
            int index = i;
            input.child("pattern" + i).ifPresent(child -> patterns[index].load(child));
        }
        int[] loaded = input.getIntArray("modes").orElse(null);
        if (loaded != null && loaded.length == SIDES) modes = loaded;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < patterns.length; i++) patterns[i].save(output.child("pattern" + i));
        output.putIntArray("modes", modes);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())
                < 400D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) {
            int i = data.getIntOr("toggle", 0);
            modes[i] = (modes[i] + 1) % MODES;
        }
        if (data.contains("slot")) setFilterContents(data);
        networkPackNT(15);
    }

    @Override
    public @Nullable CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        ListTag tags = new ListTag();

        int count = 0;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!getItem(i).isEmpty()) {
                CompoundTag slotNbt = new CompoundTag();
                slotNbt.putByte("slot", (byte) count);
                IControlReceiverFilter.writeStack(slotNbt, getItem(i));
                tags.add(slotNbt);
            }
            count++;
        }

        nbt.put("items", tags);
        nbt.putIntArray("modes", modes);
        return nbt;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        ListTag items = nbt.getListOrEmpty("items");

        if (items.isEmpty() || !nbt.contains("modes")) {
            IControlReceiverFilter.super.pasteSettings(nbt, index, level, player, pos);
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            CompoundTag slotNbt = items.getCompoundOrEmpty(i);
            byte slot = slotNbt.getByteOr("slot", (byte) 0);
            ItemStack loaded = IControlReceiverFilter.readFilterStack(slotNbt);

            if (!loaded.isEmpty()
                    && slot > index * FILTERS_PER_SIDE
                    && slot < Math.min(index * FILTERS_PER_SIDE + FILTERS_PER_SIDE, SLOT_COUNT)) {
                setItem(slot, loaded);
                nextMode(slot);
                setChanged();
            }
        }

        int[] loaded = nbt.getIntArray("modes").orElse(null);
        if (loaded != null && loaded.length == SIDES) modes = loaded;
        networkPackNT(15);
    }

    @Override
    public String[] infoForDisplay(Level level, BlockPos pos) {
        String[] options = new String[patterns.length];
        for (int i = 0; i < options.length; i++) options[i] = "copytool.pattern" + i;
        return options;
    }

    private void writePatterns(ByteBuf output) {
        for (ModulePatternMatcher pattern : patterns) pattern.serialize(output);
    }

    private void readPatterns(ByteBuf input) {
        for (ModulePatternMatcher pattern : patterns) pattern.deserialize(input);
    }

    private void writeModes(ByteBuf output) {
        for (int mode : modes) output.writeInt(mode);
    }

    private void readModes(ByteBuf input) {
        for (int i = 0; i < modes.length; i++) modes[i] = input.readInt();
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writePatterns(output);
            case 1 -> writeModes(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readPatterns(input);
            case 1 -> readModes(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
