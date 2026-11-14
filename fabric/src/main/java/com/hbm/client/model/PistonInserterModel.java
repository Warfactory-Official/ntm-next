/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.model;

import com.hbm.blocks.machine.PistonInserterBlock;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.RegistryUtil;
import com.mojang.math.OctahedralGroup;
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

public record PistonInserterModel() implements BlockModel<HFRWavefrontObject> {

    public static final Identifier OBJ = RegistryUtil.id("models/machines/piston_inserter.obj");
    public static final Identifier BASE_MODEL = RegistryUtil.id("block/piston_inserter_base");

    public static final String PISTON = "Piston";
    private static final String[] FRAME = {"Frame"};

    private static OctahedralGroup rotationFor(Direction facing) {
        return switch (facing) {
            case DOWN -> OctahedralGroup.ROT_180_FACE_YZ;
            case UP -> OctahedralGroup.IDENTITY;
            case NORTH -> OctahedralGroup.ROT_180_EDGE_YZ_NEG;
            case SOUTH -> OctahedralGroup.ROT_90_X_POS;
            case WEST -> OctahedralGroup.ROT_120_PNP;
            case EAST -> OctahedralGroup.ROT_120_PPN;
        };
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(prepared, state);
    }

    public record Root(HFRWavefrontObject obj, BlockState state) implements CarrierRoot {

        @Override
        public Identifier carrier() {
            return BASE_MODEL;
        }

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.SINGLE_SLOT);
            Direction facing = state.getValue(PistonInserterBlock.FACING);
            return ObjUnbakedGeometry.bakeGroups(
                    obj,
                    FRAME,
                    material,
                    BlockModelRotation.get(rotationFor(facing)),
                    0F,
                    0F,
                    0F,
                    false);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return state.getValue(PistonInserterBlock.FACING);
        }
    }
}
