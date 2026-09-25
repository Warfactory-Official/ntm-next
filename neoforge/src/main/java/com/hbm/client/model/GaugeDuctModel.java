// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
