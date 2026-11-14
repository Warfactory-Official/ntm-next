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

package com.hbm.blocks.bomb;

import com.hbm.blockentity.bomb.CrashedBombBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.data.ExplosionData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.logic.BalefireEntity;
import com.hbm.entity.logic.NukeExplosionMK5Entity;
import com.hbm.explosion.MiniNukeAftermath;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.AttenuatedRayBlockSelector;
import com.hbm.explosion.vanillant.standard.CellDestruction;
import com.hbm.explosion.vanillant.standard.MultiPointExplosionEntityProcessor;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import com.hbm.interfaces.TriggerHandler;
import com.hbm.interfaces.TriggerResult;
import com.hbm.inventory.material.Materials;
import com.hbm.items.ModItems;
import com.hbm.particle.helper.ExplosionCreator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CrashedBombBlock extends Block
        implements TickingEntityBlock, ToolInteractionHandler, TriggerHandler {
    private static void crashedBombEffect(Level level, Vec3 at, boolean balefire) {
        MiniNukeAftermath.soundWithoutVibration(level, at, 15F, 1F);
        MiniNukeAftermath.cloud(level, at, 0D, balefire);
    }

    public enum EnumDudType {
        BALEFIRE,
        CONVENTIONAL,
        NUKE,
        SALTED
    }

    public final EnumDudType type;
    private final MapCodec<CrashedBombBlock> codec;

    public CrashedBombBlock(Properties properties, EnumDudType type) {
        super(properties);
        this.type = type;
        codec = simpleCodec(blockProperties -> new CrashedBombBlock(blockProperties, type));
    }

    @Override
    protected MapCodec<CrashedBombBlock> codec() {
        return codec;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrashedBombBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean onToolUse(
            Level level,
            @Nullable Player player,
            BlockPos pos,
            @Nullable Direction face,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.DEFUSER) {
            return false;
        }
        if (!level.isClientSide()) {
            var center = Vec3.atCenterOf(pos);
            switch (type) {
                case BALEFIRE ->
                        spawnSalvage(
                                level, center, new ItemStack(ModItems.EGG_BALEFIRE_SHARD.get()));
                case CONVENTIONAL ->
                        spawnSalvage(level, center, new ItemStack(ModItems.BALL_TNT.get(), 16));
                case NUKE -> {
                    spawnSalvage(level, center, new ItemStack(ModItems.BALL_TNT.get(), 8));
                    spawnSalvage(
                            level, center, new ItemStack(ModItems.billet(Materials.PLUTONIUM), 4));
                }
                case SALTED -> {
                    spawnSalvage(level, center, new ItemStack(ModItems.BALL_TNT.get(), 8));
                    spawnSalvage(
                            level, center, new ItemStack(ModItems.billet(Materials.PLUTONIUM), 2));
                    spawnSalvage(
                            level, center, new ItemStack(ModItems.ingot(Materials.COBALT), 12));
                }
            }
            level.destroyBlock(pos, false);
        }
        return true;
    }

    private static void spawnSalvage(Level level, Vec3 center, ItemStack stack) {
        level.addFreshEntity(new ItemEntity(level, center.x, center.y, center.z, stack));
    }

    @Override
    public TriggerResult trigger(Level level, BlockPos pos, @Nullable Entity initiator) {
        if (level.isClientSide()) {
            return TriggerResult.DETONATED;
        }

        level.removeBlock(pos, false);
        var center = Vec3.atCenterOf(pos);
        switch (type) {
            case CONVENTIONAL -> {
                new ComposableExplosion(level, center.x, center.y, center.z, 35F)
                        .blockSelector(new AttenuatedRayBlockSelector(24))
                        .blockProcessor(CellDestruction.noDrops())
                        .entityProcessor(new MultiPointExplosionEntityProcessor(5D).range(1.5F))
                        .explode();
                ExplosionCreator.composeEffectLarge(level, center.x, center.y, center.z);
            }
            case BALEFIRE -> {
                var balefire = new BalefireEntity(ModEntities.BALEFIRE.get(), level);
                balefire.setPos(pos.getX(), pos.getY(), pos.getZ());
                balefire.destructionRange = (int) (ExplosionData.FATMAN_RADIUS.get() * 1.25D);
                level.addFreshEntity(balefire);
                crashedBombEffect(level, center, true);
            }
            case NUKE, SALTED -> {
                int radius = type == EnumDudType.NUKE ? 35 : 25;
                var explosion =
                        NukeExplosionMK5Entity.statFac(level, radius, center.x, center.y, center.z);
                if (type == EnumDudType.SALTED) {
                    explosion.moreFallout(25);
                }
                level.addFreshEntity(explosion);
                boolean balefire = MiniNukeAftermath.balefireFlash(level.getRandom());
                crashedBombEffect(level, center, balefire);
            }
        }
        return TriggerResult.DETONATED;
    }
}
