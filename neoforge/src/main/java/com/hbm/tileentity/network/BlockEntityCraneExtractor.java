// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.NtmContracts;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuCraneExtractor;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.IControlReceiverFilter;
import com.hbm.util.ForeignItems;
import com.hbm.util.InventoryUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockEntityCraneExtractor extends BlockEntityCraneBase
        implements IGUIProvider, IControlReceiverFilter, SyncUnitSchema {

    public static final int FILTER_SLOTS = 9;
    public static final int SLOT_BUFFER = 9;
    public static final int SLOT_BUFFER_END = 18;
    public static final int SLOT_UPGRADE_STACK = 18;
    public static final int SLOT_UPGRADE_EJECTOR = 19;
    public static final int SLOT_COUNT = 20;

    private static final int[] ACCESS = {9, 10, 11, 12, 13, 14, 15, 16, 17};

    @SyncField(units = 1L)
    public final ModulePatternMatcher matcher = new ModulePatternMatcher(FILTER_SLOTS);

    @SyncField(units = 1L << 1)
    public boolean isWhitelist = false;

    @SyncField(units = 1L << 2)
    public boolean maxEject = false;

    public BlockEntityCraneExtractor(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_EXTRACTOR.get(), pos, state, SLOT_COUNT);
    }

    public static int delayFor(int tier) {
        return switch (tier) {
            case 1 -> 10;
            case 2 -> 5;
            case 3 -> 2;
            default -> 20;
        };
    }

    public static int amountFor(int tier) {
        return switch (tier) {
            case 1 -> 4;
            case 2 -> 16;
            case 3 -> 64;
            default -> 1;
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.craneExtractor");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected int[] droppedBand() {
        return new int[] {SLOT_BUFFER, SLOT_COUNT};
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);

        if (level == null || stack.isEmpty()) return;

        boolean seated =
                (slot == SLOT_UPGRADE_EJECTOR && upgradeTier(stack, UpgradeType.EJECTOR) > 0)
                        || (slot == SLOT_UPGRADE_STACK
                                && upgradeTier(stack, UpgradeType.STACK) > 0);

        if (seated) {
            level.playSound(
                    null,
                    worldPosition,
                    ModSounds.UPGRADE_PLUG.get(),
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
        }
    }

    private static int upgradeTier(ItemStack stack, UpgradeType type) {
        return ItemMachineUpgrade.getLevel(stack, type);
    }

    @Override
    public void tickServer() {
        int delay = delayFor(upgradeTier(getItem(SLOT_UPGRADE_EJECTOR), UpgradeType.EJECTOR));

        if (level.getGameTime() % delay == 0 && !level.hasNeighborSignal(worldPosition)) {
            extract(amountFor(upgradeTier(getItem(SLOT_UPGRADE_STACK), UpgradeType.STACK)));
        }

        networkPackNT(15);
    }

    private void extract(int amount) {
        Direction pullSide = getOutputSide();
        Direction pushSide = getInputSide();

        BlockPos pullPos = worldPosition.relative(pullSide);
        BlockPos pushPos = worldPosition.relative(pushSide);

        IConveyorBelt belt = NtmContracts.CONVEYOR_BELT.at(level, pushPos);
        boolean hasSent = pull(pullPos, pullSide, belt, pushSide, pushPos, amount);

        if (!hasSent && belt != null) {
            for (int i = SLOT_BUFFER; i < SLOT_BUFFER_END; i++) {
                ItemStack stack = getItem(i);
                if (stack.isEmpty()) continue;

                int maxTarget = Math.min(amount, stack.getMaxStackSize());
                if (this.maxEject && stack.getCount() < maxTarget) continue;

                int toSend = Math.min(amount, stack.getCount());

                ItemStack taken = stack.copyWithCount(toSend);
                removeItem(i, toSend);
                sendItem(taken, belt, pushSide, pushPos);
                break;
            }
        }
    }

    private void sendItem(
            ItemStack stack, IConveyorBelt belt, Direction pushSide, BlockPos pushPos) {
        Vec3 pos =
                new Vec3(
                        worldPosition.getX() + 0.5 + pushSide.getStepX() * 0.55,
                        worldPosition.getY() + 0.5 + pushSide.getStepY() * 0.55,
                        worldPosition.getZ() + 0.5 + pushSide.getStepZ() * 0.55);
        Vec3 snap = belt.getClosestSnappingPosition(level, pushPos, pos);

        EntityMovingItem moving = new EntityMovingItem(level);
        moving.setItemStack(stack);
        moving.snapTo(snap.x, snap.y, snap.z, 0F, 0F);
        level.addFreshEntity(moving);

        IEnterableBlock enterable = NtmContracts.ENTERABLE.at(level, pushPos);
        if (enterable != null
                && enterable.canItemEnter(level, pushPos, pushSide.getOpposite(), moving)) {
            enterable.onItemEnter(level, pushPos, pushSide.getOpposite(), moving);
            moving.discard();
        }
    }

    private boolean pull(
            BlockPos pullPos,
            Direction pullSide,
            @Nullable IConveyorBelt belt,
            Direction pushSide,
            BlockPos pushPos,
            int amount) {
        Direction face = pullSide.getOpposite();
        ItemStack taken =
                ForeignItems.extract(
                        level,
                        pullPos,
                        face,
                        stack -> {
                            int maxTarget = Math.min(amount, stack.getMaxStackSize());
                            if (this.maxEject && stack.getCount() < maxTarget) return false;
                            return isWhitelist == matchesFilter(stack);
                        },
                        amount);
        if (taken.isEmpty()) return false;

        if (belt != null) {
            sendItem(taken, belt, pushSide, pushPos);
        } else {
            ItemStack left =
                    InventoryUtil.tryAddItemToInventory(
                            inventory, SLOT_BUFFER, SLOT_BUFFER_END - 1, taken);
            if (!left.isEmpty()) ForeignItems.insert(level, pullPos, face, left);
            setChanged();
        }
        return true;
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
    public int[] getSlotsForFace(Direction side) {
        return ACCESS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= SLOT_BUFFER && slot < SLOT_BUFFER_END && isWhitelist == matchesFilter(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_BUFFER && slot < SLOT_BUFFER_END;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuCraneExtractor(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        isWhitelist = input.getBooleanOr("isWhitelist", false);
        maxEject = input.getBooleanOr("maxEject", false);
        matcher.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("isWhitelist", isWhitelist);
        output.putBoolean("maxEject", maxEject);
        matcher.save(output);
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
        if (data.contains("maxEject")) this.maxEject = !this.maxEject;
        if (data.contains("slot")) setFilterContents(data);
        setChanged();
    }

    @Override
    public long syncUnitMask() {
        return 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> this.matcher.serialize(output);
            case 1 -> output.writeBoolean(this.isWhitelist);
            case 2 -> output.writeBoolean(this.maxEject);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.matcher.deserialize(input);
            case 1 -> this.isWhitelist = input.readBoolean();
            case 2 -> this.maxEject = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
