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

package com.hbm.client.model;

import com.hbm.blocks.network.FluidDuctGaugeBlock;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record GaugeDuctModel() implements SimpleBlockModel {

    @Override
    public BlockStateModel.UnbakedRoot root(Block block, BlockState state) {
        return new Root(BlockModel.base(block), state);
    }

    public record Root(Identifier carrier, BlockState state) implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Direction facing = state.getValue(FluidDuctGaugeBlock.FACING);
            Material.Baked steel = BlockModel.slot(baker, carrier, slots, "base");
            Material.Baked overlay = BlockModel.slot(baker, carrier, slots, "overlay");
            Material.Baked gauge = BlockModel.slot(baker, carrier, slots, "gauge");

            QuadCollection.Builder b = new QuadCollection.Builder();
            for (Direction dir : Direction.VALUES) {
                b.addCulledFace(dir, Boxes.face(baker, Boxes.UNIT_FROM, Boxes.UNIT_TO, dir, steel));
                b.addCulledFace(
                        dir,
                        Boxes.face(
                                baker,
                                Boxes.UNIT_FROM,
                                Boxes.UNIT_TO,
                                dir,
                                dir == facing ? gauge : overlay));
            }
            return b.build();
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return state.getValue(FluidDuctGaugeBlock.FACING);
        }
    }
}
