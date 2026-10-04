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

        boolean pX = BoxConduitShapes.maskHas(mask, Direction.EAST);
        boolean nX = BoxConduitShapes.maskHas(mask, Direction.WEST);
        boolean pY = BoxConduitShapes.maskHas(mask, Direction.UP);
        boolean nY = BoxConduitShapes.maskHas(mask, Direction.DOWN);
        boolean pZ = BoxConduitShapes.maskHas(mask, Direction.SOUTH);
        boolean nZ = BoxConduitShapes.maskHas(mask, Direction.NORTH);
        int count = Integer.bitCount(mask);

        float l = 2 + size, u = 14 - size;
        float jl = hub + size, ju = 16 - hub - size;

        int[] rot = new int[6];
        if ((mask & 0b001111) == 0 && mask > 0) {
            rot[Direction.UP.ordinal()] = 1;
            rot[Direction.DOWN.ordinal()] = 1;
            rot[Direction.NORTH.ordinal()] = 2;
            rot[Direction.SOUTH.ordinal()] = 1;
            box(b, baker, base, slots, tinted, mask, rot, 0, l, l, 16, u, u);
        } else if ((mask & 0b111100) == 0 && mask > 0) {
            rot[Direction.WEST.ordinal()] = 1;
            rot[Direction.EAST.ordinal()] = 2;
            box(b, baker, base, slots, tinted, mask, rot, l, l, 0, u, u, 16);
        } else if ((mask & 0b110011) == 0 && mask > 0) {
            box(b, baker, base, slots, tinted, mask, rot, l, 0, l, u, 16, u);
        } else if (count == 2) {
            if ((nY || pY) && (pX || nX)) {
                rot[Direction.UP.ordinal()] = 1;
                rot[Direction.DOWN.ordinal()] = 1;
            }
            if (!nY && !pY) {
                rot[Direction.WEST.ordinal()] = 1;
                rot[Direction.EAST.ordinal()] = 2;
                rot[Direction.NORTH.ordinal()] = 2;
                rot[Direction.SOUTH.ordinal()] = 1;
            }
            box(b, baker, base, slots, tinted, mask, rot, l, l, l, u, u, u);
            if (nY) box(b, baker, base, slots, tinted, mask, rot, l, 0, l, u, l, u);
            if (pY) box(b, baker, base, slots, tinted, mask, rot, l, u, l, u, 16, u);
            if (nX) box(b, baker, base, slots, tinted, mask, rot, 0, l, l, l, u, u);
            if (pX) box(b, baker, base, slots, tinted, mask, rot, u, l, l, 16, u, u);
            if (nZ) box(b, baker, base, slots, tinted, mask, rot, l, l, 0, u, u, l);
            if (pZ) box(b, baker, base, slots, tinted, mask, rot, l, l, u, u, u, 16);
        } else {
            box(b, baker, base, slots, tinted, mask, rot, jl, jl, jl, ju, ju, ju);
            if (nY) box(b, baker, base, slots, tinted, mask, rot, l, 0, l, u, jl, u);
            if (pY) box(b, baker, base, slots, tinted, mask, rot, l, ju, l, u, 16, u);
            if (nX) box(b, baker, base, slots, tinted, mask, rot, 0, l, l, jl, u, u);
            if (pX) box(b, baker, base, slots, tinted, mask, rot, ju, l, l, 16, u, u);
            if (nZ) box(b, baker, base, slots, tinted, mask, rot, l, l, 0, u, u, jl);
            if (pZ) box(b, baker, base, slots, tinted, mask, rot, l, l, ju, u, u, 16);
        }

        return b.build();
    }

    private static void box(
            QuadCollection.Builder b,
            ModelBaker baker,
            ResolvedModel base,
            TextureSlots slots,
            boolean tinted,
            int mask,
            int[] rot,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1) {
        Vector3f from = new Vector3f(x0, y0, z0);
        Vector3f to = new Vector3f(x1, y1, z1);
        int tint = tinted ? 0 : CuboidFace.NO_TINT;
        UvRotation[] uvs =
                (new UvRotation[] {
                    switch (rot[0]) {
                        case 0 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(from.x(), to.z(), to.x(), from.z()),
                                        Quadrant.R0));
                        case 1 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                16F - to.z(), to.x(), 16F - from.z(), from.x()),
                                        Quadrant.R90));
                        case 2 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                to.z(), 16F - to.x(), from.z(), 16F - from.x()),
                                        Quadrant.R90));
                        case 3 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                16F - from.x(),
                                                16F - to.z(),
                                                16F - to.x(),
                                                16F - from.z()),
                                        Quadrant.R0));
                        default ->
                                throw new IllegalArgumentException(
                                        "uvRotate value " + rot[0] + " outside 0..3");
                    },
                    switch (rot[1]) {
                        case 0 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(from.x(), from.z(), to.x(), to.z()),
                                        Quadrant.R0));
                        case 1 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                from.z(), 16F - to.x(), to.z(), 16F - from.x()),
                                        Quadrant.R90));
                        case 2 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                16F - from.z(), to.x(), 16F - to.z(), from.x()),
                                        Quadrant.R90));
                        case 3 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                16F - from.x(),
                                                16F - from.z(),
                                                16F - to.x(),
                                                16F - to.z()),
                                        Quadrant.R0));
                        default ->
                                throw new IllegalArgumentException(
                                        "uvRotate value " + rot[1] + " outside 0..3");
                    },
                    switch (rot[2]) {
                        case 0 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                16F - to.x(),
                                                16F - to.y(),
                                                16F - from.x(),
                                                16F - from.y()),
                                        Quadrant.R0));
                        case 1 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                16F - from.y(), from.x(), 16F - to.y(), to.x()),
                                        Quadrant.R90));
                        case 2 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                from.y(), 16F - from.x(), to.y(), 16F - to.x()),
                                        Quadrant.R90));
                        case 3 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                16F - from.x(), to.y(), 16F - to.x(), from.y()),
                                        Quadrant.R0));
                        default ->
                                throw new IllegalArgumentException(
                                        "uvRotate value " + rot[2] + " outside 0..3");
                    },
                    switch (rot[3]) {
                        case 0 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                from.x(), 16F - to.y(), to.x(), 16F - from.y()),
                                        Quadrant.R0));
                        case 1 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                from.y(), 16F - to.x(), to.y(), 16F - from.x()),
                                        Quadrant.R90));
                        case 2 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                16F - from.y(), to.x(), 16F - to.y(), from.x()),
                                        Quadrant.R90));
                        case 3 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                16F - from.x(), to.y(), 16F - to.x(), from.y()),
                                        Quadrant.R0));
                        default ->
                                throw new IllegalArgumentException(
                                        "uvRotate value " + rot[3] + " outside 0..3");
                    },
                    switch (rot[4]) {
                        case 0 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                from.z(), 16F - to.y(), to.z(), 16F - from.y()),
                                        Quadrant.R0));
                        case 1 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                from.y(), 16F - to.z(), to.y(), 16F - from.z()),
                                        Quadrant.R90));
                        case 2 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                16F - from.y(), to.z(), 16F - to.y(), from.z()),
                                        Quadrant.R90));
                        case 3 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                16F - from.z(), to.y(), 16F - to.z(), from.y()),
                                        Quadrant.R0));
                        default ->
                                throw new IllegalArgumentException(
                                        "uvRotate value " + rot[4] + " outside 0..3");
                    },
                    switch (rot[5]) {
                        case 0 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                16F - to.z(),
                                                16F - to.y(),
                                                16F - from.z(),
                                                16F - from.y()),
                                        Quadrant.R0));
                        case 1 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                16F - from.y(), from.z(), 16F - to.y(), to.z()),
                                        Quadrant.R90));
                        case 2 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                from.y(), 16F - from.z(), to.y(), 16F - to.z()),
                                        Quadrant.R90));
                        case 3 ->
                                (new UvRotation(
                                        new CuboidFace.UVs(
                                                16F - from.z(), to.y(), 16F - to.z(), from.y()),
                                        Quadrant.R0));
                        default ->
                                throw new IllegalArgumentException(
                                        "uvRotate value " + rot[5] + " outside 0..3");
                    }
                });
        for (Direction dir : DIRS) {
            Material.Baked material = BlockModel.slot(baker, base, slots, slotFor(mask, dir));
            UvRotation uv = uvs[dir.ordinal()];
            b.addUnculledFace(
                    Boxes.face(
                            baker,
                            from,
                            to,
                            dir,
                            material,
                            uv.uvs(),
                            uv.quadrant(),
                            tint,
                            Boxes.IDENTITY));
        }
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
