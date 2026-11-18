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

package com.hbm.blockentity.machine.thermal;

import com.hbm.blockentity.SmeltingFurnaceBlockEntity;
import com.hbm.blockentity.machine.FireboxBaseBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.FurnaceBrickBlock;
import com.hbm.handler.FuelHandler;
import com.hbm.inventory.container.FurnaceBrickMenu;
import com.hbm.items.ModItems;
import com.hbm.items.machine.AshType;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.OverridePoint;
import com.hbm.util.RegistryUtil;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BrickFurnaceBlockEntity extends SmeltingFurnaceBlockEntity
        implements MenuProvider, SyncUnitSchema {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_ASH = 3;
    public static final int SLOT_COUNT = 4;
    public static final int SMELT_TIME = 200;
    public static final OverridePoint<ToIntFunction<ItemStack>> EXTRA_SPEED =
            OverridePoint.of(RegistryUtil.id("brick_furnace_speed"), _ -> 1);

    private static final int[] TOP = {SLOT_INPUT};
    private static final int[] BOTTOM = {SLOT_OUTPUT, SLOT_FUEL, SLOT_ASH};
    private static final int[] SIDES = {SLOT_FUEL};
    private static final AshType[] ASH_KINDS = {AshType.WOOD, AshType.COAL, AshType.MISC};
    private final int[] ash = new int[3];
    private final SmeltingRecipeCache smelting = new SmeltingRecipeCache();

    public int burnTime;
    public int maxBurnTime;
    public int progress;

    public BrickFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FURNACE_BRICK.get(), pos, state, SLOT_COUNT, SLOT_OUTPUT);
    }

    @Override
    public void tickServer() {
        boolean wasBurning = burnTime > 0;
        int previousBurn = burnTime;
        int previousProgress = progress;
        if (wasBurning) hbm$syncSet$BrickFurnaceBlockEntity$burnTime((int) (burnTime - 1));
        var input = getItem(SLOT_INPUT);
        var fuel = getItem(SLOT_FUEL);
        if (burnTime != 0 || !fuel.isEmpty() && !input.isEmpty()) {
            boolean breathes = breatheAir(wasBurning && TickPhase.every(this, 5) ? 1 : 0);
            var result = smelting.result((ServerLevel) level, input);
            boolean fits = SmeltingRecipeCache.fits(getItem(SLOT_OUTPUT), result, 64);
            if (breathes && burnTime == 0 && fits) {
                hbm$syncSet$BrickFurnaceBlockEntity$burnTime(fuelTime(fuel));
                hbm$syncSet$BrickFurnaceBlockEntity$maxBurnTime(burnTime);
                if (burnTime > 0) {
                    ash[FireboxBaseBlockEntity.getAshFromFuel(fuel).ordinal()] += burnTime;
                    SmeltingRecipeCache.consumeFuel(inventory, SLOT_FUEL);
                    emitAsh();
                    setChanged();
                }
            }
            if (breathes && burnTime > 0 && fits) {
                hbm$syncSet$BrickFurnaceBlockEntity$progress((int) (progress + (speed(input))));
                if (progress >= SMELT_TIME) {
                    hbm$syncSet$BrickFurnaceBlockEntity$progress(0);
                    SmeltingRecipeCache.insert(inventory, SLOT_OUTPUT, result);
                    input.shrink(1);
                    recipesUsed.record(0, smelting.recipe());
                    setChanged();
                }
            } else hbm$syncSet$BrickFurnaceBlockEntity$progress(0);
        }
        if (wasBurning != (burnTime > 0)) {
            level.setBlock(
                    worldPosition,
                    getBlockState().setValue(FurnaceBrickBlock.LIT, burnTime > 0),
                    2);
            setChanged();
        }
        if (previousBurn != burnTime || previousProgress != progress) setChanged();
        networkPackNT(15);
    }

    private int fuelTime(ItemStack fuel) {
        return FuelHandler.getBurnTime(this, fuel);
    }

    public static int speed(ItemStack input) {
        if (input.is(Items.CLAY_BALL)
                || input.is(ModItems.BALL_FIRECLAY.get())
                || input.is(Items.NETHERRACK)) return 4;
        if (input.is(Items.COBBLESTONE)
                || input.is(Items.COBBLED_DEEPSLATE)
                || input.is(ItemTags.LOGS_THAT_BURN)
                || input.is(ItemTags.SMELTS_TO_GLASS)) return 2;
        return EXTRA_SPEED.get().applyAsInt(input);
    }

    private void emitAsh() {
        for (int kind = 0; kind < ASH_KINDS.length; kind++) {
            if (ash[kind] < 2000) continue;
            var item = ModItems.POWDER_ASH.get(ASH_KINDS[kind]);
            var output = getItem(SLOT_ASH);
            if (output.isEmpty()) inventory.set(SLOT_ASH, new ItemStack(item));
            else if (output.is(item) && output.getCount() < output.getMaxStackSize())
                output.grow(1);
            else continue;
            ash[kind] -= 2000;
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? TOP : side == Direction.DOWN ? BOTTOM : SIDES;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT || slot == SLOT_FUEL && FuelHandler.isFuel(this, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT || slot == SLOT_ASH;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.furnaceBrick");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FurnaceBrickMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$BrickFurnaceBlockEntity$burnTime(input.getIntOr("burnTime", burnTime));
        hbm$syncSet$BrickFurnaceBlockEntity$maxBurnTime(input.getIntOr("maxBurn", maxBurnTime));
        hbm$syncSet$BrickFurnaceBlockEntity$progress(input.getIntOr("progress", progress));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("burnTime", burnTime);
        output.putInt("maxBurn", maxBurnTime);
        output.putInt("progress", progress);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(burnTime);
            case 1 -> output.writeInt(maxBurnTime);
            case 2 -> output.writeInt(progress);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$BrickFurnaceBlockEntity$burnTime(input.readInt());
            case 1 -> hbm$syncSet$BrickFurnaceBlockEntity$maxBurnTime(input.readInt());
            case 2 -> hbm$syncSet$BrickFurnaceBlockEntity$progress(input.readInt());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$BrickFurnaceBlockEntity$burnTime(int next) {
        if (this.burnTime != next) {
            this.burnTime = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$BrickFurnaceBlockEntity$maxBurnTime(int next) {
        if (this.maxBurnTime != next) {
            this.maxBurnTime = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    public final void hbm$syncSet$BrickFurnaceBlockEntity$progress(int next) {
        if (this.progress != next) {
            this.progress = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }
}
