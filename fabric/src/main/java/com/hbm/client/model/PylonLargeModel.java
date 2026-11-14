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

import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.Transparency;
import com.mojang.math.Transformation;
import java.util.List;
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
import org.joml.Matrix4f;

public record PylonLargeModel() implements BlockModel<HFRWavefrontObject> {

    public static final Identifier MESH = RegistryUtil.id("models/network/pylon_large.obj");

    private static final String[] PARTS = {"Cube_Cube.001"};
    private static final ObjUnbakedGeometry.Overrides NO_CULL =
            new ObjUnbakedGeometry.Overrides(null, PARTS, null, PARTS);

    private static float yawFor(Direction facing) {
        return switch (facing) {
            case NORTH -> 90F;
            case WEST -> 135F;
            case EAST -> 45F;
            default -> 0F;
        };
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(MESH);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(
                BlockModel.base(block), prepared, state.getValue(MultiblockCoreBlock.FACING));
    }

    public record Root(Identifier carrier, HFRWavefrontObject obj, Direction facing)
            implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.SINGLE_SLOT);
            ObjUnbakedGeometry.Group group =
                    new ObjUnbakedGeometry.Group(
                            obj,
                            PARTS,
                            material,
                            Boxes.pose(
                                    new Transformation(
                                            new Matrix4f()
                                                    .rotationY(
                                                            (float)
                                                                    Math.toRadians(
                                                                            yawFor(facing))))),
                            Transparency.NONE,
                            false);
            return ObjUnbakedGeometry.bakeGroups(List.of(group), List.of(), 0F, 0F, 0F, NO_CULL);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return facing;
        }
    }
}
