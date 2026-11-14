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

package com.hbm.blockentity.machine.thermal;

import com.hbm.api.tile.HeatHandler;
import com.hbm.api.tile.HeatIntake;
import com.hbm.blockentity.SmeltingFurnaceBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.ContractLink;
import com.hbm.capability.NtmContracts;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.FurnaceSteelMenu;
import com.hbm.inventory.material.MaterialTagRegistry;
import com.hbm.inventory.recipes.loader.Outputs;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SteelFurnaceBlockEntity extends SmeltingFurnaceBlockEntity
        implements MenuProvider, SyncUnitSchema {
    public static final int SLOT_COUNT = 6;
    public static final int PROCESS_TIME = 40000;
    public static final int MAX_HEAT = 100000;
    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5};
    private static final TagKey<Item> ORES =
            TagKey.create(Registries.ITEM, Identifier.parse("c:ores"));
    private static final TagKey<Item> RAW =
            TagKey.create(Registries.ITEM, Identifier.parse("c:raw_materials"));
    public final int[] progress = new int[3];
    public final int[] bonus = new int[3];
    public int heat;
    public boolean wasOn;
    private final ItemStack[] remembered = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private final SmeltingRecipeCache[] recipes = {
        new SmeltingRecipeCache(), new SmeltingRecipeCache(), new SmeltingRecipeCache()
    };
    private final SmeltingRecipeCache admission = new SmeltingRecipeCache();
    private final ContractLink<HeatHandler> heatBelow =
            new ContractLink<>(NtmContracts.HEAT_SOURCE);
    private final BlockPos below;

    public SteelFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FURNACE_STEEL.get(), pos, state, SLOT_COUNT, 3, 4, 5);
        below = pos.below();
    }

    @Override
    public void tickServer() {
        int previousHeat = heat;
        if (heat < MAX_HEAT)
            hbm$syncSet$SteelFurnaceBlockEntity$heat(
                    HeatIntake.GAP.pull(heatBelow.get(level, below), heat, MAX_HEAT, 0.05D));
        int rate = (heat - MAX_HEAT / 3) / 10;
        hbm$syncSet$SteelFurnaceBlockEntity$wasOn(false);
        boolean changed = false;
        var server = (ServerLevel) level;
        for (int lane = 0; lane < 3; lane++) {
            int previousProgress = progress[lane];
            int previousBonus = bonus[lane];
            if (!payCredit(lane, server)) {
                changed |= bonus[lane] != previousBonus;
                continue;
            }
            var input = inventory.get(lane);
            if (input.isEmpty()
                    || remembered[lane].isEmpty()
                    || !ItemStack.isSameItemSameComponents(input, remembered[lane])) {
                hbm$syncArraySet$SteelFurnaceBlockEntity$progress(progress, lane, 0);
                hbm$syncArraySet$SteelFurnaceBlockEntity$bonus(bonus, lane, 0);
            }
            var result = recipes[lane].result(server, input);
            if (heat >= MAX_HEAT / 3
                    && SmeltingRecipeCache.fits(
                            inventory.get(lane + 3), result, Integer.MAX_VALUE)) {
                hbm$syncArraySet$SteelFurnaceBlockEntity$progress(
                        progress, lane, (int) (progress[lane] + (rate)));
                hbm$syncSet$SteelFurnaceBlockEntity$heat((int) (heat - (rate)));
                hbm$syncSet$SteelFurnaceBlockEntity$wasOn(true);
                if (TickPhase.every(this, 20)) {
                    PollutionHandler.add(
                            level,
                            worldPosition,
                            PollutionType.SOOT,
                            2 * PollutionHandler.SOOT_PER_SECOND);
                }
            }
            if (!ItemStack.isSameItemSameComponents(remembered[lane], input)) {
                remembered[lane] = input.copy();
                changed = true;
            }
            if (progress[lane] >= PROCESS_TIME) {
                if (!result.isEmpty()) {
                    SmeltingRecipeCache.insert(inventory, lane + 3, result);
                    hbm$syncArraySet$SteelFurnaceBlockEntity$bonus(
                            bonus, lane, (int) (bonus[lane] + (credit(input))));
                    payCredit(lane, server);
                    input.shrink(1);
                    recipesUsed.record(lane, recipes[lane].recipe());
                }
                hbm$syncArraySet$SteelFurnaceBlockEntity$progress(progress, lane, 0);
                changed = true;
            }
            changed |= previousProgress != progress[lane] || previousBonus != bonus[lane];
        }
        if (changed || previousHeat != heat) markChanged();
        networkPackNT(50);
    }

    private boolean payCredit(int lane, ServerLevel server) {
        while (bonus[lane] >= 100) {
            var result = recipes[lane].result(server, remembered[lane]);
            if (!SmeltingRecipeCache.fits(inventory.get(lane + 3), result, Integer.MAX_VALUE))
                return false;
            SmeltingRecipeCache.insert(inventory, lane + 3, result);
            hbm$syncArraySet$SteelFurnaceBlockEntity$bonus(
                    bonus, lane, (int) (bonus[lane] - (100)));
        }
        return true;
    }

    private int credit(ItemStack input) {
        if (input.is(ORES)) return 25;
        if (input.is(RAW)) return level.getRandom().nextFloat() < Outputs.RAW_SHARE ? 25 : 0;
        if (input.is(ItemTags.LOGS) || input.is(MaterialTagRegistry.KEY_ANY_TAR)) return 50;
        return 0;
    }

    @Override
    public void tickClient() {
        if (!wasOn) return;
        var facing = MultiblockCoreBlock.coreFacing(getBlockState());
        var side = facing.getClockWise();
        double x = worldPosition.getX() + 0.5, z = worldPosition.getZ() + 0.5;
        int y = worldPosition.getY();
        var random = level.getRandom();
        level.addParticle(
                ParticleTypes.SMOKE,
                x - facing.getStepX() * 1.125 - side.getStepX() * 0.75,
                y + 2.625,
                z - facing.getStepZ() * 1.125 - side.getStepZ() * 0.75,
                0,
                0.05,
                0);
        if (random.nextInt(20) == 0) {
            level.addParticle(
                    ParticleTypes.CLOUD,
                    x + facing.getStepX() * 0.75,
                    y + 2,
                    z + facing.getStepZ() * 0.75,
                    0,
                    0.05,
                    0);
        }
        if (random.nextInt(15) == 0) {
            double offset = random.nextDouble() - 0.5;
            level.addParticle(
                    ParticleTypes.LAVA,
                    x + facing.getStepX() * 1.5 + side.getStepX() * offset,
                    y + 0.75,
                    z + facing.getStepZ() * 1.5 + side.getStepZ() * offset,
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
        return slot < 3 && level instanceof ServerLevel server && admission.accepts(server, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 3;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.furnaceSteel");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FurnaceSteelMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getIntArray("progress")
                .ifPresent(
                        values -> {
                            for (int lane = 0; lane < Math.min(values.length, 3); lane++)
                                hbm$syncArraySet$SteelFurnaceBlockEntity$progress(
                                        progress, lane, values[lane]);
                        });
        input.getIntArray("bonus")
                .ifPresent(
                        values -> {
                            for (int lane = 0; lane < Math.min(values.length, 3); lane++)
                                hbm$syncArraySet$SteelFurnaceBlockEntity$bonus(
                                        bonus, lane, values[lane]);
                        });
        hbm$syncSet$SteelFurnaceBlockEntity$heat(input.getIntOr("heat", heat));
        input.child("lastItems")
                .ifPresent(
                        items -> {
                            for (int lane = 0; lane < 3; lane++)
                                remembered[lane] =
                                        items.read("lastItem" + lane, ItemStack.CODEC)
                                                .orElse(ItemStack.EMPTY);
                        });

        heatBelow.load(input, "heatBelow");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putIntArray("progress", progress);
        output.putIntArray("bonus", bonus);
        output.putInt("heat", heat);
        var items = output.child("lastItems");
        for (int lane = 0; lane < 3; lane++) {
            if (!remembered[lane].isEmpty())
                items.store("lastItem" + lane, ItemStack.CODEC, remembered[lane]);
        }

        heatBelow.save(output, "heatBelow");
    }

    @Override
    public long syncUnitMask() {
        return 15L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> {
                for (int value : progress) output.writeInt(value);
            }
            case 1 -> {
                for (int value : bonus) output.writeInt(value);
            }
            case 2 -> output.writeInt(heat);
            case 3 -> output.writeBoolean(wasOn);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> {
                for (int lane = 0; lane < 3; lane++)
                    hbm$syncArraySet$SteelFurnaceBlockEntity$progress(
                            progress, lane, input.readInt());
            }
            case 1 -> {
                for (int lane = 0; lane < 3; lane++)
                    hbm$syncArraySet$SteelFurnaceBlockEntity$bonus(bonus, lane, input.readInt());
            }
            case 2 -> hbm$syncSet$SteelFurnaceBlockEntity$heat(input.readInt());
            case 3 -> hbm$syncSet$SteelFurnaceBlockEntity$wasOn(input.readBoolean());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public void refreshHeatBelow() {
        heatBelow.refresh(level, worldPosition.below());
    }

    public final int hbm$syncArraySet$SteelFurnaceBlockEntity$progress(
            int[] array, int index, int next) {
        if (array[index] != next) {
            array[index] = next;
            if (array == this.progress) {
                syncArrayChanged(array, index, 3, 1L);
            }
        }
        return next;
    }

    public final int hbm$syncArraySet$SteelFurnaceBlockEntity$bonus(
            int[] array, int index, int next) {
        if (array[index] != next) {
            array[index] = next;
            if (array == this.bonus) {
                syncArrayChanged(array, index, 3, 2L);
            }
        }
        return next;
    }

    public final void hbm$syncSet$SteelFurnaceBlockEntity$heat(int next) {
        if (this.heat != next) {
            this.heat = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    public final void hbm$syncSet$SteelFurnaceBlockEntity$wasOn(boolean next) {
        if (this.wasOn != next) {
            this.wasOn = next;
            if ((8L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(SteelFurnaceBlockEntity.class, "progress", 3, 1L);
        com.hbm.packet.SyncBindings.field(SteelFurnaceBlockEntity.class, "bonus", 3, 2L);
    }
}
