// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public record CableModel() implements BlockModel<HFRWavefrontObject> {

    public static final Identifier BASE = Library.id("block/cable_base");
    private static final Identifier OBJ = Library.id("models/blocks/cable_neo.obj");
    private static final float OFFSET_Y = 0.5F;

    private static final int DOWN = 1, UP = 2, NORTH = 4, SOUTH = 8, WEST = 16, EAST = 32;

    private static final String[][] PARTS_BY_MASK = BlockModel.partsByMask(CableModel::selectParts);

    private static String[] selectParts(int mask) {
        boolean n = (mask & NORTH) != 0, e = (mask & EAST) != 0, s = (mask & SOUTH) != 0;
        boolean w = (mask & WEST) != 0, u = (mask & UP) != 0, d = (mask & DOWN) != 0;

        if (e && w && !u && !d && !n && !s) return new String[] {"CX"};
        if (u && d && !e && !w && !n && !s) return new String[] {"CY"};
        if (n && s && !e && !w && !u && !d) return new String[] {"CZ"};

        String[] parts = new String[1 + Integer.bitCount(mask)];
        int i = 0;
        parts[i++] = "Core";
        if (e) parts[i++] = "posX";
        if (w) parts[i++] = "negX";
        if (u) parts[i++] = "posY";
        if (d) parts[i++] = "negY";
        if (s) parts[i++] = "negZ";
        if (n) parts[i++] = "posZ";
        return parts;
    }

    public static String[] partsFor(BlockState state) {
        int mask = 0;
        if (state.getValue(BlockStateProperties.NORTH)) mask |= NORTH;
        if (state.getValue(BlockStateProperties.EAST)) mask |= EAST;
        if (state.getValue(BlockStateProperties.SOUTH)) mask |= SOUTH;
        if (state.getValue(BlockStateProperties.WEST)) mask |= WEST;
        if (state.getValue(BlockStateProperties.UP)) mask |= UP;
        if (state.getValue(BlockStateProperties.DOWN)) mask |= DOWN;
        return PARTS_BY_MASK[mask];
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(prepared, partsFor(state));
    }

    public record Root(HFRWavefrontObject obj, String[] parts) implements CarrierRoot {

        @Override
        public Identifier carrier() {
            return BASE;
        }

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.SINGLE_SLOT);
            return ObjUnbakedGeometry.bakeGroups(
                    obj, parts, material, BlockModelRotation.IDENTITY, 0F, OFFSET_Y, 0F, false);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {

            return parts;
        }
    }
}
