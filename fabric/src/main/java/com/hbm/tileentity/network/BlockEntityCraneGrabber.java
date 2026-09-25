// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.CraneInserter;
import com.hbm.capability.NtmContracts;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuCraneGrabber;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.IControlReceiverFilter;
import com.hbm.util.InventoryUtil;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BlockEntityCraneGrabber extends BlockEntityCraneBase
        implements IGUIProvider, IControlReceiverFilter, SyncUnitSchema {

    public static final int FILTER_SLOTS = 9;
    public static final int SLOT_UPGRADE_STACK = 9;
    public static final int SLOT_UPGRADE_EJECTOR = 10;
    public static final int SLOT_COUNT = 11;

    private static final double WINDOW_MIN = 0.1875D;
    private static final double WINDOW_MAX = 0.8125D;

    @SyncField(units = 1L)
    public final ModulePatternMatcher matcher = new ModulePatternMatcher(FILTER_SLOTS);

    @SyncField(units = 1L << 1)
    public boolean isWhitelist = false;

    public long lastGrabbedTick = 0;

    public BlockEntityCraneGrabber(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_GRABBER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.craneGrabber");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected int[] droppedBand() {
        return new int[] {SLOT_UPGRADE_STACK, SLOT_COUNT};
    }

    private static int upgradeTier(ItemStack stack, UpgradeType type) {
        return ItemMachineUpgrade.getLevel(stack, type);
    }

    @Override
    public void tickServer() {
        int delay =
                BlockEntityCraneExtractor.delayFor(
                        upgradeTier(getItem(SLOT_UPGRADE_EJECTOR), UpgradeType.EJECTOR));

        if (level.getGameTime() >= lastGrabbedTick + delay
                && !level.hasNeighborSignal(worldPosition)) {
            grab(
                    BlockEntityCraneExtractor.amountFor(
                            upgradeTier(getItem(SLOT_UPGRADE_STACK), UpgradeType.STACK)));
        }

        networkPackNT(15);
    }

    private AABB grabWindow(Direction input) {
        double reach = 1D;

        if (input.getAxis().isHorizontal()) {
            Block front = level.getBlockState(worldPosition.relative(input)).getBlock();
            if (front == ModBlocks.CONVEYOR_DOUBLE.get()) reach = 0.5D;
            if (front == ModBlocks.CONVEYOR_TRIPLE.get()) reach = 0.33D;
        }

        double x = worldPosition.getX() + input.getStepX() * reach;
        double y = worldPosition.getY() + input.getStepY() * reach;
        double z = worldPosition.getZ() + input.getStepZ() * reach;

        return new AABB(
                x + WINDOW_MIN,
                y + WINDOW_MIN,
                z + WINDOW_MIN,
                x + WINDOW_MAX,
                y + WINDOW_MAX,
                z + WINDOW_MAX);
    }

    private void grab(int amount) {
        Direction input = getInputSide();
        Direction output = getOutputSide();
        BlockPos outputPos = worldPosition.relative(output);

        IConveyorBelt belt = NtmContracts.CONVEYOR_BELT.at(level, outputPos);
        List<EntityMovingItem> riders =
                level.getEntitiesOfClass(EntityMovingItem.class, grabWindow(input));

        if (belt != null) {
            for (EntityMovingItem item : riders) {
                if (item.isRemoved()) continue;
                if (!accepts(item.getItemStack())) continue;

                lastGrabbedTick = level.getGameTime();

                Vec3 pos =
                        new Vec3(
                                worldPosition.getX() + 0.5 + output.getStepX() * 0.55,
                                worldPosition.getY() + 0.5 + output.getStepY() * 0.55,
                                worldPosition.getZ() + 0.5 + output.getStepZ() * 0.55);
                Vec3 snap = belt.getClosestSnappingPosition(level, outputPos, pos);

                EntityMovingItem moved = new EntityMovingItem(level);
                moved.setItemStack(item.getItemStack().copy());
                moved.snapTo(snap.x, snap.y, snap.z, 0F, 0F);
                item.discard();
                level.addFreshEntity(moved);
                return;
            }
            return;
        }

        if (!InventoryUtil.inventoryAt(level, outputPos, output.getOpposite())) return;

        for (EntityMovingItem item : riders) {
            ItemStack stack = item.getItemStack();
            if (!accepts(stack)) continue;

            lastGrabbedTick = level.getGameTime();

            int toAdd = Math.min(stack.getCount(), amount);
            ItemStack left =
                    InventoryUtil.insertAt(
                            level, outputPos, output.getOpposite(), stack.copyWithCount(toAdd));
            int didAdd = toAdd - left.getCount();

            ItemStack remaining = stack.copyWithCount(stack.getCount() - didAdd);

            if (remaining.isEmpty()) item.discard();
            else item.setItemStack(remaining);

            amount -= didAdd;
            if (amount <= 0) return;
        }
    }

    private boolean accepts(ItemStack stack) {
        return isWhitelist == matchesFilter(stack);
    }

    public boolean matchesFilter(ItemStack stack) {
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack filter = getItem(i);
            if (!filter.isEmpty() && matcher.isValidForFilter(filter, i, stack)) return true;
        }
        return false;
    }

    @Override
    public void nextMode(int i) {
        matcher.nextMode(level, getItem(i), i);
    }

    @Override
    public int[] getFilterSlots() {
        return new int[] {0, FILTER_SLOTS};
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuCraneGrabber(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        isWhitelist = input.getBooleanOr("isWhitelist", false);
        matcher.load(input);
        lastGrabbedTick = input.getLongOr("lastGrabbedTick", 0L);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("isWhitelist", isWhitelist);
        matcher.save(output);
        output.putLong("lastGrabbedTick", lastGrabbedTick);
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
        if (data.contains("whitelist")) this.isWhitelist = !this.isWhitelist;
        if (data.contains("slot")) setFilterContents(data);
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> this.matcher.serialize(output);
            case 1 -> output.writeBoolean(this.isWhitelist);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.matcher.deserialize(input);
            case 1 -> this.isWhitelist = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
