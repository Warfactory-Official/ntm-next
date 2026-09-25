// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.packet.SyncField;
import com.hbm.packet.SyncInventory;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.Nameable;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class BlockEntityMachineBase extends BlockEntity
        implements Synced,
                GraphResident,
                FoldedCoreResident,
                WorldlyContainer,
                Nameable,
                RandomizableContainer {

    private static final int[] NO_SLOTS = new int[0];
    protected static final int MUFFLE_UNIT = Long.SIZE - 2;
    private static final long MUFFLE_MASK = 1L << MUFFLE_UNIT;

    @SyncField public final NonNullList<ItemStack> inventory;

    @SyncField(units = MUFFLE_MASK)
    private boolean muffled;

    private @Nullable Component customName;
    private @Nullable ResourceKey<LootTable> lootTable;
    private long lootTableSeed;

    protected BlockEntityMachineBase(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state);
        this.inventory = SyncInventory.create(this, slots);
    }

    public int getComparatorPower() {
        return 0;
    }

    public boolean isMuffled() {
        return muffled;
    }

    public void setMuffled() {
        if (muffled) return;
        muffled = true;
        setChanged();
        if (this instanceof SyncUnitSchema schema && (schema.syncUnitMask() & MUFFLE_MASK) != 0) {
            syncToTracking();
        }
    }

    protected boolean syncMuffled() {
        return this instanceof AudioLoop;
    }

    public long syncUnitMask() {
        return syncMuffled() ? MUFFLE_MASK : 0;
    }

    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit != MUFFLE_UNIT || !syncMuffled()) throw new IllegalArgumentException();
        output.writeBoolean(muffled);
    }

    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit != MUFFLE_UNIT || !syncMuffled()) throw new IllegalArgumentException();
        muffled = input.readBoolean();
    }

    public boolean dropsSlot(int slot) {
        if (!(this instanceof IControlReceiverFilter filter)) return true;
        int[] range = filter.getFilterSlots();
        return slot < range[0] || slot >= range[1];
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        spillInventory(pos);
    }

    public void spillInventory(BlockPos pos) {
        if (level == null) return;

        unpackLootTable(null);
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.get(slot);
            if (stack.isEmpty() || !dropsSlot(slot)) continue;
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
    }

    public void tickServer() {}

    public void tickClient() {}

    @Override
    public int getContainerSize() {
        return inventory.size();
    }

    @Override
    public @Nullable ResourceKey<LootTable> getLootTable() {
        return lootTable;
    }

    @Override
    public void setLootTable(@Nullable ResourceKey<LootTable> lootTable) {
        this.lootTable = lootTable;
    }

    @Override
    public long getLootTableSeed() {
        return lootTableSeed;
    }

    @Override
    public void setLootTableSeed(long lootTableSeed) {
        this.lootTableSeed = lootTableSeed;
    }

    @Override
    public boolean isEmpty() {
        unpackLootTable(null);
        for (ItemStack stack : inventory) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        unpackLootTable(null);
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        unpackLootTable(null);
        ItemStack removed = ContainerHelper.removeItem(inventory, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        unpackLootTable(null);
        return ContainerHelper.takeItem(inventory, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        unpackLootTable(null);
        inventory.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    protected double interactionRangeSq() {
        return 12D * 12D;
    }

    @Override
    public boolean stillValid(Player player) {
        if (level.getBlockEntity(worldPosition) != this) return false;
        return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(worldPosition))
                <= interactionRangeSq();
    }

    @Override
    public void clearContent() {
        inventory.clear();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return NO_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public Component getName() {
        return customName != null ? customName : getDefaultName();
    }

    @Override
    public @Nullable Component getCustomName() {
        return customName;
    }

    public void setCustomName(@Nullable Component name) {
        this.customName = name;
    }

    @Override
    public boolean hasCustomName() {
        return customName != null;
    }

    protected Component getDefaultName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        if (!tryLoadLootTable(input)) {
            input.child("inventory").ifPresent(in -> ContainerHelper.loadAllItems(in, inventory));
        }
        customName = input.read("customName", ComponentSerialization.CODEC).orElse(null);
        muffled = input.getBooleanOr("muffled", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!trySaveLootTable(output))
            ContainerHelper.saveAllItems(output.child("inventory"), inventory);
        if (customName != null)
            output.store("customName", ComponentSerialization.CODEC, customName);
        if (muffled) output.putBoolean("muffled", true);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (lootTable != null) {
            components.set(
                    DataComponents.CONTAINER_LOOT,
                    new SeededContainerLoot(lootTable, lootTableSeed));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        SeededContainerLoot loot = components.get(DataComponents.CONTAINER_LOOT);
        if (loot != null) {
            lootTable = loot.lootTable();
            lootTableSeed = loot.seed();
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard(RandomizableContainer.LOOT_TABLE_TAG);
        output.discard(RandomizableContainer.LOOT_TABLE_SEED_TAG);
    }
}
