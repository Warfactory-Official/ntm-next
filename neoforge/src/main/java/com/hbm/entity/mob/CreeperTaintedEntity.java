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

package com.hbm.entity.mob;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.TaintBlock;
import com.hbm.data.ItemData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;

public class CreeperTaintedEntity extends CreeperBaseEntity {

    public CreeperTaintedEntity(EntityType<? extends CreeperTaintedEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D);
    }

    @Override
    public void tick() {
        super.tick();

        if (isAlive() && getHealth() < getMaxHealth() && tickCount % 10 == 0) heal(1.0F);
    }

    @Override
    public void detonate(ServerLevel level) {
        level.explode(this, getX(), getY(), getZ(), 5.0F, Level.ExplosionInteraction.NONE);

        if (!EventHooks.canEntityGrief(level, (Entity) this)) return;

        boolean trails = ItemData.TAINT_TRAILS.get();

        if (isPowered()) {
            scatter(level, 255, 15, 7, trails ? 0 : 5, 3);
        } else {
            scatter(level, 85, 7, 3, trails ? 4 : 10, trails ? 3 : 6);
        }
    }

    private void scatter(
            ServerLevel level, int count, int spread, int offset, int ageBase, int ageSpread) {
        for (int i = 0; i < count; i++) {
            BlockPos pos =
                    new BlockPos(
                            random.nextInt(spread) + (int) getX() - offset,
                            random.nextInt(spread) + (int) getY() - offset,
                            random.nextInt(spread) + (int) getZ() - offset);

            BlockState state = level.getBlockState(pos);
            if (!state.isSolidRender() || state.isAir()) continue;

            level.setBlock(
                    pos,
                    ModBlocks.TAINT
                            .get()
                            .defaultBlockState()
                            .setValue(TaintBlock.AGE, random.nextInt(ageSpread) + ageBase),
                    Block.UPDATE_CLIENTS);
        }
    }
}
