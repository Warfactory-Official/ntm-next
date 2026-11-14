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

package com.hbm.blockentity.encounter;

import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.TrappedBrickBlock;
import com.hbm.entity.projectile.RubbleEntity;
import com.hbm.items.ModItems;
import com.hbm.sound.WorldSounds;
import java.util.Arrays;
import java.util.Collections;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public final class TrappedBrickBlockEntity extends BlockEntity implements BlockEntityTicks {
    private @Nullable AABB detectionArea;
    private @Nullable Direction launchDirection;

    public TrappedBrickBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRAPPED_BRICK.get(), pos, state);
    }

    @Override
    public void tickServer() {
        var state = getBlockState();
        var kind = ((TrappedBrickBlock) state.getBlock()).trap;
        if (this.detectionArea == null) this.prepare(kind);
        if (level.getEntitiesOfClass(Player.class, this.detectionArea).isEmpty()) return;
        this.trigger((ServerLevel) level, kind);
        WorldSounds.machine(
                level,
                null,
                worldPosition,
                SoundEvents.UI_BUTTON_CLICK.value(),
                SoundSource.BLOCKS,
                0.3F,
                0.6F);
        level.setBlockAndUpdate(worldPosition, ModBlocks.BRICK_JUNGLE.get().defaultBlockState());
    }

    private void prepare(TrappedBrickBlock.Trap kind) {
        detectionArea =
                switch (kind) {
                    case FALLING_ROCKS, SPIDERS ->
                            new AABB(-1, -3, -1, 2, 0, 2).move(worldPosition);
                    case PILLAR -> new AABB(0.2, -3, 0.2, 0.8, 0, 0.8).move(worldPosition);
                    case ZOMBIE -> new AABB(-1, 1, -1, 2, 2, 2).move(worldPosition);
                    case ARROW, FLAMING_ARROW, POISON_DART -> directionalArea();
                    default -> throw new IllegalArgumentException();
                };
    }

    private AABB directionalArea() {
        var choices =
                Arrays.asList(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
        Collections.shuffle(choices);
        for (var direction : choices) {
            if (!level.getBlockState(worldPosition.relative(direction)).isAir()) continue;
            launchDirection = direction;
            return new AABB(0.4, 0.4, 0.4, 0.6, 0.6, 0.6)
                    .expandTowards(direction.getStepX() * 3, 0, direction.getStepZ() * 3)
                    .move(worldPosition);
        }
        return new AABB(worldPosition);
    }

    private void trigger(ServerLevel server, TrappedBrickBlock.Trap kind) {
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY();
        double z = worldPosition.getZ() + 0.5;
        switch (kind) {
            case FALLING_ROCKS -> {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        var rubble = new RubbleEntity(server, x + dx, y - 0.5, z + dz);
                        rubble.setBlockState(ModBlocks.REINFORCED_STONE.get().defaultBlockState());
                        server.addFreshEntity(rubble);
                    }
                }
            }
            case ARROW, FLAMING_ARROW -> {
                int dx = launchDirection == null ? 0 : launchDirection.getStepX();
                int dz = launchDirection == null ? 0 : launchDirection.getStepZ();
                var arrow =
                        new Arrow(
                                server, x + dx, y + 0.5, z + dz, new ItemStack(Items.ARROW), null);
                arrow.setDeltaMovement(dx, 0, dz);
                if (kind == TrappedBrickBlock.Trap.FLAMING_ARROW) arrow.igniteForSeconds(60);
                server.addFreshEntity(arrow);
            }
            case PILLAR -> {
                for (int depth = 1; depth <= 3; depth++) {
                    server.setBlockAndUpdate(
                            worldPosition.below(depth),
                            ModBlocks.CONCRETE_PILLAR.get().defaultBlockState());
                }
            }
            case ZOMBIE -> {
                var zombie = EntityTypes.ZOMBIE.create(server, EntitySpawnReason.TRIGGERED);
                if (zombie != null) {
                    zombie.setPos(x, y + 1, z);
                    var weapon =
                            switch (server.getRandom().nextInt(3)) {
                                case 0 -> ModItems.CHERNOBYLSIGN.get();
                                case 1 -> ModItems.COBALT_SWORD.get();
                                default -> ModItems.CMB_HOE.get();
                            };
                    zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));
                    zombie.setDropChance(EquipmentSlot.MAINHAND, 1);
                    server.addFreshEntity(zombie);
                }
            }
            case SPIDERS -> {
                for (int i = 0; i < 3; i++) {
                    var spider =
                            EntityTypes.CAVE_SPIDER.create(server, EntitySpawnReason.TRIGGERED);
                    if (spider != null) {
                        spider.setPos(x, y - 1, z);
                        server.addFreshEntity(spider);
                    }
                }
            }
            case POISON_DART -> {}
            default -> throw new IllegalArgumentException();
        }
    }
}
