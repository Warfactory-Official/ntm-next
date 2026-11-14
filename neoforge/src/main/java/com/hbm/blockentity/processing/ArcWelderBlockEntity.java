/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blockentity.processing;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.port.CapabilityPortProvider;
import com.hbm.capability.port.ItemPort;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.container.ArcWelderMenu;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.recipes.ArcWelderRecipe;
import com.hbm.inventory.recipes.ArcWelderRecipes;
import com.hbm.inventory.recipes.ingredient.IngredientGroup;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.EffectHadronLayout;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class ArcWelderBlockEntity extends FabricationBlockEntity implements CapabilityPortProvider {
    public static final int SLOT_OUTPUT = 3;
    public static final int SLOT_BATTERY = 4;
    public static final int SLOT_FLUID_ID = 5;
    public static final int SLOT_UPGRADE_START = 6;
    public static final int SLOT_UPGRADE_END = 7;
    public static final int SLOT_COUNT = 8;

    private static final int[] INPUTS = {0, 1, 2};
    private static final int[] RECIPE_SLOTS = {0, 1, 2, 5, 6, 7};
    private static final ItemPort RED =
            new ItemPort(
                    new int[] {0, 3},
                    (slot, _) -> slot == 0,
                    (slot, _) -> slot == 3);
    private static final ItemPort YELLOW =
            new ItemPort(
                    new int[] {1, 3},
                    (slot, _) -> slot == 1,
                    (slot, _) -> slot == 3);
    private static final ItemPort GREEN =
            new ItemPort(
                    new int[] {2, 3},
                    (slot, _) -> slot == 2,
                    (slot, _) -> slot == 3);

    public ArcWelderBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.ARC_WELDER.get(),
                pos,
                state,
                SLOT_COUNT,
                24_000,
                RECIPE_SLOTS,
                SLOT_BATTERY,
                SLOT_FLUID_ID,
                SLOT_OUTPUT,
                SLOT_UPGRADE_START);
    }

    @Override
    public void tickServer() {
        tickFabrication();
    }

    @Override
    protected List<ArcWelderRecipe> recipes() {
        return ArcWelderRecipes.INSTANCE.recipes();
    }

    @Override
    protected @Nullable GenericRecipe selectRecipe() {
        for (var row : ArcWelderRecipes.INSTANCE.recipes()) {
            if (IngredientGroup.matches(row.inputItem, this, INPUTS)) return row;
        }
        return null;
    }

    @Override
    protected @Nullable FluidStackNTM recipeFluid() {
        return ((ArcWelderRecipe) recipe).fluid();
    }

    @Override
    protected Item previewItem() {
        return ((ArcWelderRecipe) recipe).output().getItem();
    }

    @Override
    protected ItemStack craftProduct() {
        return ((ArcWelderRecipe) recipe).output();
    }

    @Override
    protected void consumeIngredients() {
        consumeGroup(recipe.inputItem, INPUTS);
    }

    @Override
    protected void emitWorkEffects() {
        var forward = MultiblockCoreBlock.coreFacing(getBlockState());
        double x = worldPosition.getX() + .5 - forward.getStepX() * .5;
        double y = worldPosition.getY() + 1.25;
        double z = worldPosition.getZ() + .5 - forward.getStepZ() * .5;
        if (TickPhase.every(this, 20)) {
            ParticleCreators.sparks(level, x, y, z, 5, false, 25);
        } else if (TickPhase.every(this, 2)) {
            var effect = EffectHadronLayout.encode(x, y, z);
            PacketWire.sendToAllAround(effect, new TargetPoint((ServerLevel) level, x, y, z, 25));
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < SLOT_OUTPUT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return YELLOW.slots();
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public @Nullable ItemPort itemAccess(BlockPos cell, Direction side) {
        if (cell.equals(worldPosition)) return null;
        if (cell.getY() != worldPosition.getY()) return ItemPort.none();
        var forward = MultiblockCoreBlock.coreFacing(getBlockState());
        var right = forward.getClockWise();
        int dx = cell.getX() - worldPosition.getX();
        int dz = cell.getZ() - worldPosition.getZ();
        int along = dx * forward.getStepX() + dz * forward.getStepZ();
        int across = dx * right.getStepX() + dz * right.getStepZ();
        if (along == 0) {
            return switch (across) {
                case -1 -> GREEN;
                case 0 -> YELLOW;
                case 1 -> RED;
                default -> ItemPort.none();
            };
        }
        if (along == -1) {
            return switch (across) {
                case -1 -> RED;
                case 0 -> YELLOW;
                case 1 -> GREEN;
                default -> ItemPort.none();
            };
        }
        return ItemPort.none();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineArcWelder");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ArcWelderMenu(containerId, inventory, this);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x7F;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit == 5) tank.packetSerialize(output);
        else super.writeSyncUnit(unit, output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit == 5) tank.packetDeserialize(input);
        else super.readSyncUnit(unit, input);
    }
}
