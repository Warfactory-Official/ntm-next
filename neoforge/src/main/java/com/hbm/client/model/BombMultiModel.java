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

import com.hbm.blocks.bomb.BombMultiBlock;
import com.hbm.render.loader.HFRWavefrontObject;
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
import org.joml.Matrix4fc;

public record BombMultiModel(Identifier mesh) implements BlockModel<HFRWavefrontObject> {

    private static final ObjUnbakedGeometry.Overrides NO_CULL =
            new ObjUnbakedGeometry.Overrides(null, null, null, new String[] {"Cylinder"});

    public static Matrix4fc matrixFor(Direction facing) {
        return new Matrix4f()
                .translate(0.5F, 0.5F, 0.5F)
                .rotateX((float) Math.PI)
                .rotateY(BombMultiBlock.yawFor(facing));
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(mesh);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(
                BlockModel.base(block), prepared, matrixFor(state.getValue(BombMultiBlock.FACING)));
    }

    public record Root(Identifier carrier, HFRWavefrontObject obj, Matrix4fc matrix)
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
                    List.of(ObjUnbakedGeometry.Group.rawCutout(obj, null, material, matrix)),
                    List.of(),
                    0F,
                    ObjUnbakedGeometry.RAW_OFFSET_Y,
                    0F,
                    NO_CULL);
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return matrix;
        }
    }
}
