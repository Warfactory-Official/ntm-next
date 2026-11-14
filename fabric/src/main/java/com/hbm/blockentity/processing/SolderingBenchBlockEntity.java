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
import com.hbm.inventory.container.SolderingStationMenu;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.recipes.SolderingRecipe;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class SolderingBenchBlockEntity extends FabricationBlockEntity {
    public static final int SLOT_TOPPING_START = 0;
    public static final int SLOT_PCB_START = 3;
    public static final int SLOT_SOLDER = 5;
    public static final int SLOT_OUTPUT = 6;
    public static final int SLOT_BATTERY = 7;
    public static final int SLOT_FLUID_ID = 8;
    public static final int SLOT_UPGRADE_START = 9;
    public static final int SLOT_UPGRADE_END = 10;
    public static final int SLOT_COUNT = 11;

    private static final int[] TOPPINGS = {0, 1, 2};
    private static final int[] BOARDS = {3, 4};
    private static final int[] SOLDER = {5};
    private static final int[] AUTOMATION = {0, 1, 2, 3, 4, 5, 6};
    private static final int[] RECIPE_SLOTS = {0, 1, 2, 3, 4, 5, 8, 9, 10};

    public boolean collisionPrevention;

    public SolderingBenchBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.SOLDERING_STATION.get(),
                pos,
                state,
                SLOT_COUNT,
                8_000,
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
    protected List<SolderingRecipe> recipes() {
        return SolderingRecipes.INSTANCE.recipes();
    }

    @Override
    protected @Nullable GenericRecipe selectRecipe() {
        return SolderingRecipes.INSTANCE.getRecipe(this, TOPPINGS, BOARDS, SLOT_SOLDER);
    }

    @Override
    protected @Nullable FluidStackNTM recipeFluid() {
        return ((SolderingRecipe) recipe).fluid();
    }

    @Override
    protected Item previewItem() {
        return ((SolderingRecipe) recipe).previewItem();
    }

    @Override
    protected ItemStack craftProduct() {
        return ((SolderingRecipe) recipe).output();
    }

    @Override
    protected void consumeIngredients() {
        var row = (SolderingRecipe) recipe;
        consumeGroup(row.toppings(), TOPPINGS);
        consumeGroup(row.pcb(), BOARDS);
        consumeGroup(row.solder(), SOLDER);
    }

    @Override
    protected boolean allowsFluidFreeWork() {
        return !collisionPrevention || tank.getFill() == 0;
    }

    @Override
    protected void emitWorkEffects() {
        if (!TickPhase.every(this, 20)) return;
        var forward = MultiblockCoreBlock.coreFacing(getBlockState());
        var right = forward.getClockWise();
        ParticleCreators.sparks(
                level,
                worldPosition.getX() + .5 - forward.getStepX() * .5 + right.getStepX() * .5,
                worldPosition.getY() + 1.125,
                worldPosition.getZ() + .5 - forward.getStepZ() * .5 + right.getStepZ() * .5,
                3,
                false,
                25);
    }

    public void toggleCollisionPrevention() {
        hbm$syncSet$SolderingBenchBlockEntity$collisionPrevention(!collisionPrevention);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        var slots = slot < SLOT_PCB_START ? TOPPINGS : slot < SLOT_SOLDER ? BOARDS : SOLDER;
        List<CountIngredient> admitted;
        if (slot >= SLOT_TOPPING_START && slot < SLOT_PCB_START)
            admitted = SolderingRecipes.toppings();
        else if (slot >= SLOT_PCB_START && slot < SLOT_SOLDER) admitted = SolderingRecipes.pcb();
        else if (slot == SLOT_SOLDER) admitted = SolderingRecipes.solder();
        else return false;
        for (int other : slots) {
            if (other != slot && ItemStack.isSameItemSameComponents(inventory.get(other), stack))
                return false;
        }
        for (var ingredient : admitted) if (ingredient.matchesItem(stack)) return true;
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineSolderingStation");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SolderingStationMenu(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$SolderingBenchBlockEntity$collisionPrevention(
                input.getBooleanOr("collisionPrevention", collisionPrevention));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("collisionPrevention", collisionPrevention);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0xFF;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 5 -> output.writeBoolean(collisionPrevention);
            case 7 -> tank.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 5 ->
                    hbm$syncSet$SolderingBenchBlockEntity$collisionPrevention(input.readBoolean());
            case 7 -> tank.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$SolderingBenchBlockEntity$collisionPrevention(boolean next) {
        if (this.collisionPrevention != next) {
            this.collisionPrevention = next;
            if ((32) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 32);
        }
    }

    static {
        com.hbm.inventory.container.SyncedData.fields(
                SolderingBenchBlockEntity.class, "collisionPrevention");
    }
}
