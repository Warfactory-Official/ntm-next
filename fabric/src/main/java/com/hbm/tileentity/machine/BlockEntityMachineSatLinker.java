// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuMachineSatLinker;
import com.hbm.items.ISatChip;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.tileentity.BlockEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineSatLinker extends BlockEntityMachineBase implements IGUIProvider {

    public static final int SLOT_SOURCE = 0;
    public static final int SLOT_TARGET = 1;
    public static final int SLOT_RANDOMIZE = 2;
    public static final int SLOT_COUNT = 3;

    public static final int FREQ_BOUND = 100000;

    private static final int[] ACCESS_TOP = {SLOT_SOURCE};
    private static final int[] ACCESS_BOTTOM = {SLOT_TARGET};
    private static final int[] ACCESS_SIDE = {SLOT_RANDOMIZE};

    private @Nullable ItemStack rolled;

    public BlockEntityMachineSatLinker(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SATLINKER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.satLinker");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level instanceof ServerLevel server) link(server);
    }

    private void link(ServerLevel server) {
        boolean written = false;

        ItemStack source = getItem(SLOT_SOURCE);
        ItemStack target = getItem(SLOT_TARGET);
        if (source.getItem() instanceof ISatChip && target.getItem() instanceof ISatChip) {
            int freq = ISatChip.getFreqS(source);
            if (ISatChip.getFreqS(target) != freq) {
                ISatChip.setFreqS(target, freq);
                written = true;
            }
        }

        ItemStack randomize = getItem(SLOT_RANDOMIZE);
        if (randomize.isEmpty()) {
            rolled = null;
        } else if (randomize != rolled && randomize.getItem() instanceof ISatChip) {
            rolled = randomize;
            SatelliteSavedData satellites = SatelliteSavedData.get(server);
            for (int attempt = 0; attempt < FREQ_BOUND; attempt++) {
                int freq = server.getRandom().nextInt(FREQ_BOUND);
                if (satellites.isFreqTaken(freq)) continue;
                ISatChip.setFreqS(randomize, freq);
                written = true;
                break;
            }
        }

        if (written) super.setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return switch (side) {
            case DOWN -> ACCESS_BOTTOM;
            case UP -> ACCESS_TOP;
            default -> ACCESS_SIDE;
        };
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuMachineSatLinker(containerId, inventory, this);
    }
}
