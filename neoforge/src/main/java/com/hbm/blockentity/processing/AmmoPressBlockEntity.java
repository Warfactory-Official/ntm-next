/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.processing;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.AmmoPressMenu;
import com.hbm.inventory.recipes.AmmoPressRecipe;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.Outputs;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class AmmoPressBlockEntity extends MachineBlockEntity
        implements MenuProvider, SyncUnitSchema {
    public static final int SLOT_OUTPUT = 9;
    public static final int SLOT_COUNT = 10;
    private static final int[] AUTOMATION = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    public int selectedRecipe;

    {
        hbm$syncSet$AmmoPressBlockEntity$selectedRecipe(-1);
    }

    public int playAnimation;
    public double press;
    public double prevPress;
    public double lift;
    public double prevLift;
    public AnimationState animState = AnimationState.LIFTING;

    public enum AnimationState {
        LIFTING,
        PRESSING,
        RETRACTING,
        LOWERING
    }

    public AmmoPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AMMO_PRESS.get(), pos, state, SLOT_COUNT);
    }

    public void selectRecipe(int index) {
        int next = selectedRecipe == index ? -1 : index;
        if (next != selectedRecipe) {
            hbm$syncSet$AmmoPressBlockEntity$selectedRecipe(next);
            setChanged();
        }
    }

    private @Nullable AmmoPressRecipe selected() {
        return selectedRecipe < 0 ? null : AmmoPressRecipes.INSTANCE.recipes().get(selectedRecipe);
    }

    private boolean matches(CountIngredient[] cells) {
        for (int slot = 0; slot < SLOT_OUTPUT; slot++) {
            var required = cells[slot];
            var stack = inventory.get(slot);
            if (required == null ? !stack.isEmpty() : !required.test(stack)) return false;
        }
        return true;
    }

    public boolean canProcess() {
        var row = selected();
        return row != null
                && matches(row.input())
                && Outputs.fits(inventory.get(SLOT_OUTPUT), row.outputItems()[0]);
    }

    @Override
    public void tickServer() {
        if (playAnimation > 0)
            hbm$syncSet$AmmoPressBlockEntity$playAnimation((int) (playAnimation - 1));
        var row = selected();
        if (row != null) {
            var cells = row.input();
            var outputs = row.outputItems()[0];
            while (matches(cells) && Outputs.fits(inventory.get(SLOT_OUTPUT), outputs)) {
                var product = row.output();
                for (int slot = 0; slot < SLOT_OUTPUT; slot++) {
                    var required = cells[slot];
                    if (required != null) inventory.get(slot).shrink(required.count());
                }
                var stored = inventory.get(SLOT_OUTPUT);
                if (stored.isEmpty()) inventory.set(SLOT_OUTPUT, product.copy());
                else stored.grow(product.getCount());
                hbm$syncSet$AmmoPressBlockEntity$playAnimation(40);
                setChanged();
            }
        }
        networkPackNT(25);
    }

    @Override
    public void tickClient() {
        prevPress = press;
        prevLift = lift;
        if (playAnimation > 0 || lift > 0) {
            switch (animState) {
                case LIFTING -> {
                    lift = Math.min(1D, lift + 1D / 40D);
                    if (lift == 1D) animState = AnimationState.PRESSING;
                }
                case PRESSING -> {
                    press = Math.min(1D, press + 1D / 20D);
                    if (press == 1D) animState = AnimationState.RETRACTING;
                }
                case RETRACTING -> {
                    press = Math.max(0D, press - 1D / 20D);
                    if (press == 0D) animState = AnimationState.LOWERING;
                }
                case LOWERING -> {
                    lift = Math.max(0D, lift - 1D / 40D);
                    if (lift == 0D) animState = AnimationState.LIFTING;
                }
            }
        }
        if (playAnimation > 0)
            hbm$syncSet$AmmoPressBlockEntity$playAnimation((int) (playAnimation - 1));
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_OUTPUT) return false;
        var row = selected();
        if (row == null) return false;
        var ingredient = row.input()[slot];
        return ingredient != null && ingredient.matchesItem(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineAmmoPress");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new AmmoPressMenu(id, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$AmmoPressBlockEntity$selectedRecipe(input.getIntOr("recipe", selectedRecipe));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("recipe", selectedRecipe);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 3;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(selectedRecipe);
            case 1 -> output.writeInt(playAnimation);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$AmmoPressBlockEntity$selectedRecipe(input.readInt());
            case 1 -> hbm$syncSet$AmmoPressBlockEntity$playAnimation(input.readInt());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$AmmoPressBlockEntity$selectedRecipe(int next) {
        if (this.selectedRecipe != next) {
            this.selectedRecipe = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$AmmoPressBlockEntity$playAnimation(int next) {
        if (this.playAnimation != next) {
            this.playAnimation = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }
}
