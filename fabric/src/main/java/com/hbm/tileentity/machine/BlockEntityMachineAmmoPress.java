// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuMachineAmmoPress;
import com.hbm.inventory.recipes.AmmoPressRecipe;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityMachineAmmoPress extends BlockEntityMachineBase
        implements IControlReceiver, MenuProvider, SyncUnitSchema {

    public static final int SLOT_OUTPUT = 9;
    public static final int SLOT_COUNT = 10;
    private static final int[] ALL_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    @SyncField(units = 1L << 0)
    public int selectedRecipe = -1;

    @SyncField(units = 1L << 1)
    public int playAnimation = 0;

    public AnimationState animState = AnimationState.LIFTING;
    public float prevLift, lift, prevPress, press;

    public BlockEntityMachineAmmoPress(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AMMO_PRESS.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineAmmoPress");
    }

    @Override
    public void tickServer() {
        if (playAnimation > 0) playAnimation--;
        performRecipe();
        networkPackNT(25);
    }

    private void performRecipe() {
        if (selectedRecipe < 0 || selectedRecipe >= AmmoPressRecipes.INSTANCE.recipes().size())
            return;
        AmmoPressRecipe recipe = AmmoPressRecipes.INSTANCE.recipes().get(selectedRecipe);

        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (!out.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(out, recipe.output())) return;
            if (out.getCount() + recipe.output().getCount() > out.getMaxStackSize()) return;
        }

        if (hasIngredients(recipe)) {
            produceAmmo(recipe);
            performRecipe();
        }
    }

    private boolean hasIngredients(AmmoPressRecipe recipe) {
        CountIngredient[] input = recipe.input();
        for (int i = 0; i < 9; i++) {
            ItemStack slot = inventory.get(i);
            if (input[i] == null && slot.isEmpty()) continue;
            if (input[i] == null) return false;
            if (slot.isEmpty()) return false;
            if (!input[i].test(slot)) return false;
        }
        return true;
    }

    private void produceAmmo(AmmoPressRecipe recipe) {
        CountIngredient[] input = recipe.input();
        for (int i = 0; i < 9; i++) {
            if (input[i] != null) removeItem(i, input[i].count());
        }
        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (out.isEmpty()) inventory.set(SLOT_OUTPUT, recipe.output().copy());
        else out.grow(recipe.output().getCount());
        playAnimation = 40;
        setChanged();
    }

    @Override
    public void tickClient() {
        prevLift = lift;
        prevPress = press;
        if (playAnimation > 0 || lift > 0) {
            switch (animState) {
                case LIFTING -> {
                    lift += 1F / 40F;
                    if (lift >= 1F) {
                        lift = 1F;
                        animState = AnimationState.PRESSING;
                    }
                }
                case PRESSING -> {
                    press += 1F / 20F;
                    if (press >= 1F) {
                        press = 1F;
                        animState = AnimationState.RETRACTING;
                    }
                }
                case RETRACTING -> {
                    press -= 1F / 20F;
                    if (press <= 0F) {
                        press = 0F;
                        animState = AnimationState.LOWERING;
                    }
                }
                case LOWERING -> {
                    lift -= 1F / 40F;
                    if (lift <= 0F) {
                        lift = 0F;
                        animState = AnimationState.LIFTING;
                    }
                }
            }
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot > 8) return false;
        if (selectedRecipe < 0 || selectedRecipe >= AmmoPressRecipes.INSTANCE.recipes().size())
            return false;
        CountIngredient ci = AmmoPressRecipes.INSTANCE.recipes().get(selectedRecipe).input()[slot];
        return ci != null && ci.matchesItem(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        int newRecipe = data.getIntOr("selection", -1);
        selectedRecipe = newRecipe == selectedRecipe ? -1 : newRecipe;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineAmmoPress(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        selectedRecipe = input.getIntOr("recipe", selectedRecipe);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("recipe", selectedRecipe);
    }

    public enum AnimationState {
        LIFTING,
        PRESSING,
        RETRACTING,
        LOWERING
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.selectedRecipe);
            case 1 -> output.writeInt(this.playAnimation);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.selectedRecipe = input.readInt();
            case 1 -> this.playAnimation = input.readInt();
            default -> throw new IllegalArgumentException();
        }
    }
}
