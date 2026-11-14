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

import com.google.common.base.Suppliers;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.ExplosionBlockSelector;
import com.hbm.explosion.vanillant.interfaces.ExplosionBlockProcessor;
import com.hbm.util.RegistryUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class Erosion {

    private static final Supplier<Erosion> CONCRETE =
            Suppliers.memoize(
                    () -> {
                        var gravel = Blocks.GRAVEL.defaultBlockState();
                        return new Erosion(
                                new Block[] {
                                    hbm("concrete"),
                                    hbm("concrete_smooth"),
                                    hbm("brick_concrete"),
                                    hbm("brick_concrete_broken")
                                },
                                new BlockState[] {
                                    gravel,
                                    gravel,
                                    hbm("brick_concrete_broken").defaultBlockState(),
                                    gravel
                                });
                    });

    private final Block[] erodible;
    private final BlockState[] products;

    private Erosion(Block[] erodible, BlockState[] products) {
        this.erodible = erodible;
        this.products = products;
    }

    public static Erosion concrete() {
        return CONCRETE.get();
    }

    private static Block hbm(String name) {
        return BuiltInRegistries.BLOCK.getOptional(RegistryUtil.id(name)).orElseThrow();
    }

    public @Nullable BlockState productOf(BlockState state) {
        var block = state.getBlock();
        for (int i = 0; i < erodible.length; i++) {
            if (erodible[i] == block) return products[i];
        }
        return null;
    }

    public ExplosionBlockSelector rays(int resolution) {
        return new Rays(ExplosionRayDirections.checkResolution(resolution), this);
    }

    public ExplosionBlockProcessor handler(float chance) {
        return new Destruction(this, chance);
    }

    private record Rays(int resolution, Erosion erosion) implements ExplosionBlockSelector {

        private static final float FADE = 0.3F * 0.75F;

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
                        open = true;
                        resists = ComposableExplosion.resists(state);
                        if (resists) loss = (blast.resistance(level, pos, state) + 0.3F) * 0.3F;
                    }
                    if (resists) power -= loss;
                    if (power > 0F) {
                        if (open
                                && (!gated || blast.shouldExplodeBlock(level, pos, state, power))) {
                            cells.add(pos);
                            open = false;
                        }
                    } else if (erosion.productOf(state) != null) {
                        cells.add(pos);
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

    private record Destruction(Erosion erosion, float chance) implements ExplosionBlockProcessor {

        @Override
        public void process(
                ComposableExplosion blast,
                ServerLevel level,
                double x,
                double y,
                double z,
                Set<BlockPos> cells) {
            var order = new ObjectArrayList<BlockPos>(cells);
            var random = level.getRandom();
            Util.shuffle(order, random);
            var drops = new CellDestruction.Drops();
            for (int i = 0, n = order.size(); i < n; i++) {
                var pos = order.get(i);
                var state = level.getBlockState(pos);
                if (state.isAir()) {
                    cells.remove(pos);
                    continue;
                }
                var product = erosion.productOf(state);
                boolean erodes = product != null && random.nextFloat() < chance;
                state.onExplosionHit(level, pos, blast, erodes ? CellDestruction.DISCARD : drops);
                if (erodes) level.setBlock(pos, product, Block.UPDATE_ALL);
            }
            drops.pop(level);
        }
    }
}
