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

package com.hbm.blocks.generic;

import com.hbm.api.environment.Environment;
import com.hbm.blockentity.prop.PedestalBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.extprop.PlayerState;
import com.hbm.inventory.recipes.PedestalRecipe;
import com.hbm.inventory.recipes.PedestalRecipes;
import com.hbm.particle.helper.ExplosionSmallCreator;
import com.hbm.stats.ModStats;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class PedestalBlock extends Block implements TickingEntityBlock {

    private static final BlockPos[] RITUAL_OFFSETS = {
        new BlockPos(-2, 0, -2), new BlockPos(0, 0, -3), new BlockPos(2, 0, -2),
        new BlockPos(-3, 0, 0), BlockPos.ZERO, new BlockPos(3, 0, 0),
        new BlockPos(-2, 0, 2), new BlockPos(0, 0, 3), new BlockPos(2, 0, 2)
    };

    public PedestalBlock(Properties properties) {
        super(properties);
    }

    private static PedestalBlockEntity[] findPedestals(Level level, BlockPos center) {
        PedestalBlockEntity[] result = new PedestalBlockEntity[RITUAL_OFFSETS.length];
        for (int i = 0; i < result.length; i++) {
            if (level.getBlockEntity(center.offset(RITUAL_OFFSETS[i]))
                    instanceof PedestalBlockEntity pedestal) result[i] = pedestal;
        }
        return result;
    }

    private static void update(
            Level level, BlockPos pos, BlockState state, PedestalBlockEntity pedestal) {
        pedestal.setChanged();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    private static boolean matchesExtra(
            Level level, BlockPos pos, PedestalRecipes.ExtraCondition extra) {
        float celestialAngle = celestialAngle(level.getOverworldClockTime());
        return switch (extra) {
            case NONE -> true;
            case FULL_MOON ->
                    celestialAngle >= 0.35F
                            && celestialAngle <= 0.65F
                            && level.environmentAttributes()
                                            .getValue(EnvironmentAttributes.MOON_PHASE, pos)
                                    == MoonPhase.FULL_MOON;
            case NEW_MOON ->
                    Environment.eclipse(level)
                            .toBoolean(
                                    celestialAngle >= 0.35F
                                            && celestialAngle <= 0.65F
                                            && level.environmentAttributes()
                                                            .getValue(
                                                                    EnvironmentAttributes
                                                                            .MOON_PHASE,
                                                                    pos)
                                                    == MoonPhase.NEW_MOON);
            case SUN -> celestialAngle <= 0.15F || celestialAngle >= 0.85F;
            case BAD_KARMA -> hasReputation(level, pos, -10, false);
            case GOOD_KARMA -> hasReputation(level, pos, 10, true);
        };
    }

    private static float celestialAngle(long time) {
        float angle = (float) Math.floorMod(time, 24000L) / 24000.0F - 0.25F;
        if (angle < 0.0F) angle++;
        if (angle > 1.0F) angle--;
        float linear = angle;
        angle = 1.0F - ((float) Math.cos(angle * Math.PI) + 1.0F) / 2.0F;
        return linear + (angle - linear) / 3.0F;
    }

    private static boolean hasReputation(
            Level level, BlockPos pos, int threshold, boolean positive) {
        AABB nearby =
                new AABB(
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                pos.getX() + 1,
                                pos.getY() + 1,
                                pos.getZ() + 1)
                        .inflate(20.0);
        for (Player player : level.getEntitiesOfClass(Player.class, nearby)) {
            int reputation = PlayerState.getOrCreate(player).reputation;
            if (positive ? reputation >= threshold : reputation <= threshold) return true;
        }
        return false;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PedestalBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal))
            return InteractionResult.PASS;

        ItemStack held = player.getMainHandItem();
        if (pedestal.display.isEmpty() && !held.isEmpty()) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            pedestal.hbm$syncSet$PedestalBlockEntity$display(held.copy());
            held.consume(held.getCount(), player);
            update(level, pos, state, pedestal);
            return InteractionResult.SUCCESS;
        }
        if (!pedestal.display.isEmpty() && held.isEmpty()) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            player.setItemInHand(InteractionHand.MAIN_HAND, pedestal.display.copy());
            pedestal.hbm$syncSet$PedestalBlockEntity$display(ItemStack.EMPTY);
            update(level, pos, state, pedestal);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (level.isClientSide() || !level.hasNeighborSignal(pos)) return;
        if (!(level.getBlockEntity(pos) instanceof PedestalBlockEntity center)) return;

        PedestalBlockEntity[] pedestals = findPedestals(level, pos);
        ItemStack[] stacks = new ItemStack[pedestals.length];
        for (int i = 0; i < pedestals.length; i++) {
            stacks[i] = pedestals[i] == null ? ItemStack.EMPTY : pedestals[i].display;
        }

        for (PedestalRecipe recipe : PedestalRecipes.INSTANCE.recipes()) {
            if (!matchesExtra(level, pos, recipe.extra()) || !recipe.matches(stacks)) continue;

            for (int i = 0; i < pedestals.length; i++) {
                if (i == 4 || pedestals[i] == null || pedestals[i].display.isEmpty()) continue;
                pedestals[i].hbm$syncSet$PedestalBlockEntity$display(ItemStack.EMPTY);
                update(level, pos.offset(RITUAL_OFFSETS[i]), state, pedestals[i]);
            }

            center.hbm$syncSet$PedestalBlockEntity$display(recipe.output());
            update(level, pos, state, center);
            ExplosionSmallCreator.composeEffect(
                    level, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 10, 2.5F, 1F);

            AABB players =
                    new AABB(
                                    pos.getX() + 0.5,
                                    pos.getY(),
                                    pos.getZ() + 0.5,
                                    pos.getX() + 0.5,
                                    pos.getY(),
                                    pos.getZ() + 0.5)
                            .inflate(50.0);
            for (Player player : level.getEntitiesOfClass(Player.class, players))
                player.awardStat(ModStats.LEGENDARY.get());
            return;
        }
    }
}
