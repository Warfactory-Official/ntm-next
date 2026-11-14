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
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public record AttenuatedRayBlockSelector(int resolution, boolean liquidsTransparent)
        implements ExplosionBlockSelector {

    private static final float FADE = 0.3F * 0.75F;

    public AttenuatedRayBlockSelector {
        ExplosionRayDirections.checkResolution(resolution);
    }

    public AttenuatedRayBlockSelector(int resolution) {
        this(resolution, false);
    }

    public AttenuatedRayBlockSelector() {
        this(16, false);
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
        var random = level.getRandom();
        boolean gated = blast.getDirectSourceEntity() != null;
        for (int ray = 0; ray < steps.length; ray += 3) {
            double sx = steps[ray], sy = steps[ray + 1], sz = steps[ray + 2];
            float power = size * (0.7F + random.nextFloat() * 0.6F);
            double px = x, py = y, pz = z;
            int cx = 0, cy = 0, cz = 0;
            BlockPos pos = null;
            BlockState state = null;
            boolean resists = false, open = false;
            float loss = 0F;
            while (power > 0F) {
                int bx = Mth.floor(px), by = Mth.floor(py), bz = Mth.floor(pz);
                if (pos == null || bx != cx || by != cy || bz != cz) {
                    cx = bx;
                    cy = by;
                    cz = bz;
                    pos = new BlockPos(bx, by, bz);
                    if (!level.isInWorldBounds(pos)) break;
                    state = level.getBlockState(pos);
                    open = !liquidsTransparent || !state.liquid();
                    resists = open && ComposableExplosion.resists(state);
                    if (resists) {
                        float resistance =
                                liquidsTransparent
                                        ? blast.blockResistance(level, pos, state)
                                        : blast.resistance(level, pos, state);
                        loss = (resistance + 0.3F) * 0.3F;
                    }
                }
                if (resists) power -= loss;
                if (open
                        && power > 0F
                        && (!gated || blast.shouldExplodeBlock(level, pos, state, power))) {
                    cells.add(pos);
                    open = false;
                }
                px += sx;
                py += sy;
                pz += sz;
                power -= FADE;
            }
        }
        return cells;
    }
}
