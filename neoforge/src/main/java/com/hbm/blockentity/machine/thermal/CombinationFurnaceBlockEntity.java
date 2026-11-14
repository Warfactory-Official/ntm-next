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

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.tile.HeatHandler;
import com.hbm.api.tile.HeatIntake;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.PollutingMachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ContractLink;
import com.hbm.capability.NtmContracts;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.FurnaceCombinationMenu;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CombinationRecipe;
import com.hbm.inventory.recipes.CombinationRecipes;
import com.hbm.inventory.recipes.loader.Outputs;
import com.hbm.inventory.recipes.loader.RecipeSource;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class CombinationFurnaceBlockEntity extends PollutingMachineBlockEntity
        implements FluidTankHandler, FluidSettingsHandler, MenuProvider, SyncUnitSchema {
    public static final int SLOT_INPUT = 0,
            SLOT_OUTPUT = 1,
            SLOT_CANISTER_IN = 2,
            SLOT_CANISTER_OUT = 3,
            SLOT_COUNT = 4;
    public static final int PROCESS_TIME = 20000, MAX_HEAT = 100000;
    private static final int[] SLOTS = {0, 1};
    public boolean wasOn;
    public int heat;
    public int progress;
    public final FluidTankNTM tank = new FluidTankNTM(24000);
    private final FluidTankNTM[] sending = {tank, smoke, smokeLeaded, smokePoison};
    private final FluidTankNTM[] settings = {tank};
    private final ContractLink<HeatHandler> heatBelow =
            new ContractLink<>(NtmContracts.HEAT_SOURCE);
    private final BlockPos below;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final AABB exhaust;
    private ItemStack remembered = ItemStack.EMPTY;
    private RecipeSource.@Nullable Token generation;
    private @Nullable CombinationRecipe heldRecipe;

    public CombinationFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMBINATION_OVEN.get(), pos, state, SLOT_COUNT, 50);
        below = pos.below();
        exhaust =
                new AABB(
                        pos.getX() - 0.5,
                        pos.getY() + 2,
                        pos.getZ() - 0.5,
                        pos.getX() + 1.5,
                        pos.getY() + 4,
                        pos.getZ() + 1.5);
    }

    private @Nullable CombinationRecipe recipe() {
        var input = inventory.get(SLOT_INPUT);
        var current = RecipeSource.token();
        if (generation != current || !ItemStack.isSameItemSameComponents(input, remembered)) {
            generation = current;
            remembered = input.copy();
            heldRecipe = CombinationRecipes.INSTANCE.getOutput(input);
        }
        return heldRecipe;
    }

    public boolean canSmelt() {
        var recipe = recipe();
        if (recipe == null || !Outputs.fits(inventory.get(SLOT_OUTPUT), recipe.outputItems(), 0))
            return false;
        return recipe.fluidType() == null
                || (tank.getFill() == 0 || tank.getTankType() == recipe.fluidType())
                        && tank.getMaxFill() - tank.getFill() >= recipe.fluidAmount();
    }

    @Override
    public void tickServer() {
        int previousHeat = heat, previousProgress = progress;
        if (heat < MAX_HEAT)
            hbm$syncSet$CombinationFurnaceBlockEntity$heat(
                    HeatIntake.GAP.pull(heatBelow.get(level, below), heat, MAX_HEAT, 0.25D));
        boolean changed = tank.unloadTank(SLOT_CANISTER_IN, SLOT_CANISTER_OUT, inventory);
        hbm$syncSet$CombinationFurnaceBlockEntity$wasOn(false);
        if (canSmelt()) {
            int rate = heat / 100;
            if (rate > 0) {
                hbm$syncSet$CombinationFurnaceBlockEntity$wasOn(true);
                hbm$syncSet$CombinationFurnaceBlockEntity$heat((int) (heat - (rate)));
                hbm$syncSet$CombinationFurnaceBlockEntity$progress((int) (progress + (rate)));
                if (progress >= PROCESS_TIME) {
                    hbm$syncSet$CombinationFurnaceBlockEntity$progress(
                            (int) (progress - (PROCESS_TIME)));
                    var recipe = heldRecipe;
                    var result = recipe.makeOutput();
                    if (!result.isEmpty())
                        SmeltingRecipeCache.insert(inventory, SLOT_OUTPUT, result);
                    if (recipe.fluidType() != null) {
                        tank.setTankType(recipe.fluidType());
                        tank.setFill(tank.getFill() + recipe.fluidAmount());
                    }
                    inventory.get(SLOT_INPUT).shrink(1);
                    changed = true;
                }
                vent();
            }
        } else hbm$syncSet$CombinationFurnaceBlockEntity$progress(0);
        if (changed || heat != previousHeat || progress != previousProgress) markChanged();
        flush.provide((ServerLevel) level, this);
        networkPackNT(50);
    }

    private void vent() {
        for (var entity : level.getEntitiesOfClass(Entity.class, exhaust))
            entity.igniteForSeconds(5);
        if (TickPhase.every(this, 10)) {
            WorldSounds.machine(
                    level,
                    null,
                    worldPosition.above(),
                    ModSounds.FLAMETHROWER_SHOOT.get(),
                    SoundSource.BLOCKS,
                    0.25F,
                    0.5F);
        }
        if (TickPhase.every(this, 20))
            pollute(PollutionType.SOOT, 3 * PollutionHandler.SOOT_PER_SECOND);
    }

    @Override
    public void tickClient() {
        if (wasOn && level.getRandom().nextInt(15) == 0) {
            level.addParticle(
                    ParticleTypes.LAVA,
                    worldPosition.getX() + 0.5 + level.getRandom().nextGaussian() * 0.5,
                    worldPosition.getY() + 2,
                    worldPosition.getZ() + 0.5 + level.getRandom().nextGaussian() * 0.5,
                    0,
                    0,
                    0);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction face) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT && CombinationRecipes.INSTANCE.getOutput(stack) != null;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return settings;
    }

    @Override
    public void declareFlush(FlushLanes lanes) {
        for (var output : sending) lanes.add(output, FlushFaces.own(), 20);
    }

    @Override
    public int tankCount() {
        return sending.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return sending[index];
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.furnaceCombination");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FurnaceCombinationMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$CombinationFurnaceBlockEntity$progress(input.getIntOr("prog", progress));
        hbm$syncSet$CombinationFurnaceBlockEntity$heat(input.getIntOr("heat", heat));
        input.child("tank").ifPresent(tank::deserialize);

        heatBelow.load(input, "heatBelow");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("prog", progress);
        output.putInt("heat", heat);
        tank.serialize(output.child("tank"));

        heatBelow.save(output, "heatBelow");
    }

    @Override
    public long syncUnitMask() {
        return 15L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(wasOn);
            case 1 -> output.writeInt(heat);
            case 2 -> output.writeInt(progress);
            case 3 -> tank.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$CombinationFurnaceBlockEntity$wasOn(input.readBoolean());
            case 1 -> hbm$syncSet$CombinationFurnaceBlockEntity$heat(input.readInt());
            case 2 -> hbm$syncSet$CombinationFurnaceBlockEntity$progress(input.readInt());
            case 3 -> tank.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public void refreshHeatBelow() {
        heatBelow.refresh(level, worldPosition.below());
    }

    public final void hbm$syncSet$CombinationFurnaceBlockEntity$wasOn(boolean next) {
        if (this.wasOn != next) {
            this.wasOn = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$CombinationFurnaceBlockEntity$heat(int next) {
        if (this.heat != next) {
            this.heat = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    public final void hbm$syncSet$CombinationFurnaceBlockEntity$progress(int next) {
        if (this.progress != next) {
            this.progress = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(CombinationFurnaceBlockEntity.class, "tank", 3, 8L);
    }
}
