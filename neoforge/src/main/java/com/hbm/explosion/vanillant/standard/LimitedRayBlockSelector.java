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

package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.ExplosionBlockSelector;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public record LimitedRayBlockSelector(
        double maxResistance, int resolution, @Nullable Predicate<BlockState> stop)
        implements ExplosionBlockSelector {

    public LimitedRayBlockSelector {
        ExplosionRayDirections.checkResolution(resolution);
    }

    public LimitedRayBlockSelector(double maxResistance, int resolution) {
        this(maxResistance, resolution, null);
    }

    public LimitedRayBlockSelector(double maxResistance, Predicate<BlockState> stop) {
        this(maxResistance, 16, stop);
    }

    @Override
    public Set<BlockPos> select(
            ComposableExplosion blast,
            ServerLevel level,
            double x,
            double y,
            double z,
            float size) {
        var cells = new ObjectOpenHashSet<BlockPos>();
        var steps = ExplosionRayDirections.steps(resolution);
        boolean gated = blast.getDirectSourceEntity() != null;
        for (int ray = 0; ray < steps.length; ray += 3) {
            double sx = steps[ray], sy = steps[ray + 1], sz = steps[ray + 2];
            double px = x, py = y, pz = z;
            int cx = 0, cy = 0, cz = 0;
            BlockPos pos = null;
            while (true) {
                int bx = Mth.floor(px), by = Mth.floor(py), bz = Mth.floor(pz);
                if (pos == null || bx != cx || by != cy || bz != cz) {
                    cx = bx;
                    cy = by;
                    cz = bz;
                    pos = new BlockPos(bx, by, bz);
                    if (!level.isInWorldBounds(pos)) break;
                    var state = level.getBlockState(pos);
                    if (ComposableExplosion.resists(state)
                            && blast.resistance(level, pos, state) > maxResistance) break;
                    if (stop != null && !state.isAir() && stop.test(state)) break;
                    if (!gated || blast.shouldExplodeBlock(level, pos, state, size)) cells.add(pos);
                }
                double dx = px - x, dy = py - y, dz = pz - z;
                if (Math.sqrt(dx * dx + dy * dy + dz * dz) > size) break;
                px += sx;
                py += sy;
                pz += sz;
            }
        }
        return cells;
    }
}
