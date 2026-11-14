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

import com.hbm.blocks.network.RadioTorchBlock;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.hbm.util.RegistryUtil;
import com.mojang.math.OctahedralGroup;
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

public record RadioTorchModel() implements BlockModel<HFRWavefrontObject> {

    static final Identifier OBJ = RegistryUtil.id("models/blocks/rtty.obj");

    public static Identifier baseModel(RadioTorchBlock.Kind kind, BlockState state) {
        String texture =
                switch (kind) {
                    case SENDER ->
                            state.getValue(RadioTorchBlock.LIT)
                                    ? "rtty_sender_on"
                                    : "rtty_sender_off";
                    case RECEIVER ->
                            state.getValue(RadioTorchBlock.LIT) ? "rtty_rec_on" : "rtty_rec_off";
                    case LOGIC ->
                            state.getValue(RadioTorchBlock.LIT)
                                    ? "rtty_logic_on"
                                    : "rtty_logic_off";
                    case COUNTER -> "rtty_counter";
                    case READER -> "rtty_reader";
                    case CONTROLLER -> "rtty_controller";
                };
        return RegistryUtil.id("block/" + texture + "_base");
    }

    static OctahedralGroup rotationFor(Direction facing) {
        return switch (facing) {
            case DOWN -> Facing.rz(-180);
            case UP -> OctahedralGroup.IDENTITY;
            case NORTH -> Facing.ry(90).compose(Facing.rz(-90));
            case SOUTH -> Facing.ry(270).compose(Facing.rz(-90));
            case WEST -> Facing.ry(180).compose(Facing.rz(-90));
            case EAST -> Facing.rz(-90);
        };
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.faceNormals(OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(baseModel(((RadioTorchBlock) block).kind(), state), prepared, state);
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
            return ObjUnbakedGeometry.bakeGroups(
                    obj,
                    null,
                    material,
                    BlockModelRotation.get(rotationFor(state.getValue(RadioTorchBlock.FACING))),
                    0F,
                    0.5F,
                    0F,
                    false,
                    false);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return List.of(carrier, state.getValue(RadioTorchBlock.FACING));
        }
    }
}
