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

import com.hbm.blocks.machine.pile.PileDeviceBlock;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.hbm.util.RegistryUtil;
import com.mojang.math.OctahedralGroup;
import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
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

public record PileDeviceModel(PileDeviceBlock.Kind kind) implements BlockModel<HFRWavefrontObject> {

    public static final String VENT_FAN = "Fan";
    private static final String[] LOADER_BODY = {"Loader"};
    private static final String[] VENT_PIPE = {"Pipe"};
    private static final String[] CONTROL_BASE = {"Base"};

    public static Identifier objId(PileDeviceBlock.Kind kind) {
        return RegistryUtil.id("models/pile/" + kind.modelName() + ".obj");
    }

    public static Identifier baseModel(PileDeviceBlock.Kind kind) {
        return RegistryUtil.id("block/" + kind.modelName() + "_base");
    }

    private static OctahedralGroup facingRotation(Direction facing) {
        return Facing.yawRotation(facing, 90);
    }

    private static List<ObjUnbakedGeometry.Group> groups(
            HFRWavefrontObject obj,
            Material.Baked material,
            PileDeviceBlock.Kind kind,
            Direction facing) {
        OctahedralGroup yaw = facingRotation(facing);
        List<ObjUnbakedGeometry.Group> groups = new ArrayList<>(2);
        switch (kind) {
            case LOADER -> {
                groups.add(
                        ObjUnbakedGeometry.Group.opaque(
                                obj, LOADER_BODY, material, posed(yaw, 0F, 0F, 0F)));
            }
            case VENT ->
                    groups.add(
                            ObjUnbakedGeometry.Group.opaque(
                                    obj, VENT_PIPE, material, posed(yaw, 0F, 0F, 0F)));
            case CONTROL -> {
                groups.add(
                        ObjUnbakedGeometry.Group.opaque(
                                obj, CONTROL_BASE, material, posed(yaw, 0F, 0F, 0F)));
            }
        }
        return groups;
    }

    private static ModelState posed(OctahedralGroup rotation, float tx, float ty, float tz) {
        Matrix4f matrix =
                new Matrix4f().translation(tx, ty, tz).mul(new Matrix4f(rotation.transformation()));
        return Boxes.pose(new Transformation(matrix));
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(objId(kind));
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(baseModel(kind), kind, prepared, state);
    }

    public record Root(
            Identifier carrier, PileDeviceBlock.Kind kind, HFRWavefrontObject obj, BlockState state)
            implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.SINGLE_SLOT);
            Direction facing = state.getValue(PileDeviceBlock.FACING);
            return ObjUnbakedGeometry.bakeGroups(groups(obj, material, kind, facing), 0F, 0F, 0F);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return state.getValue(PileDeviceBlock.FACING);
        }
    }
}
