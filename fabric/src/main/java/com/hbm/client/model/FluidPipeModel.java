// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import java.util.Arrays;
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

public record FluidPipeModel() implements BlockModel<HFRWavefrontObject> {

    public static final Identifier BASE = Library.id("block/fluid_pipe_base");
    private static final Identifier OBJ = Library.id("models/blocks/pipe_neo.obj");
    private static final float OFFSET_Y = 0.5F;

    private static final int NEG_Z = 1, POS_Z = 2, NEG_Y = 4, POS_Y = 8, NEG_X = 16, POS_X = 32;
    private static final String[][] PARTS_BY_MASK =
            BlockModel.partsByMask(FluidPipeModel::selectParts);

    private static String[] selectParts(int mask) {
        boolean pX = (mask & POS_X) != 0, nX = (mask & NEG_X) != 0;
        boolean pY = (mask & POS_Y) != 0, nY = (mask & NEG_Y) != 0;
        boolean pZ = (mask & POS_Z) != 0, nZ = (mask & NEG_Z) != 0;

        return switch (mask) {
            case 0 -> new String[] {"pX", "nX", "pY", "nY", "pZ", "nZ"};
            case POS_X, NEG_X, POS_X | NEG_X -> new String[] {"pX", "nX"};
            case POS_Y, NEG_Y, POS_Y | NEG_Y -> new String[] {"pY", "nY"};
            case POS_Z, NEG_Z, POS_Z | NEG_Z -> new String[] {"pZ", "nZ"};
            default -> {
                String[] parts = new String[14];
                int i = 0;
                if (pX) parts[i++] = "pX";
                if (nX) parts[i++] = "nX";
                if (pY) parts[i++] = "pY";
                if (nY) parts[i++] = "nY";
                if (pZ) parts[i++] = "nZ";
                if (nZ) parts[i++] = "pZ";

                if (!pX && !pY && !pZ) parts[i++] = "ppn";
                if (!pX && !pY && !nZ) parts[i++] = "ppp";
                if (!nX && !pY && !pZ) parts[i++] = "npn";
                if (!nX && !pY && !nZ) parts[i++] = "npp";
                if (!pX && !nY && !pZ) parts[i++] = "pnn";
                if (!pX && !nY && !nZ) parts[i++] = "pnp";
                if (!nX && !nY && !pZ) parts[i++] = "nnn";
                if (!nX && !nY && !nZ) parts[i++] = "nnp";
                yield Arrays.copyOf(parts, i);
            }
        };
    }

    public static String[] partsFor(BlockState state) {
        int mask = 0;
        if (state.getValue(BlockStateProperties.NORTH)) mask |= NEG_Z;
        if (state.getValue(BlockStateProperties.SOUTH)) mask |= POS_Z;
        if (state.getValue(BlockStateProperties.DOWN)) mask |= NEG_Y;
        if (state.getValue(BlockStateProperties.UP)) mask |= POS_Y;
        if (state.getValue(BlockStateProperties.WEST)) mask |= NEG_X;
        if (state.getValue(BlockStateProperties.EAST)) mask |= POS_X;
        return PARTS_BY_MASK[mask];
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(BlockModel.base(block), prepared, partsFor(state));
    }

    public record Root(Identifier carrier, HFRWavefrontObject obj, String[] parts)
            implements CarrierRoot {

        public Root(HFRWavefrontObject obj, String[] parts) {
            this(BASE, obj, parts);
        }

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.SINGLE_SLOT);
            Material.Baked overlay =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.OVERLAY_SLOT);
            return ObjUnbakedGeometry.bakeGroupsWithOverlay(
                    obj,
                    parts,
                    material,
                    overlay,
                    BlockModelRotation.IDENTITY,
                    0F,
                    OFFSET_Y,
                    0F,
                    1);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return parts;
        }
    }
}
