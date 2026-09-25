// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuMachineFunnel;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.ItemStates;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityMachineFunnel extends BlockEntityMachineBase
        implements MenuProvider, IControlReceiver, SyncUnitSchema {

    public static final int SLOT_COUNT = 18;

    public static final int MODE_ALL = 0;
    public static final int MODE_3X3 = 1;
    public static final int MODE_2X2 = 2;

    private static final Map<RecipeKey, ItemStack> FROM9_CACHE = new HashMap<>();
    private static final Map<RecipeKey, ItemStack> FROM4_CACHE = new HashMap<>();
    private static final int[] INPUT_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] OUTPUT_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17};

    private final RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> craftingCheck =
            RecipeManager.createCheck(RecipeType.CRAFTING);

    @SyncField(units = 1L << 0)
    public int mode = MODE_ALL;

    public BlockEntityMachineFunnel(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUNNEL.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        boolean changed = false;

        for (int i = 0; i < 9; i++) {
            ItemStack in = inventory.get(i);
            if (in.isEmpty()) continue;

            int stacksize = 9;
            ItemStack compressed = ItemStack.EMPTY;
            if (mode != MODE_2X2 && in.getCount() >= 9) compressed = getFrom9(in);
            if (compressed.isEmpty()) {
                stacksize = 4;
                if (mode != MODE_3X3 && in.getCount() >= 4) compressed = getFrom4(in);
            }
            if (compressed.isEmpty() || in.getCount() < stacksize) continue;

            ItemStack out = inventory.get(i + 9);
            if (out.isEmpty()) {
                inventory.set(i + 9, compressed.copy());
                in.shrink(stacksize);
                changed = true;
            } else if (ItemStack.isSameItemSameComponents(out, compressed)
                    && out.getCount() + compressed.getCount() <= compressed.getMaxStackSize()) {
                out.grow(compressed.getCount());
                in.shrink(stacksize);
                changed = true;
            }
        }

        if (changed) setChanged();
        networkPackNT(15);
    }

    public void cycleMode() {
        mode = (mode + 1) % 3;
        setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        cycleMode();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot > 8) return false;
        if (!inventory.get(slot).isEmpty()) return true;
        return !getFrom9(stack).isEmpty() || !getFrom4(stack).isEmpty();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return OUTPUT_SLOTS;
        return INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (side == Direction.DOWN) return slot > 8;
        if (side == Direction.UP) return false;
        return slot < 9;
    }

    private ItemStack getFrom9(ItemStack ingredient) {
        RecipeKey key = RecipeKey.of(ingredient);
        ItemStack cached = FROM9_CACHE.get(key);
        if (cached != null) return cached;
        ItemStack result = lookupCompress(ingredient, 9);

        if (level instanceof ServerLevel) FROM9_CACHE.put(key, result);
        return result;
    }

    private ItemStack getFrom4(ItemStack ingredient) {
        RecipeKey key = RecipeKey.of(ingredient);
        ItemStack cached = FROM4_CACHE.get(key);
        if (cached != null) return cached;
        ItemStack result = lookupCompress(ingredient, 4);
        if (level instanceof ServerLevel) FROM4_CACHE.put(key, result);
        return result;
    }

    private ItemStack lookupCompress(ItemStack ingredient, int count) {
        if (!(level instanceof ServerLevel server)) return ItemStack.EMPTY;

        List<ItemStack> cells = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) cells.add(ItemStack.EMPTY);
        if (count == 9) {
            for (int i = 0; i < 9; i++) cells.set(i, ingredient.copyWithCount(1));
        } else {
            cells.set(0, ingredient.copyWithCount(1));
            cells.set(1, ingredient.copyWithCount(1));
            cells.set(3, ingredient.copyWithCount(1));
            cells.set(4, ingredient.copyWithCount(1));
        }

        CraftingInput input = CraftingInput.of(3, 3, cells);
        Optional<RecipeHolder<CraftingRecipe>> recipe = craftingCheck.getRecipeFor(input, server);
        return recipe.map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineFunnel");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineFunnel(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mode = input.getIntOr("mode", mode);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("mode", mode);
    }

    private record RecipeKey(Item item, List<Object> state) {
        static RecipeKey of(ItemStack stack) {
            return new RecipeKey(stack.getItem(), ItemStates.key(stack));
        }
    }

    @Override
    public long syncUnitMask() {
        return 1L << 0;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.mode);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.mode = input.readInt();
            default -> throw new IllegalArgumentException();
        }
    }
}
