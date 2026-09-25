// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.machine.Floodlight;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.math.OctahedralGroup;
import java.util.List;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record FloodlightModel() implements BlockModel<HFRWavefrontObject> {

    private static final Identifier OBJ = Library.id("models/blocks/floodlight.obj");
    private static final String[] BASE = {"Base"};

    private static OctahedralGroup rotationFor(int meta) {
        return switch (meta) {
            case 0 -> OctahedralGroup.BLOCK_ROT_X_180;
            case 1 -> OctahedralGroup.IDENTITY;
            case 2 -> OctahedralGroup.BLOCK_ROT_X_90.compose(OctahedralGroup.BLOCK_ROT_Y_90);
            case 3 -> OctahedralGroup.BLOCK_ROT_X_270.compose(OctahedralGroup.BLOCK_ROT_Y_270);
            case 4 -> OctahedralGroup.BLOCK_ROT_Z_270;
            case 5 -> OctahedralGroup.BLOCK_ROT_X_180.compose(OctahedralGroup.BLOCK_ROT_Z_90);
            case 6 -> OctahedralGroup.BLOCK_ROT_X_180.compose(OctahedralGroup.BLOCK_ROT_Y_270);
            case 7 -> OctahedralGroup.BLOCK_ROT_Y_270;
            default -> throw new IllegalStateException("Unexpected floodlight facing: " + meta);
        };
    }

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
            int facing = state.getValue(Floodlight.FACING);
            List<ObjUnbakedGeometry.Group> groups =
                    List.of(
                            ObjUnbakedGeometry.Group.opaque(
                                    obj,
                                    BASE,
                                    material,
                                    BlockModelRotation.get(rotationFor(facing))));
            return ObjUnbakedGeometry.bakeGroups(groups, 0F, 0F, 0F);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return state.getValue(Floodlight.FACING);
        }
    }
}
