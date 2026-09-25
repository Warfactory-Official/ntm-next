// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.api.block.IDroneContainer;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuDroneRequester;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.IControlReceiverFilter;
import com.hbm.tileentity.network.RequestNetwork.Demand;
import com.hbm.tileentity.network.RequestNetwork.PathNode;
import com.hbm.tileentity.network.RequestNetwork.RequestNode;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class BlockEntityDroneRequester extends BlockEntityRequestNetwork
        implements MenuProvider, IControlReceiverFilter, IDroneContainer.Requester, SyncUnitSchema {

    public static final int FILTER_COUNT = 9;
    public static final int SLOT_COUNT = 18;

    private static final int[] SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17};

    private static final double CONTROL_RANGE = 20;

    @SyncField(units = 1L)
    public final ModulePatternMatcher matcher = new ModulePatternMatcher(FILTER_COUNT);

    public BlockEntityDroneRequester(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE_REQUESTER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        super.tickServer();
        networkPackNT(15);
    }

    @Override
    protected PathNode createNode(BlockPos pos) {
        List<Demand> request = new ArrayList<>();

        for (int i = 0; i < FILTER_COUNT; i++) {
            ItemStack filter = inventory.get(i);
            ItemStack stock = inventory.get(i + FILTER_COUNT);
            String mode = matcher.mode(i);
            if (filter.isEmpty() || mode == null) continue;

            if (stock.isEmpty() || !matcher.isValidForFilter(filter, i, stock)) {
                request.add(new Demand(filter.copy(), mode));
            }
        }

        return new RequestNode(pos, reachable, request);
    }

    @Override
    public Container droneDeliveryInventory() {
        return new DeliveryBank(this);
    }

    @Override
    public void nextMode(int i) {
        matcher.nextMode(level, inventory.get(i), i);
    }

    @Override
    public int[] getFilterSlots() {
        return new int[] {0, FILTER_COUNT};
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.position().distanceTo(Vec3.atLowerCornerOf(worldPosition)) < CONTROL_RANGE;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        matcher.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        matcher.save(output);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.droneRequester");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuDroneRequester(containerId, playerInventory, this);
    }

    private record DeliveryBank(BlockEntityDroneRequester requester) implements Container {

        @Override
        public int getContainerSize() {
            return FILTER_COUNT;
        }

        @Override
        public boolean isEmpty() {
            for (int slot = 0; slot < FILTER_COUNT; slot++)
                if (!getItem(slot).isEmpty()) return false;
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return requester.getItem(slot + FILTER_COUNT);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return requester.removeItem(slot + FILTER_COUNT, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return requester.removeItemNoUpdate(slot + FILTER_COUNT);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            requester.setItem(slot + FILTER_COUNT, stack);
        }

        @Override
        public void setChanged() {
            requester.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return requester.stillValid(player);
        }

        @Override
        public void clearContent() {
            for (int slot = 0; slot < FILTER_COUNT; slot++) setItem(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public long syncUnitMask() {
        return 1L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> this.matcher.serialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.matcher.deserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
