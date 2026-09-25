// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.NtmContracts;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuCraneUnboxer;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BlockEntityCraneUnboxer extends BlockEntityCraneBase implements IGUIProvider {

    public static final int BUFFER_SLOTS = 21;
    public static final int SLOT_UPGRADE_STACK = 21;
    public static final int SLOT_UPGRADE_EJECTOR = 22;
    public static final int SLOT_COUNT = 23;

    private static final int[] ACCESS = accessBuffer();

    public BlockEntityCraneUnboxer(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_UNBOXER.get(), pos, state, SLOT_COUNT);
    }

    private static int[] accessBuffer() {
        int[] access = new int[BUFFER_SLOTS];
        for (int i = 0; i < BUFFER_SLOTS; i++) access[i] = i;
        return access;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.craneUnboxer");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    private static int upgradeTier(ItemStack stack, UpgradeType type) {
        return ItemMachineUpgrade.getLevel(stack, type);
    }

    @Override
    public void tickServer() {
        int delay =
                BlockEntityCraneExtractor.delayFor(
                        upgradeTier(getItem(SLOT_UPGRADE_EJECTOR), UpgradeType.EJECTOR));

        if (level.getGameTime() % delay != 0 || level.hasNeighborSignal(worldPosition)) return;

        int amount =
                BlockEntityCraneExtractor.amountFor(
                        upgradeTier(getItem(SLOT_UPGRADE_STACK), UpgradeType.STACK));

        Direction output = getInputSide();
        BlockPos outputPos = worldPosition.relative(output);
        IConveyorBelt belt = NtmContracts.CONVEYOR_BELT.at(level, outputPos);
        if (belt == null) return;

        for (int i = 0; i < BUFFER_SLOTS; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;

            int toSend = Math.min(amount, stack.getCount());
            ItemStack sent = stack.copyWithCount(toSend);
            removeItem(i, toSend);

            Vec3 pos =
                    new Vec3(
                            worldPosition.getX() + 0.5 + output.getStepX() * 0.55,
                            worldPosition.getY() + 0.5 + output.getStepY() * 0.55,
                            worldPosition.getZ() + 0.5 + output.getStepZ() * 0.55);
            Vec3 snap = belt.getClosestSnappingPosition(level, outputPos, pos);

            EntityMovingItem moving = new EntityMovingItem(level);
            moving.setItemStack(sent);
            moving.snapTo(snap.x, snap.y, snap.z, 0F, 0F);
            level.addFreshEntity(moving);
            return;
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public int getComparatorPower() {
        return AbstractContainerMenu.getRedstoneSignalFromContainer(this);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuCraneUnboxer(containerId, inventory, this);
    }
}
