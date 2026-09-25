// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import java.util.List;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
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

public record TurretHowardDamagedModel() implements BlockModel<HFRWavefrontObject> {
    private static final Identifier OBJ = Library.id("models/turrets/turret_chekhov.obj");
    private static final String[] BASE = {"Base"};

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(BlockModel.base(block), prepared, state);
    }

    public record Root(Identifier carrier, HFRWavefrontObject obj, BlockState state)
            implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.SINGLE_SLOT);

            Direction facing = state.getValue(BlockMultiblockCore.FACING);

            float x = facing == Direction.NORTH || facing == Direction.WEST ? .5F : -.5F;
            float z = facing == Direction.NORTH || facing == Direction.EAST ? .5F : -.5F;
            return ObjUnbakedGeometry.bakeGroups(
                    List.of(
                            ObjUnbakedGeometry.Group.opaque(
                                    obj, BASE, material, BlockModelRotation.IDENTITY)),
                    x,
                    0F,
                    z);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return state.getValue(BlockMultiblockCore.FACING);
        }
    }
}
