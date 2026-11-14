/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client.model;

import com.hbm.blocks.network.CraneBlock;
import com.hbm.util.Facing;
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

public record CraneModel() implements SimpleBlockModel {

    public static final String TOP = "top";
    public static final String SIDE = "side";
    public static final String IN = "in";
    public static final String SIDE_IN = "side_in";
    public static final String OUT = "out";
    public static final String SIDE_OUT = "side_out";

    public static final String DIRECTIONAL = "directional";
    public static final String DIRECTIONAL_UP = "directional_up";
    public static final String DIRECTIONAL_DOWN = "directional_down";
    public static final String TURN_LEFT = "turn_left";
    public static final String TURN_RIGHT = "turn_right";
    public static final String SIDE_LEFT_TURN_UP = "side_left_turn_up";
    public static final String SIDE_RIGHT_TURN_UP = "side_right_turn_up";
    public static final String SIDE_LEFT_TURN_DOWN = "side_left_turn_down";
    public static final String SIDE_RIGHT_TURN_DOWN = "side_right_turn_down";
    public static final String SIDE_UP_TURN_LEFT = "side_up_turn_left";
    public static final String SIDE_UP_TURN_RIGHT = "side_up_turn_right";
    public static final String SIDE_DOWN_TURN_LEFT = "side_down_turn_left";
    public static final String SIDE_DOWN_TURN_RIGHT = "side_down_turn_right";

    public static String slotFor(Direction face, Direction input, Direction output) {
        boolean overridden = output != input.getOpposite();
        Direction leftHand = Facing.rotate(output, input);

        if (face.getAxis() == Direction.Axis.Y) {
            if (face == output) return OUT;
            if (face == input) return IN;

            if (face == Direction.UP) {
                if (!overridden) return DIRECTIONAL;
                if (leftHand == Direction.UP) return TURN_LEFT;
                if (leftHand == Direction.DOWN) return TURN_RIGHT;
            }

            return TOP;
        }

        if (face == output) return SIDE_OUT;
        if (face == input) return SIDE_IN;

        if (!overridden) {
            if (output == Direction.UP) return DIRECTIONAL_UP;
            if (output == Direction.DOWN) return DIRECTIONAL_DOWN;
            return SIDE;
        }

        if (leftHand == face) {
            if (output == Direction.UP) return SIDE_LEFT_TURN_UP;
            if (output == Direction.DOWN) return SIDE_RIGHT_TURN_DOWN;
            if (input == Direction.UP) return SIDE_UP_TURN_RIGHT;
            if (input == Direction.DOWN) return SIDE_DOWN_TURN_LEFT;
        }

        if (leftHand.getOpposite() == face) {
            if (output == Direction.UP) return SIDE_RIGHT_TURN_UP;
            if (output == Direction.DOWN) return SIDE_LEFT_TURN_DOWN;
            if (input == Direction.UP) return SIDE_UP_TURN_LEFT;
            if (input == Direction.DOWN) return SIDE_DOWN_TURN_RIGHT;
        }

        return SIDE;
    }

    @Override
    public BlockStateModel.UnbakedRoot root(Block block, BlockState state) {
        int topRot = block instanceof CraneBlock crane ? crane.topRotation(state) : 0;
        return new Root(
                BlockModel.base(block),
                CraneBlock.inputSide(state),
                CraneBlock.outputSide(state),
                topRot);
    }

    public record Root(Identifier carrier, Direction input, Direction output, int topRot)
            implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState state, ModelBaker baker, ResolvedModel carrier, TextureSlots slots) {
            QuadCollection.Builder builder = new QuadCollection.Builder();

            UvRotation[] uvs =
                    (new UvRotation[] {
                        new UvRotation(new CuboidFace.UVs(0.0F, 16.0F, 16.0F, 0.0F), Quadrant.R0),
                        switch (topRot) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 16.0F, 0.0F, 0.0F),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 16.0F, 0.0F, 0.0F),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + topRot + " outside 0..3");
                        },
                        new UvRotation(new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F), Quadrant.R0)
                    });
            for (Direction face : Direction.VALUES) {
                Material.Baked material =
                        BlockModel.slot(baker, carrier, slots, slotFor(face, input, output));
                UvRotation uv = uvs[face.ordinal()];
                builder.addCulledFace(
                        face,
                        Boxes.face(
                                baker,
                                Boxes.UNIT_FROM,
                                Boxes.UNIT_TO,
                                face,
                                material,
                                uv.uvs(),
                                uv.quadrant(),
                                CuboidFace.NO_TINT,
                                Boxes.IDENTITY));
            }

            return builder.build();
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return this;
        }
    }
}
