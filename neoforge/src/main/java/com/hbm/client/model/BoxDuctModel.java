// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.network.BoxConduitShapes;
import com.hbm.blocks.network.CableBoxBlock;
import com.hbm.blocks.network.FluidDuctBoxBlock;
import com.mojang.math.Quadrant;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public record BoxDuctModel() implements SimpleBlockModel {

    private static final Direction[] DIRS = Direction.VALUES;

    private static QuadCollection build(
            ModelBaker baker,
            ResolvedModel base,
            TextureSlots slots,
            int size,
            boolean tinted,
            int hub,
            int mask) {
        QuadCollection.Builder b = new QuadCollection.Builder();
        int key = ((hub - 1) * 5 + size) * 64 + mask;
        int tint = tinted ? 0 : CuboidFace.NO_TINT;
        for (int part = BoxDuctData.OFFSETS[key]; part < BoxDuctData.OFFSETS[key + 1]; part++) {
            int at = part * 30;
            float[] values = BoxDuctData.VALUES;
            Vector3f from = new Vector3f(values[at], values[at + 1], values[at + 2]);
            Vector3f to = new Vector3f(values[at + 3], values[at + 4], values[at + 5]);
            for (Direction dir : DIRS) {
                int face = dir.ordinal();
                int uv = at + 6 + face * 4;
                CuboidFace.UVs window =
                        new CuboidFace.UVs(
                                values[uv], values[uv + 1], values[uv + 2], values[uv + 3]);
                Quadrant quadrant =
                        (BoxDuctData.QUADRANTS[part] & (1 << face)) == 0
                                ? Quadrant.R0
                                : Quadrant.R90;
                Material.Baked material = BlockModel.slot(baker, base, slots, slotFor(mask, dir));
                b.addUnculledFace(
                        Boxes.face(
                                baker,
                                from,
                                to,
                                dir,
                                material,
                                window,
                                quadrant,
                                tint,
                                Boxes.IDENTITY));
            }
        }
        return b.build();
    }

    private static String slotFor(int mask, Direction side) {
        boolean pX = BoxConduitShapes.maskHas(mask, Direction.EAST);
        boolean nX = BoxConduitShapes.maskHas(mask, Direction.WEST);
        boolean pY = BoxConduitShapes.maskHas(mask, Direction.UP);
        boolean nY = BoxConduitShapes.maskHas(mask, Direction.DOWN);
        boolean pZ = BoxConduitShapes.maskHas(mask, Direction.SOUTH);
        boolean nZ = BoxConduitShapes.maskHas(mask, Direction.NORTH);
        int count = Integer.bitCount(mask);

        if ((mask & 0b001111) == 0 && mask > 0) {
            return side.getAxis() == Direction.Axis.X ? "end" : "straight";
        } else if ((mask & 0b111100) == 0 && mask > 0) {
            return side.getAxis() == Direction.Axis.Z ? "end" : "straight";
        } else if ((mask & 0b110011) == 0 && mask > 0) {
            return side.getAxis() == Direction.Axis.Y ? "end" : "straight";
        } else if (count == 2) {
            if (side == Direction.DOWN && nY
                    || side == Direction.UP && pY
                    || side == Direction.NORTH && nZ
                    || side == Direction.SOUTH && pZ
                    || side == Direction.WEST && nX
                    || side == Direction.EAST && pX) return "end";
            if (side == Direction.UP && nY
                    || side == Direction.DOWN && pY
                    || side == Direction.SOUTH && nZ
                    || side == Direction.NORTH && pZ
                    || side == Direction.EAST && nX
                    || side == Direction.WEST && pX) return "straight";

            if (nY && pZ) return side == Direction.WEST ? "curve_br" : "curve_bl";
            if (nY && nZ) return side == Direction.EAST ? "curve_br" : "curve_bl";
            if (nY && pX) return side == Direction.SOUTH ? "curve_br" : "curve_bl";
            if (nY && nX) return side == Direction.NORTH ? "curve_br" : "curve_bl";
            if (pY && pZ) return side == Direction.WEST ? "curve_tr" : "curve_tl";
            if (pY && nZ) return side == Direction.EAST ? "curve_tr" : "curve_tl";
            if (pY && pX) return side == Direction.SOUTH ? "curve_tr" : "curve_tl";
            if (pY && nX) return side == Direction.NORTH ? "curve_tr" : "curve_tl";

            if (pX && nZ) return "curve_tr";
            if (pX && pZ) return "curve_br";
            if (nX && nZ) return "curve_tl";
            if (nX && pZ) return "curve_bl";

            return "junction";
        }

        return "junction";
    }

    @Override
    public BlockStateModel.UnbakedRoot root(Block block, BlockState state) {

        if (block instanceof FluidDuctBoxBlock duct) {
            return new Root(
                    BlockModel.base(block),
                    duct.size,
                    duct.tinted,
                    BoxConduitShapes.DUCT_HUB,
                    state);
        }
        return new Root(
                BlockModel.base(block),
                ((CableBoxBlock) block).size,
                false,
                BoxConduitShapes.CABLE_HUB,
                state);
    }

    public record Root(Identifier carrier, int size, boolean tinted, int hub, BlockState state)
            implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            return build(baker, carrier, slots, size, tinted, hub, BoxConduitShapes.maskOf(state));
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return BoxConduitShapes.maskOf(state);
        }
    }
}
