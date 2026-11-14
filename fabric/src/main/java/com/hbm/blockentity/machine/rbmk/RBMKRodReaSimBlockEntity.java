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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKColumnType;
import com.hbm.blockentity.machine.rbmk.RBMKRodBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RBMKRodReaSimBlockEntity extends RBMKRodBlockEntity {
    private static final Vec3[][] FANS = new Vec3[4][8];

    static {
        for (int start = 0; start < FANS.length; start++) {
            Vec3 ray = turned(Vec3.X_AXIS, start * 9D);
            for (int i = 0; i < FANS[start].length; i++) {
                FANS[start][i] = ray;
                ray = turned(ray, 45D);
            }
        }
    }

    public RBMKRodReaSimBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.RBMK_ROD_REASIM.get(), pos, state);
    }

    protected RBMKRodReaSimBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private static Vec3 turned(Vec3 ray, double degrees) {
        double radians = degrees / 180D * Math.PI;
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(ray.x * cos + ray.z * sin, 0, ray.z * cos - ray.x * sin);
    }

    @Override
    public void spreadFlux(double flux, double ratio) {
        if (flux == 0D) return;
        for (Vec3 ray : FANS[level.getRandom().nextInt(FANS.length)])
            RbmkRay.emit(this, ray, flux * 0.75, ratio);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkReaSim");
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.FUEL_SIM;
    }
}
