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

package com.hbm.blockentity.machine.reactor;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.handler.BossSpawnHandler;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.inventory.container.ReactorResearchMenu;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.machine.PlateFuelItem;
import com.hbm.items.tool.SwordMeteoriteItem;
import com.hbm.packet.SyncArrays;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ResearchReactorBlockEntity extends MachineBlockEntity
        implements MenuProvider, SyncUnitSchema {
    public static final int SLOT_COUNT = 12;
    public static final int HEAT_LIMIT = 50_000;
    private static final int[] NO_PENDING_FLUX = new int[SLOT_COUNT];
    private static final int[] ALL_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private static final int[] EDGE_START = {0, 2, 4, 6, 9, 11, 14, 17, 19, 22, 24, 26, 28};
    private static final int[] EDGE_TO = {
        1, 5, 0, 6, 3, 7, 2, 4, 8, 3, 9, 0, 6, 10, 1, 5, 11, 2, 8, 3, 7, 9, 4, 8, 5, 11, 6, 10
    };
    private static final Direction[] WATER_SIDES = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    public int heat;
    public byte waterCount;
    public double rodPosition;
    public double targetRodPosition;
    public double previousRodPosition;
    public final int[] pendingFlux = new int[SLOT_COUNT];
    public int totalFlux;
    private final BlockPos.MutableBlockPos waterProbe = new BlockPos.MutableBlockPos();

    public ResearchReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_RESEARCH.get(), pos, state, SLOT_COUNT);
    }

    public void requestRodPosition(double target) {
        if (Double.doubleToRawLongBits(targetRodPosition) == Double.doubleToRawLongBits(target))
            return;
        hbm$syncSet$ResearchReactorBlockEntity$targetRodPosition(target);
        setChanged();
    }

    @Override
    public void tickServer() {
        double oldRods = rodPosition;
        if (rodPosition < targetRodPosition)
            hbm$syncSet$ResearchReactorBlockEntity$rodPosition(
                    Math.min(targetRodPosition, rodPosition + 0.04D));
        else if (rodPosition > targetRodPosition)
            hbm$syncSet$ResearchReactorBlockEntity$rodPosition(
                    Math.max(targetRodPosition, rodPosition - 0.04D));
        if (Double.doubleToRawLongBits(oldRods) != Double.doubleToRawLongBits(rodPosition))
            setChanged();

        int oldHeat = heat;
        byte oldWater = waterCount;
        hbm$syncSet$ResearchReactorBlockEntity$totalFlux(0);
        if (rodPosition > 0) reactPlates();
        if (heat > 0) {
            hbm$syncSet$ResearchReactorBlockEntity$waterCount(countCoolingWater());
            hbm$syncSet$ResearchReactorBlockEntity$heat(
                    Math.max(
                            0,
                            waterCount == 0
                                    ? heat - 1
                                    : (int) (heat - heat * 0.07F * waterCount / 12F)));
        }
        if (heat != oldHeat || waterCount != oldWater) setChanged();
        if (heat > HEAT_LIMIT) {
            failColumn();
            return;
        }
        if (rodPosition > 0 && heat > 0 && !shielded()) {
            float gamma = (float) heat / HEAT_LIMIT * 50F;
            RadiationSystemNT.incrementRad((ServerLevel) level, worldPosition, gamma);
        }
        networkPackNT(150);
    }

    @Override
    public void tickClient() {
        previousRodPosition = rodPosition;
    }

    public void reactPlates() {
        var lifeType = ModDataComponents.PLATE_FUEL_LIFE.get();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            var stack = getItem(slot);
            if (stack.getItem() instanceof PlateFuelItem plate) {
                var oldLife = stack.get(lifeType);
                int output = plate.react(stack, pendingFlux[slot]);
                int newLife = PlateFuelItem.getLifeTime(stack);
                if (oldLife == null || oldLife.intValue() != newLife) setChanged();
                hbm$syncSet$ResearchReactorBlockEntity$heat((int) (heat + (output * 2)));
                hbm$syncSet$ResearchReactorBlockEntity$totalFlux((int) (totalFlux + (output)));
                hbm$syncArraySet$ResearchReactorBlockEntity$pendingFlux(pendingFlux, slot, 0);
                int sharedFlux = (int) (output * rodPosition);
                for (int edge = EDGE_START[slot]; edge < EDGE_START[slot + 1]; edge++) {
                    int destination = EDGE_TO[edge];
                    hbm$syncArraySet$ResearchReactorBlockEntity$pendingFlux(
                            pendingFlux,
                            destination,
                            (int) (pendingFlux[destination] + (sharedFlux)));
                }
                if (newLife > plate.lifeTime) setItem(slot, plate.spent().hot());
            } else {
                hbm$syncArraySet$ResearchReactorBlockEntity$pendingFlux(pendingFlux, slot, 0);
                SwordMeteoriteItem.upgrade(
                        inventory,
                        slot,
                        ModItems.METEORITE_SWORD_BRED,
                        ModItems.METEORITE_SWORD_IRRADIATED);
                if (inventory.get(slot) != stack) setChanged();
            }
        }
    }

    public static boolean hasWater(Level level, BlockPos pos) {
        return level.getBlockState(pos).getFluidState().is(FluidTags.WATER);
    }

    private byte countCoolingWater() {
        int count = 0;
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        if (hasWater(level, waterProbe.set(x, y - 1, z))) count++;
        if (hasWater(level, waterProbe.set(x, y + 3, z))) count++;
        for (var side : WATER_SIDES) {
            for (int height = 0; height < 3; height++) {
                if (hasWater(
                        level,
                        waterProbe.set(x + side.getStepX(), y + height, z + side.getStepZ())))
                    count++;
            }
        }
        return (byte) count;
    }

    public boolean visiblySubmerged() {
        for (var side : WATER_SIDES) {
            if (hasWater(level, worldPosition.offset(side.getStepX(), 1, side.getStepZ())))
                return true;
        }
        return false;
    }

    private boolean shielded() {
        for (var side : WATER_SIDES) {
            waterProbe.set(
                    worldPosition.getX() + side.getStepX(),
                    worldPosition.getY() + 1,
                    worldPosition.getZ() + side.getStepZ());
            var state = level.getBlockState(waterProbe);
            var fluid = state.getFluidState();
            if (fluid.is(FluidTags.WATER) && fluid.isSource()) continue;
            var core = MultiblockSurface.coreOfAny(level, waterProbe, state);
            var block = core == null ? state.getBlock() : level.getBlockState(core).getBlock();
            if (block == ModBlocks.BLOCK_LEAD.get()
                    || block == ModBlocks.BLOCK_DESH.get()
                    || block == ModBlocks.REACTOR_RESEARCH.get()
                    || block == ModBlocks.MACHINE_REACTOR_BREEDING.get()
                    || block.getExplosionResistance() >= 100F) continue;
            return false;
        }
        return true;
    }

    public void failColumn() {
        var server = (ServerLevel) level;
        for (int slot = 0; slot < SLOT_COUNT; slot++) setItem(slot, ItemStack.EMPTY);
        server.removeBlock(worldPosition, false);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        removeWaterAt(x, y - 1, z);
        removeWaterAt(x, y + 3, z);
        for (var side : WATER_SIDES) {
            for (int height = 0; height < 3; height++) {
                removeWaterAt(x + side.getStepX(), y + height, z + side.getStepZ());
            }
        }
        server.explode(null, x, y, z, 18F, Level.ExplosionInteraction.BLOCK);
        server.setBlockAndUpdate(worldPosition, ModBlocks.DECO_STEEL.get().defaultBlockState());
        server.setBlockAndUpdate(worldPosition.above(), ModBlocks.CORIUM.get().defaultBlockState());
        server.setBlockAndUpdate(
                worldPosition.above(2), ModBlocks.DECO_STEEL.get().defaultBlockState());
        RadiationSystemNT.incrementRad(server, worldPosition, 50);
        BossSpawnHandler.markRad(server, x + 0.5D, y + 0.5D, z + 0.5D);
    }

    private void removeWaterAt(int x, int y, int z) {
        waterProbe.set(x, y, z);
        if (hasWater(level, waterProbe)) level.removeBlock(waterProbe, false);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof PlateFuelItem;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.reactorResearch");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ReactorResearchMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$ResearchReactorBlockEntity$heat(input.getIntOr("heat", 0));
        hbm$syncSet$ResearchReactorBlockEntity$waterCount((byte) input.getIntOr("water", 0));
        hbm$syncSet$ResearchReactorBlockEntity$rodPosition(input.getDoubleOr("level", 0));
        hbm$syncSet$ResearchReactorBlockEntity$targetRodPosition(
                input.getDoubleOr("targetLevel", 0));
        previousRodPosition = rodPosition;
        SyncArrays.copy(this, 3, 16L, NO_PENDING_FLUX, 0, pendingFlux, 0, SLOT_COUNT);
        hbm$syncSet$ResearchReactorBlockEntity$totalFlux(0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("heat", heat);
        output.putInt("water", waterCount);
        output.putDouble("level", rodPosition);
        output.putDouble("targetLevel", targetRodPosition);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(heat);
            case 1 -> output.writeByte(waterCount);
            case 2 -> output.writeDouble(rodPosition);
            case 3 -> output.writeDouble(targetRodPosition);
            case 4 -> {
                for (int value : pendingFlux) output.writeInt(value);
            }
            case 5 -> output.writeInt(totalFlux);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$ResearchReactorBlockEntity$heat(input.readInt());
            case 1 -> hbm$syncSet$ResearchReactorBlockEntity$waterCount(input.readByte());
            case 2 -> {
                previousRodPosition = rodPosition;
                hbm$syncSet$ResearchReactorBlockEntity$rodPosition(input.readDouble());
            }
            case 3 -> hbm$syncSet$ResearchReactorBlockEntity$targetRodPosition(input.readDouble());
            case 4 -> {
                for (int slot = 0; slot < SLOT_COUNT; slot++)
                    hbm$syncArraySet$ResearchReactorBlockEntity$pendingFlux(
                            pendingFlux, slot, input.readInt());
            }
            case 5 -> hbm$syncSet$ResearchReactorBlockEntity$totalFlux(input.readInt());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$ResearchReactorBlockEntity$heat(int next) {
        if (this.heat != next) {
            this.heat = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$ResearchReactorBlockEntity$waterCount(byte next) {
        if (this.waterCount != next) {
            this.waterCount = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$ResearchReactorBlockEntity$rodPosition(double next) {
        if (Double.doubleToRawLongBits(this.rodPosition) != Double.doubleToRawLongBits(next)) {
            this.rodPosition = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    public final void hbm$syncSet$ResearchReactorBlockEntity$targetRodPosition(double next) {
        if (Double.doubleToRawLongBits(this.targetRodPosition)
                != Double.doubleToRawLongBits(next)) {
            this.targetRodPosition = next;
            if ((8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8);
        }
    }

    public final int hbm$syncArraySet$ResearchReactorBlockEntity$pendingFlux(
            int[] array, int index, int next) {
        if (array[index] != next) {
            array[index] = next;
            if (array == this.pendingFlux) {
                syncArrayChanged(array, index, 3, 16);
            }
        }
        return next;
    }

    public final void hbm$syncSet$ResearchReactorBlockEntity$totalFlux(int next) {
        if (this.totalFlux != next) {
            this.totalFlux = next;
            if ((32) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 32);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(ResearchReactorBlockEntity.class, "pendingFlux", 3, 16);
        com.hbm.inventory.container.SyncedData.fields(
                ResearchReactorBlockEntity.class, "heat", "totalFlux");
    }
}
