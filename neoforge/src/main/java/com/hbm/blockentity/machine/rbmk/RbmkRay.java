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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKBaseBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKRodBlockEntity;
import com.hbm.data.MachineData;
import com.hbm.handler.neutron.NeutronHandler;
import com.hbm.handler.neutron.NeutronRay;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.util.ChunkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public record RbmkRay(RBMKRodBlockEntity source, Vec3 direction, double flux, double fastRatio)
        implements NeutronRay {

    private static final double RADIATION_PER_FLUX = 0.05D;
    private static final double SATURATION_PER_EMISSION = 1024D;

    private static final BlockPos.MutableBlockPos CELL = new BlockPos.MutableBlockPos();
    private static final BlockPos.MutableBlockPos ESCAPE = new BlockPos.MutableBlockPos();
    private static final BlockPos.MutableBlockPos SPAN_CELL = new BlockPos.MutableBlockPos();

    private static int span;
    private static int range;
    private static double moderation;
    private static double reflection;
    private static double absorption;
    private static double heatPerFlux;

    public static void emit(
            RBMKRodBlockEntity source, Vec3 direction, double flux, double fastRatio) {
        if (flux == 0D) return;
        NeutronHandler.enqueue(
                (ServerLevel) source.getLevel(), new RbmkRay(source, direction, flux, fastRatio));
    }

    public static void readDials() {
        span = MachineData.RBMK_COLUMN_HEIGHT.get();
        range = MachineData.RBMK_FLUX_RANGE.get();
        moderation = MachineData.RBMK_MODERATOR_EFFICIENCY.get();
        reflection = MachineData.RBMK_REFLECTOR_EFFICIENCY.get();
        absorption = MachineData.RBMK_ABSORBER_EFFICIENCY.get();
        heatPerFlux = MachineData.RBMK_ABSORBER_HEAT_CONVERSION.get();
    }

    public static int occlusion(ServerLevel level, BlockPos cell) {
        var chunk = level.getChunkSource().getChunkNow(cell.getX() >> 4, cell.getZ() >> 4);
        if (chunk == null) return span;
        int hits = 0;
        for (int j = 0; j < span; j++) {
            if (chunk.getBlockState(SPAN_CELL.set(cell.getX(), cell.getY() + j, cell.getZ()))
                    .canOcclude()) hits++;
        }
        return hits;
    }

    @Override
    public void travel(ServerLevel level) {
        double flux = this.flux;
        if (flux == 0D) return;
        BlockPos origin = source.getBlockPos();
        if (source.isRemoved() || ChunkUtil.blockEntityIfLoaded(level, origin) != source) return;
        double ratio = fastRatio;
        int moderations = 0;
        boolean endsInColumn = false;
        for (int i = 1; i <= range; i++) {
            if (flux == 0D) return;
            CELL.set(
                    origin.getX() + Mth.floor(0.5D + direction.x * i),
                    origin.getY(),
                    origin.getZ() + Mth.floor(0.5D + direction.z * i));
            if (!(ChunkUtil.blockEntityIfLoaded(level, CELL)
                    instanceof RBMKBaseBlockEntity column)) {
                endsInColumn = false;
                int hits = occlusion(level, CELL);
                if (hits == span) return;
                double open = 1D - (double) hits / span;
                leak(level, origin, flux * RADIATION_PER_FLUX * open);
                flux *= open;
                continue;
            }
            endsInColumn = true;
            var role = column.fluxRole();
            if (role == FluxRole.INERT) continue;
            if (!column.hasLid()) {
                double emission = flux * RADIATION_PER_FLUX;
                RadiationSystemNT.incrementRad(
                        level, CELL, emission, emission * SATURATION_PER_EMISSION);
            }
            if (role == FluxRole.MODERATOR || column.isModerated()) {
                moderations++;
                ratio *= 1D - moderation;
            }
            switch (role) {
                case FUEL -> {
                    var rod = (RBMKRodBlockEntity) column;
                    if (rod.hasRod) {
                        rod.absorbFlux(flux, ratio);
                        return;
                    }
                }
                case OUTGASSER -> {
                    var outgasser = (RBMKIrradiationColumnBlockEntity) column;
                    if (outgasser.isReady()) {
                        outgasser.absorbFlux(flux, ratio);
                        return;
                    }
                }
                case CONTROL -> {
                    double extraction = ((RBMKControlBlockEntity) column).fluxMultiplier();
                    if (extraction <= 0D) return;
                    flux *= extraction;
                }
                case REFLECTOR -> {
                    if (source.isModerated()) moderations++;
                    if (ratio > 0D) for (int m = 0; m < moderations; m++) ratio *= 1D - moderation;
                    if (reflection == 1D) {
                        if (source.hasRod) source.absorbFlux(flux, ratio);
                        return;
                    }
                    flux *= reflection;
                }
                case ABSORBER -> {
                    column.hbm$syncSet$RBMKBaseBlockEntity$heat(
                            (double) (column.heat + (heatPerFlux * flux)));
                    column.markChanged();
                    if (absorption == 1D) return;
                    flux *= absorption;
                }
                default -> {}
            }
        }
        if (endsInColumn) return;
        ESCAPE.set(
                Mth.floor(origin.getX() + direction.x),
                origin.getY(),
                Mth.floor(origin.getZ() + direction.z));
        leak(
                level,
                ESCAPE,
                flux * RADIATION_PER_FLUX * (1D - (double) occlusion(level, ESCAPE) / span));
    }

    private static void leak(ServerLevel level, BlockPos at, double emission) {
        RadiationSystemNT.incrementRad(
                level, at, emission, Math.abs(emission) * SATURATION_PER_EMISSION + 1D);
    }
}
