// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuMachineAutocrafter;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IControlReceiverFilter;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineAutocrafter extends BlockEntityMachineBase
        implements IEnergyHandlerMK2, IGUIProvider, IControlReceiverFilter, SyncUnitSchema {

    public static final int SLOT_TEMPLATE_START = 0;
    public static final int SLOT_TEMPLATE_RESULT = 9;
    public static final int SLOT_INGREDIENT_START = 10;
    public static final int SLOT_OUTPUT = 19;
    public static final int SLOT_BATTERY = 20;
    public static final int SLOT_COUNT = 21;

    public static final int GRID_WIDTH = 3;
    public static final int GRID_HEIGHT = 3;
    public static final int GRID_SIZE = GRID_WIDTH * GRID_HEIGHT;

    public static final long CONSUMPTION = 100L;
    public static final long MAX_POWER = CONSUMPTION * 100L;

    public static final int MAX_INGREDIENT_STACK = 4;

    private static final int[] ACCESSIBLE_SLOTS = {10, 11, 12, 13, 14, 15, 16, 17, 18, 19};

    @SyncField(units = 1L << 1)
    public final ModulePatternMatcher matcher = new ModulePatternMatcher(GRID_SIZE);

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 3)
    public int recipeIndex;

    @SyncField(units = 1L << 2)
    public int recipeCount;

    private List<CraftingRecipe> recipes = List.of();

    private boolean recipesStale = true;

    public BlockEntityMachineAutocrafter(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTOCRAFTER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.autocrafter");
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public int[] getFilterSlots() {
        return new int[] {SLOT_TEMPLATE_START, SLOT_TEMPLATE_RESULT};
    }

    @Override
    public void nextMode(int i) {
        matcher.nextMode(level, inventory.get(i), i);
        setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())
                < 400D;
    }

    @Override
    public void setFilterContents(CompoundTag nbt) {
        int slot = nbt.getIntOr("slot", 0);
        if (slot < 0 || slot >= SLOT_TEMPLATE_RESULT) return;
        ItemStack item = IControlReceiverFilter.readFilterStack(nbt);
        setItem(slot, item);
        matcher.initPatternSmart(level, item, slot);
        updateTemplateGrid();
    }

    public CraftingInput templateGrid() {
        return grid(SLOT_TEMPLATE_START);
    }

    public CraftingInput recipeGrid() {
        return grid(SLOT_INGREDIENT_START);
    }

    private CraftingInput grid(int start) {
        List<ItemStack> items = new ArrayList<>(GRID_SIZE);
        for (int i = 0; i < GRID_SIZE; i++) items.add(inventory.get(start + i));
        return CraftingInput.of(GRID_WIDTH, GRID_HEIGHT, items);
    }

    public List<CraftingRecipe> matchingRecipes(CraftingInput grid) {
        MinecraftServer server = level == null ? null : level.getServer();
        if (server == null) return List.of();

        if (grid.isEmpty()) return List.of();

        List<CraftingRecipe> found = new ArrayList<>();
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            if (holder.value() instanceof CraftingRecipe recipe && recipe.matches(grid, level)) {
                found.add(recipe);
            }
        }
        return found;
    }

    public void updateTemplateGrid() {
        if (level == null || level.isClientSide()) return;
        recipes = matchingRecipes(templateGrid());
        recipeCount = recipes.size();
        recipeIndex = 0;
        recipesStale = false;
        refreshTemplateResult();
    }

    public void nextTemplate() {
        if (level == null || level.isClientSide()) return;
        recipeIndex++;
        if (recipeIndex >= recipes.size()) recipeIndex = 0;
        refreshTemplateResult();
    }

    private void refreshTemplateResult() {
        inventory.set(
                SLOT_TEMPLATE_RESULT,
                recipes.isEmpty()
                        ? ItemStack.EMPTY
                        : recipes.get(recipeIndex).assemble(templateGrid()));
        setChanged();
    }

    private void restoreRecipes() {
        recipesStale = false;
        recipes = matchingRecipes(templateGrid());
        recipeCount = recipes.size();
        if (recipeIndex >= recipes.size()) recipeIndex = 0;
        refreshTemplateResult();
    }

    @Override
    public void tickServer() {
        if (recipesStale) restoreRecipes();

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);

        if (!recipes.isEmpty() && power >= CONSUMPTION) {
            CraftingRecipe recipe = recipes.get(recipeIndex);
            CraftingInput grid = recipeGrid();

            if (recipe.matches(grid, level)) {
                ItemStack stack = recipe.assemble(grid);

                if (!stack.isEmpty()) {
                    boolean didCraft = false;
                    ItemStack out = inventory.get(SLOT_OUTPUT);

                    if (out.isEmpty()) {
                        inventory.set(SLOT_OUTPUT, stack.copy());
                        didCraft = true;
                    } else if (ItemStack.isSameItemSameComponents(out, stack)
                            && out.getCount() + stack.getCount() <= out.getMaxStackSize()) {
                        out.grow(stack.getCount());
                        didCraft = true;
                    }

                    if (didCraft) {
                        for (int i = SLOT_INGREDIENT_START; i < SLOT_OUTPUT; i++) {
                            ItemStack ingredient = inventory.get(i);
                            if (ingredient.isEmpty()) continue;

                            ItemStackTemplate remainder = ingredient.getCraftingRemainder();
                            removeItem(i, 1);
                            if (inventory.get(i).isEmpty() && remainder != null)
                                setItem(i, remainder.create());
                        }

                        power -= CONSUMPTION;
                        setChanged();
                    }
                }
            }
        }

        networkPackNT(15);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot == SLOT_OUTPUT) return true;

        if (slot >= SLOT_INGREDIENT_START && slot < SLOT_OUTPUT) {
            int filterSlot = slot - SLOT_INGREDIENT_START;
            ItemStack filter = inventory.get(filterSlot);
            String mode = matcher.mode(filterSlot);
            if (filter.isEmpty() || mode == null || mode.isEmpty()) return true;
            return !matcher.isValidForFilter(filter, filterSlot, stack);
        }

        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.getCount() > 1 && stack.getCraftingRemainder() != null) return false;

        if (slot < SLOT_INGREDIENT_START || slot >= SLOT_OUTPUT) return false;

        int ownFilter = slot - SLOT_INGREDIENT_START;

        if (inventory.get(ownFilter).isEmpty()) return false;

        ItemStack present = inventory.get(slot);
        if (!present.isEmpty() && present.getCount() + stack.getCount() > MAX_INGREDIENT_STACK)
            return false;
        if (stack.getCount() > MAX_INGREDIENT_STACK) return false;

        List<Integer> validSlots = new ArrayList<>();
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack filter = inventory.get(i);
            String mode = matcher.mode(i);
            if (filter.isEmpty() || mode == null || mode.isEmpty()) continue;

            if (matcher.isValidForFilter(filter, i, stack)) {
                validSlots.add(i + SLOT_INGREDIENT_START);

                if (i == ownFilter && present.isEmpty()) return true;
            }
        }

        if (!validSlots.contains(slot)) return false;

        int size = present.getCount();

        for (int i : validSlots) {
            ItemStack valid = inventory.get(i);

            if (valid.isEmpty()) return false;
            if (!ModulePatternMatcher.sameItemAndState(valid, stack)) continue;
            if (valid.getCount() < size) return false;
        }

        return stack.getCraftingRemainder() == null;
    }

    @Override
    public boolean dropsSlot(int slot) {
        return slot >= SLOT_INGREDIENT_START;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        matcher.load(input);
        recipeIndex = input.getIntOr("rec", 0);
        recipesStale = true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        matcher.save(output);
        output.putInt("rec", recipeIndex);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineAutocrafter(containerId, playerInventory, this);
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> this.matcher.serialize(output);
            case 2 -> output.writeInt(this.recipeCount);
            case 3 -> output.writeInt(this.recipeIndex);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.matcher.deserialize(input);
            case 2 -> this.recipeCount = input.readInt();
            case 3 -> this.recipeIndex = input.readInt();
            default -> throw new IllegalArgumentException();
        }
    }
}
