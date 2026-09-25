// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.MachineFurnaceBrick;
import com.hbm.handler.FuelHandler;
import com.hbm.inventory.container.MenuFurnaceBrick;
import com.hbm.items.ModItems;
import com.hbm.items.machine.EnumAshType;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntitySmeltingFurnace;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityFurnaceBrick extends BlockEntitySmeltingFurnace
        implements MenuProvider, SyncUnitSchema {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_ASH = 3;
    public static final int SLOT_COUNT = 4;

    public static final int SMELT_TIME = 200;
    public static final int ASH_THRESHOLD = 2_000;

    private static final int[] SLOTS_TOP = {SLOT_INPUT};
    private static final int[] SLOTS_BOTTOM = {SLOT_OUTPUT, SLOT_FUEL, SLOT_ASH};
    private static final int[] SLOTS_SIDES = {SLOT_FUEL};

    private static final Map<Item, Integer> BURN_SPEED = new HashMap<>();
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> quickCheck =
            RecipeManager.createCheck(RecipeType.SMELTING);

    @SyncField(units = 1L << 0)
    public int burnTime;

    @SyncField(units = 1L << 1)
    public int maxBurnTime;

    @SyncField(units = 1L << 2)
    public int progress;

    public int ashLevelWood;
    public int ashLevelCoal;
    public int ashLevelMisc;

    public BlockEntityFurnaceBrick(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FURNACE_BRICK.get(), pos, state, SLOT_COUNT, SLOT_OUTPUT);
    }

    private static Map<Item, Integer> burnSpeed() {
        if (BURN_SPEED.isEmpty()) {
            BURN_SPEED.put(Items.CLAY_BALL, 4);
            BURN_SPEED.put(ModItems.BALL_FIRECLAY.get(), 4);
            BURN_SPEED.put(Items.NETHERRACK, 4);
            BURN_SPEED.put(Items.COBBLESTONE, 2);

            BURN_SPEED.put(Items.COBBLED_DEEPSLATE, 2);
        }
        return BURN_SPEED;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.furnaceBrick");
    }

    @Override
    public void tickServer() {
        boolean wasBurning = burnTime > 0;
        boolean markDirty = false;

        if (burnTime > 0) burnTime--;

        if (burnTime != 0
                || (!inventory.get(SLOT_FUEL).isEmpty() && !inventory.get(SLOT_INPUT).isEmpty())) {
            if (burnTime == 0 && canSmelt()) {
                ItemStack fuel = inventory.get(SLOT_FUEL);
                maxBurnTime = burnTime = FuelHandler.getBurnTime(level, fuel);

                if (burnTime > 0) {
                    markDirty = true;

                    EnumAshType type = BlockEntityFireboxBase.getAshFromFuel(fuel);
                    if (type == EnumAshType.WOOD) ashLevelWood += burnTime;
                    if (type == EnumAshType.COAL) ashLevelCoal += burnTime;
                    if (type == EnumAshType.MISC) ashLevelMisc += burnTime;

                    if (tryEmitAsh(ashLevelWood, EnumAshType.WOOD)) ashLevelWood -= ASH_THRESHOLD;
                    if (tryEmitAsh(ashLevelCoal, EnumAshType.COAL)) ashLevelCoal -= ASH_THRESHOLD;
                    if (tryEmitAsh(ashLevelMisc, EnumAshType.MISC)) ashLevelMisc -= ASH_THRESHOLD;

                    ItemStackTemplate remainder = fuel.getCraftingRemainder();
                    fuel.shrink(1);
                    if (fuel.isEmpty())
                        inventory.set(
                                SLOT_FUEL,
                                remainder != null ? remainder.create() : ItemStack.EMPTY);
                }
            }

            if (burnTime > 0 && canSmelt()) {
                progress += getBurnSpeed();
                if (progress >= SMELT_TIME) {
                    progress = 0;
                    smeltItem();
                    markDirty = true;
                }
            } else {
                progress = 0;
            }
        }

        if (wasBurning != burnTime > 0) {
            markDirty = true;
            BlockState state = getBlockState();
            if (state.hasProperty(MachineFurnaceBrick.LIT)
                    && state.getValue(MachineFurnaceBrick.LIT) != burnTime > 0) {
                level.setBlock(
                        worldPosition, state.setValue(MachineFurnaceBrick.LIT, burnTime > 0), 2);
            }
        }

        if (markDirty) setChanged();
        networkPackNT(15);
    }

    private int getBurnSpeed() {
        ItemStack in = inventory.get(SLOT_INPUT);
        Integer speed = burnSpeed().get(in.getItem());
        if (speed != null) return speed;

        if (in.is(ItemTags.LOGS_THAT_BURN) || in.is(ItemTags.SMELTS_TO_GLASS)) return 2;
        return 1;
    }

    private boolean tryEmitAsh(int accumulated, EnumAshType type) {
        if (accumulated < ASH_THRESHOLD) return false;
        ItemStack out = inventory.get(SLOT_ASH);
        if (out.isEmpty()) {
            inventory.set(SLOT_ASH, ModItems.POWDER_ASH.stack(type));
            return true;
        }
        if (ModItems.POWDER_ASH.is(out, type) && out.getCount() < out.getMaxStackSize()) {
            out.grow(1);
            return true;
        }
        return false;
    }

    private @Nullable RecipeHolder<SmeltingRecipe> smeltRecipe() {
        ItemStack in = inventory.get(SLOT_INPUT);
        if (in.isEmpty() || !(level instanceof ServerLevel server)) return null;
        return quickCheck.getRecipeFor(new SingleRecipeInput(in), server).orElse(null);
    }

    private @Nullable ItemStack smeltResult() {
        RecipeHolder<SmeltingRecipe> recipe = smeltRecipe();
        if (recipe == null) return null;
        return recipe.value().assemble(new SingleRecipeInput(inventory.get(SLOT_INPUT)));
    }

    private boolean canSmelt() {
        ItemStack result = smeltResult();
        if (result == null || result.isEmpty()) return false;
        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (out.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(out, result)) return false;
        int sum = out.getCount() + result.getCount();
        return sum <= getMaxStackSize() && sum <= out.getMaxStackSize();
    }

    private void smeltItem() {
        RecipeHolder<SmeltingRecipe> recipe = smeltRecipe();
        if (recipe == null) return;
        ItemStack result =
                recipe.value().assemble(new SingleRecipeInput(inventory.get(SLOT_INPUT)));
        if (result.isEmpty()) return;
        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (out.isEmpty()) inventory.set(SLOT_OUTPUT, result.copy());
        else out.grow(result.getCount());
        inventory.get(SLOT_INPUT).shrink(1);
        recipesUsed.record(0, recipe);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= SLOT_OUTPUT) return false;
        if (slot == SLOT_FUEL) return level != null && FuelHandler.getBurnTime(level, stack) > 0;
        return true;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN
                ? SLOTS_BOTTOM
                : side == Direction.UP ? SLOTS_TOP : SLOTS_SIDES;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_OUTPUT;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getInt("burnTime").ifPresent(v -> burnTime = v);
        input.getInt("maxBurn").ifPresent(v -> maxBurnTime = v);
        input.getInt("progress").ifPresent(v -> progress = v);

        input.getInt("ashWood").ifPresent(v -> ashLevelWood = v);
        input.getInt("ashCoal").ifPresent(v -> ashLevelCoal = v);
        input.getInt("ashMisc").ifPresent(v -> ashLevelMisc = v);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("burnTime", burnTime);
        output.putInt("maxBurn", maxBurnTime);
        output.putInt("progress", progress);
        output.putInt("ashWood", ashLevelWood);
        output.putInt("ashCoal", ashLevelCoal);
        output.putInt("ashMisc", ashLevelMisc);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuFurnaceBrick(containerId, playerInventory, this);
    }

    @Override
    public long syncUnitMask() {
        return 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.burnTime);
            case 1 -> output.writeInt(this.maxBurnTime);
            case 2 -> output.writeInt(this.progress);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.burnTime = input.readInt();
            case 1 -> this.maxBurnTime = input.readInt();
            case 2 -> this.progress = input.readInt();
            default -> throw new IllegalArgumentException();
        }
    }
}
