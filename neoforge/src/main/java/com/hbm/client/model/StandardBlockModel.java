// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public record StandardBlockModel() implements SimpleBlockModel {

    @Override
    public BlockStateModel.UnbakedRoot root(Block block, BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return new Root(id.withPath("block/" + id.getPath()));
    }

    public record Root(Identifier carrier) implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState state, ModelBaker baker, ResolvedModel carrier, TextureSlots slots) {
            QuadCollection declared = carrier.bakeTopGeometry(slots, baker, Boxes.IDENTITY);
            List<ObjUnbakedGeometry.BoxQuad> quads = new ArrayList<>(declared.getAll().size());
            add(quads, declared, null);
            for (Direction dir : Direction.VALUES) add(quads, declared, dir);
            return ObjUnbakedGeometry.bakeGroups(List.of(), quads, 0F, 0F, 0F);
        }

        private static void add(
                List<ObjUnbakedGeometry.BoxQuad> into,
                QuadCollection declared,
                @Nullable Direction cull) {
            for (BakedQuad quad : declared.getQuads(cull)) {
                into.add(
                        new ObjUnbakedGeometry.BoxQuad(
                                quad, ObjUnbakedGeometry.WHITE, cull, QuadLighting.cell(0, 0, 0)));
            }
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return this;
        }
    }
}
