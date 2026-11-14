/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

package com.hbm.blockentity.machine;

import com.hbm.api.environment.Environment;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.fluidmk2.PlainFluidProvider;
import com.hbm.api.fluidmk2.PlainFluidReceiver;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.Tiltable;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.config.BalanceConfig;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.BlastFurnaceMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.inventory.recipes.BlastFurnaceRecipe;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.modules.ModuleBurnTime;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class BlastFurnaceBlockEntity extends MachineBlockEntity
        implements MenuProvider,
                PlainFluidReceiver,
                PlainFluidProvider,
                FluidFlushSender,
                FluidSettingsHandler,
                Tiltable,
                SyncUnitSchema {
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_INPUT_1 = 1;
    public static final int SLOT_INPUT_2 = 2;
    public static final int SLOT_OUTPUT_1 = 3;
    public static final int SLOT_OUTPUT_2 = 4;
    public static final int SLOT_COUNT = 5;
    public static final int FUEL_RATE = 800;
    public static final int MAX_FUEL = 38_400;
    private static final int[] AUTOMATED_SLOTS = {
        SLOT_INPUT_1, SLOT_INPUT_2, SLOT_FUEL, SLOT_OUTPUT_1, SLOT_OUTPUT_2
    };
    private static final ModuleBurnTime FUELS = new ModuleBurnTime().setWoodHeatMod(0);
    public static Consumer<BlastFurnaceBlockEntity> CLIENT_TOWER;

    public int fuel;
    public float progress;
    public float speed;
    public boolean processing;
    public final FluidTankNTM[] tanks;
    private final FluidTankNTM[] sendingTanks;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final BlockPos[] floor;
    private final BlockPos chimney;

    public BlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLAST_FURNACE.get(), pos, state, SLOT_COUNT);
        tanks =
                new FluidTankNTM[] {
                    new FluidTankNTM(NTMFluids.AIRBLAST, 4_000),
                    new FluidTankNTM(NTMFluids.FLUE, 1_000)
                };
        sendingTanks = new FluidTankNTM[] {tanks[1]};
        tanks[0].bindOwner(this);
        tanks[1].bindOwner(this);
        floor =
                new BlockPos[] {
                    pos.offset(-1, -1, -1),
                    pos.offset(-1, -1, 1),
                    pos.offset(1, -1, -1),
                    pos.offset(1, -1, 1)
                };
        chimney = pos.above(7);
    }

    @Override
    public void tickServer() {
        tickTilt(Policy.CONFIGURED, false);
        flush.provide((ServerLevel) level, this);
        var heldFuel = getItem(SLOT_FUEL);
        int heat = heatValue(heldFuel);
        if (heat > 0 && heat <= MAX_FUEL - fuel) {
            hbm$syncSet$BlastFurnaceBlockEntity$fuel((int) (fuel + (heat)));
            heldFuel.shrink(1);
            markChanged();
        }

        var recipe =
                BlastFurnaceRecipesNT.INSTANCE.getRecipe(
                        getItem(SLOT_INPUT_1), getItem(SLOT_INPUT_2));
        int orientation = recipe == null ? -1 : inputOrientation(recipe);
        hbm$syncSet$BlastFurnaceBlockEntity$processing(
                !isTilted()
                        && fuel >= FUEL_RATE
                        && orientation >= 0
                        && outputsFit(recipe.outputItems()));
        if (processing) {
            hbm$syncSet$BlastFurnaceBlockEntity$speed(
                    Math.clamp(1F + 8F * tanks[0].getFill() / 4_000F, 1F, 5F));
            hbm$syncSet$BlastFurnaceBlockEntity$progress(
                    (float) (progress + (speed / recipe.duration)));
            markChanged();
            if (progress >= 1F) finish(recipe, orientation);
            if (level.getRandom().nextInt(10) == 0 && !isMuffled()) {
                level.playSound(
                        null,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5,
                        SoundEvents.FIRE_AMBIENT,
                        SoundSource.BLOCKS,
                        1F,
                        0.5F + level.getRandom().nextFloat() * 0.25F);
            }
        } else {
            hbm$syncSet$BlastFurnaceBlockEntity$speed(0);
            if (progress != 0) {
                hbm$syncSet$BlastFurnaceBlockEntity$progress(0);
                markChanged();
            }
        }
        int air = tanks[0].getFill();
        if (air > 0) {
            tanks[0].setFill((int) (air * 0.95D));
            markChanged();
        }
        networkPackNT(100);
    }

    private int heatValue(ItemStack stack) {
        if (stack.isEmpty() || stack.getCraftingRemainder() != null) return 0;
        int duration = FUELS.getBurnTime(level, stack, 0D);
        return FUELS.getBurnHeat(duration, stack);
    }

    private int inputOrientation(BlastFurnaceRecipe recipe) {
        var first = getItem(SLOT_INPUT_1);
        var second = getItem(SLOT_INPUT_2);
        var ingredients = recipe.inputItem;
        if (ingredients.length == 1) {
            if (second.isEmpty() && ingredients[0].test(first)) return 0;
            return first.isEmpty() && ingredients[0].test(second) ? 1 : -1;
        }
        if (ingredients[0].test(first) && ingredients[1].test(second)) return 0;
        return ingredients[0].test(second) && ingredients[1].test(first) ? 1 : -1;
    }

    private boolean outputsFit(WeightedList<ItemStack>[] outputs) {
        for (int lane = 0; lane < outputs.length; lane++) {
            var held = getItem(SLOT_OUTPUT_1 + lane);
            if (held.isEmpty()) continue;
            var choices = outputs[lane].unwrap();
            if (choices.size() != 1) return false;
            var result = choices.getFirst().value();
            if (!ItemStack.isSameItemSameComponents(held, result)
                    || held.getCount() + result.getCount()
                            > Math.min(getMaxStackSize(), result.getMaxStackSize())) return false;
        }
        return true;
    }

    private void finish(BlastFurnaceRecipe recipe, int orientation) {
        var outputs = recipe.outputItems();
        for (int lane = 0; lane < outputs.length; lane++) {
            var result = outputs[lane].getRandomOrThrow(GenericRecipes.RNG);
            var held = getItem(SLOT_OUTPUT_1 + lane);
            if (held.isEmpty()) inventory.set(SLOT_OUTPUT_1 + lane, result.copy());
            else held.grow(result.getCount());
        }
        var ingredients = recipe.inputItem;
        for (int index = 0; index < ingredients.length; index++) {
            getItem(SLOT_INPUT_1 + (index ^ orientation)).shrink(ingredients[index].count());
        }
        hbm$syncSet$BlastFurnaceBlockEntity$progress(0);
        hbm$syncSet$BlastFurnaceBlockEntity$fuel((int) (fuel - (FUEL_RATE)));
        var flue = tanks[1];
        int fill = (int) (flue.getFill() + 8F * (recipe.duration / speed));
        if (fill > flue.getMaxFill()) {
            FluidReleaseType.SPILL.dispatch(
                    level, worldPosition, flue.getFluid(), flue, fill - flue.getMaxFill());
        }
        flue.setFill(fill);
        markChanged();
    }

    @Override
    public void tickClient() {
        if (!processing || !TickPhase.every(this, 2)) return;
        if (level.getBlockState(chimney).isAir()) {
            level.addParticle(
                    ParticleTypes.LAVA,
                    worldPosition.getX() + 0.25 + level.getRandom().nextDouble() * 0.5,
                    worldPosition.getY() + 7.25,
                    worldPosition.getZ() + 0.25 + level.getRandom().nextDouble() * 0.5,
                    0,
                    0,
                    0);
        }
        if (tanks[1].getFill() >= 1_000) CLIENT_TOWER.accept(this);
    }

    @Override
    public int getSupportCheckCount() {
        return floor.length;
    }

    @Override
    public BlockPos getSupportCheckPosition(int index) {
        return floor[index];
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATED_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_FUEL) {
            if (stack.isEmpty() || stack.getCraftingRemainder() != null) return false;
            return heatValue(stack) > 0;
        }
        if (slot != SLOT_INPUT_1 && slot != SLOT_INPUT_2) return false;
        if (ItemStack.isSameItemSameComponents(stack, getItem(SLOT_INPUT_1 + SLOT_INPUT_2 - slot)))
            return false;
        var candidates = BlastFurnaceRecipesNT.INSTANCE.byInputItem().candidates(stack.getItem());
        for (int row = 0; row < candidates.size(); row++) {
            for (var ingredient : candidates.get(row).inputItem)
                if (ingredient.matchesItem(stack)) return true;
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT_1 || slot == SLOT_OUTPUT_2;
    }

    @Override
    public boolean canReceiveFluid(Fluid type) {
        return tanks[0].joins(type);
    }

    @Override
    public boolean canProvideFluid(Fluid type) {
        return tanks[1].joins(type);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        var tank = tanks[0];
        return tank.getPressure() == pressure && tank.accepts(type)
                ? tank.getMaxFill() - tank.getFill()
                : 0;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        var tank = tanks[0];
        if (tank.getPressure() != pressure || !tank.accepts(type)) return amount;
        int accepted =
                tank.receive(type, (int) Math.min(amount, tank.getMaxFill() - tank.getFill()));
        if (accepted != 0) markChanged();
        return amount - accepted;
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        var tank = tanks[1];
        return tank.getPressure() == pressure && tank.provides(type) ? tank.getFill() : 0;
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        var tank = tanks[1];
        if (tank.getPressure() == pressure
                && tank.provides(type)
                && tank.drain((int) amount, true) != 0) markChanged();
    }

    @Override
    public long getProviderSpeed(Fluid type, int pressure) {
        return Math.max(8, 50 * tanks[1].getFill() / 1_000);
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sendingTanks;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(tanks[1], FlushFaces.activePlane());
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        visitor.accept(tanks[0]);
        visitor.accept(tanks[1]);
    }

    @Override
    public int tankCount() {
        return tanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return tanks;
    }

    public static List<String> getFuelBonuses() {
        return FUELS.getDesc();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.blastFurnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BlastFurnaceMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$BlastFurnaceBlockEntity$fuel(input.getIntOr("fuel", 0));
        hbm$syncSet$BlastFurnaceBlockEntity$progress(input.getFloatOr("progress", 0));
        tanks[0].deserialize(input.childOrEmpty("airblast"));
        tanks[1].deserialize(input.childOrEmpty("flue"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("fuel", fuel);
        output.putFloat("progress", progress);
        tanks[0].serialize(output.child("airblast"));
        tanks[1].serialize(output.child("flue"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 31;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(fuel);
            case 1 -> output.writeFloat(progress);
            case 2 -> output.writeFloat(speed);
            case 3 -> output.writeBoolean(processing);
            case 4 -> {
                tanks[0].packetSerialize(output);
                tanks[1].packetSerialize(output);
            }
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$BlastFurnaceBlockEntity$fuel(input.readInt());
            case 1 -> hbm$syncSet$BlastFurnaceBlockEntity$progress(input.readFloat());
            case 2 -> hbm$syncSet$BlastFurnaceBlockEntity$speed(input.readFloat());
            case 3 -> hbm$syncSet$BlastFurnaceBlockEntity$processing(input.readBoolean());
            case 4 -> {
                tanks[0].packetDeserialize(input);
                tanks[1].packetDeserialize(input);
            }
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$BlastFurnaceBlockEntity$fuel(int next) {
        if (this.fuel != next) {
            this.fuel = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$BlastFurnaceBlockEntity$progress(float next) {
        if (Float.floatToRawIntBits(this.progress) != Float.floatToRawIntBits(next)) {
            this.progress = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$BlastFurnaceBlockEntity$speed(float next) {
        if (Float.floatToRawIntBits(this.speed) != Float.floatToRawIntBits(next)) {
            this.speed = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    public final void hbm$syncSet$BlastFurnaceBlockEntity$processing(boolean next) {
        if (this.processing != next) {
            this.processing = next;
            if ((8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8);
        }
    }

    public final FluidTankNTM hbm$syncArraySet$BlastFurnaceBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 16);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(BlastFurnaceBlockEntity.class, "tanks", 3, 16);
    }

    private int hbm$tiltChecked;

    private int hbm$tiltValid;

    public boolean isTilted() {
        return getBlockState().getValue(Tiltable.TILT);
    }

    public void tickTilt(Tiltable.Policy policy, boolean extraHeavy) {
        Level level = getLevel();
        boolean doesTilt =
                (policy == Tiltable.Policy.UNAVOIDABLE
                                ? Environment.unavoidableGravity(level)
                                : MachineData.ENABLE_MACHINE_GRAVITY.get()
                                        || BalanceConfig.enable528MachineGravity)
                        && !Environment.orbital(level);

        if (!doesTilt || getSupportCheckCount() <= 0) {
            hbm$settle(false);
            return;
        }
        if (!TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) return;

        if (hbm$tiltChecked >= getSupportCheckCount()) {
            boolean tilted = hbm$tiltValid < hbm$tiltChecked * 0.95;
            if (tilted && !isTilted()) {
                SoundEvent impact = ModSounds.METAL_IMPACT.get();
                level.playSound(null, getBlockPos(), impact, SoundSource.BLOCKS, 3F, 1F);
            }
            hbm$settle(tilted);
            hbm$tiltChecked = 0;
            hbm$tiltValid = 0;
        }

        BlockPos pos = getSupportCheckPosition(hbm$tiltChecked);
        if (pos == null) return;

        BlockState ground = level.getBlockState(pos);
        hbm$tiltChecked++;

        if (extraHeavy) {
            if (!ground.isSolidRender() || ground.isSignalSource()) return;
            if (ground.getBlock() instanceof FallingBlock
                    || ground.is(BlockTags.MINEABLE_WITH_SHOVEL)
                    || ground.is(BlockTags.WOOL)) return;
            if (ground.getBlock().getExplosionResistance() < Blocks.STONE.getExplosionResistance())
                return;
            hbm$tiltValid++;
            return;
        }
        if (!ground.isFaceSturdy(level, pos, Direction.UP)) return;
        if (ground.getBlock() instanceof FallingBlock) return;
        if (ground.is(ModBlocks.DIRT_DEAD.get())
                || ground.is(ModBlocks.DIRT_OILY.get())
                || ground.is(ModBlocks.STONE_CRACKED.get())) return;
        hbm$tiltValid++;
    }

    private void hbm$settle(boolean tilted) {
        BlockState state = getBlockState();
        if (state.getValue(Tiltable.TILT) == tilted) return;
        BlockState settled = state.setValue(Tiltable.TILT, tilted);
        getLevel()
                .setBlock(
                        getBlockPos(),
                        settled,
                        Block.UPDATE_CLIENTS
                                | Block.UPDATE_KNOWN_SHAPE
                                | Block.UPDATE_SKIP_ON_PLACE);
    }
}
