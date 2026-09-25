// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public record CableClassicModel() implements SimpleBlockModel {

    private static final float CORE_MIN = 5.5F;
    private static final float CORE_MAX = 10.5F;

    private static final CuboidFace.UVs CAP = new CuboidFace.UVs(0F, 0F, 5F, 5F);

    private static final CuboidFace.UVs CAP_DOWN = new CuboidFace.UVs(0F, 5F, 5F, 0F);
    private static final CuboidFace.UVs ARM = new CuboidFace.UVs(5F, 0F, 10F, 5F);

    private static final Quadrant[][] ARM_QUADRANT = armQuadrants();

    private static Quadrant[][] armQuadrants() {
        Quadrant[][] table = new Quadrant[6][6];
        put(table, Direction.DOWN, Quadrant.R90, Quadrant.R90, Quadrant.R90, Quadrant.R90);
        put(table, Direction.UP, Quadrant.R270, Quadrant.R270, Quadrant.R270, Quadrant.R270);
        table[Direction.NORTH.ordinal()][Direction.DOWN.ordinal()] = Quadrant.R90;
        table[Direction.NORTH.ordinal()][Direction.UP.ordinal()] = Quadrant.R270;
        table[Direction.NORTH.ordinal()][Direction.WEST.ordinal()] = Quadrant.R180;
        table[Direction.NORTH.ordinal()][Direction.EAST.ordinal()] = Quadrant.R0;
        table[Direction.SOUTH.ordinal()][Direction.DOWN.ordinal()] = Quadrant.R270;
        table[Direction.SOUTH.ordinal()][Direction.UP.ordinal()] = Quadrant.R90;
        table[Direction.SOUTH.ordinal()][Direction.WEST.ordinal()] = Quadrant.R0;
        table[Direction.SOUTH.ordinal()][Direction.EAST.ordinal()] = Quadrant.R180;
        table[Direction.WEST.ordinal()][Direction.DOWN.ordinal()] = Quadrant.R180;
        table[Direction.WEST.ordinal()][Direction.UP.ordinal()] = Quadrant.R180;
        table[Direction.WEST.ordinal()][Direction.NORTH.ordinal()] = Quadrant.R0;
        table[Direction.WEST.ordinal()][Direction.SOUTH.ordinal()] = Quadrant.R180;
        table[Direction.EAST.ordinal()][Direction.DOWN.ordinal()] = Quadrant.R0;
        table[Direction.EAST.ordinal()][Direction.UP.ordinal()] = Quadrant.R0;
        table[Direction.EAST.ordinal()][Direction.NORTH.ordinal()] = Quadrant.R180;
        table[Direction.EAST.ordinal()][Direction.SOUTH.ordinal()] = Quadrant.R0;
        return table;
    }

    private static void put(
            Quadrant[][] table,
            Direction arm,
            Quadrant north,
            Quadrant south,
            Quadrant west,
            Quadrant east) {
        table[arm.ordinal()][Direction.NORTH.ordinal()] = north;
        table[arm.ordinal()][Direction.SOUTH.ordinal()] = south;
        table[arm.ordinal()][Direction.WEST.ordinal()] = west;
        table[arm.ordinal()][Direction.EAST.ordinal()] = east;
    }

    public static int maskOf(BlockState state) {
        int mask = 0;
        for (Direction dir : Direction.VALUES) {
            if (state.getValue(propertyFor(dir))) mask |= 1 << dir.ordinal();
        }
        return mask;
    }

    private static BooleanProperty propertyFor(Direction dir) {
        return switch (dir) {
            case DOWN -> BlockStateProperties.DOWN;
            case UP -> BlockStateProperties.UP;
            case NORTH -> BlockStateProperties.NORTH;
            case SOUTH -> BlockStateProperties.SOUTH;
            case WEST -> BlockStateProperties.WEST;
            case EAST -> BlockStateProperties.EAST;
        };
    }

    @Override
    public BlockStateModel.UnbakedRoot root(Block block, BlockState state) {
        return new Root(BlockModel.base(block), maskOf(state));
    }

    public record Root(Identifier carrier, int mask) implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material = BlockModel.slot(baker, carrier, slots, "all");
            QuadCollection.Builder builder = new QuadCollection.Builder();
            Vector3f coreFrom = new Vector3f(CORE_MIN, CORE_MIN, CORE_MIN);
            Vector3f coreTo = new Vector3f(CORE_MAX, CORE_MAX, CORE_MAX);

            for (Direction arm : Direction.VALUES) {
                if ((mask & (1 << arm.ordinal())) == 0) {
                    builder.addUnculledFace(
                            Boxes.face(
                                    baker,
                                    coreFrom,
                                    coreTo,
                                    arm,
                                    material,
                                    arm == Direction.DOWN ? CAP_DOWN : CAP,
                                    Quadrant.R0,
                                    CuboidFace.NO_TINT,
                                    Boxes.IDENTITY));
                    continue;
                }
                Vector3f from = new Vector3f(CORE_MIN, CORE_MIN, CORE_MIN);
                Vector3f to = new Vector3f(CORE_MAX, CORE_MAX, CORE_MAX);
                extend(from, to, arm);
                for (Direction side : Direction.VALUES) {
                    @Nullable Quadrant quadrant = ARM_QUADRANT[arm.ordinal()][side.ordinal()];
                    if (quadrant == null) continue;
                    builder.addUnculledFace(
                            Boxes.face(
                                    baker,
                                    from,
                                    to,
                                    side,
                                    material,
                                    ARM,
                                    quadrant,
                                    CuboidFace.NO_TINT,
                                    Boxes.IDENTITY));
                }
            }
            return builder.build();
        }

        private void extend(Vector3f from, Vector3f to, Direction arm) {
            boolean positive = arm.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            float min = positive ? CORE_MAX : 0F, max = positive ? 16F : CORE_MIN;
            switch (arm.getAxis()) {
                case X -> {
                    from.x = min;
                    to.x = max;
                }
                case Y -> {
                    from.y = min;
                    to.y = max;
                }
                case Z -> {
                    from.z = min;
                    to.z = max;
                }
            }
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return mask;
        }
    }
}
