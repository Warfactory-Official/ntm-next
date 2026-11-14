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

import com.hbm.blocks.machine.RTGBlock;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
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

public record RTGModel() implements BlockModel<HFRWavefrontObject> {

    private static final Identifier OBJ = RegistryUtil.id("models/machines/rtg.obj");
    private static final String[] GEN = {"Gen"};
    private static final String[] CONNECTOR = {"Connector"};
    private static final ObjUnbakedGeometry.Overrides NO_CULL =
            new ObjUnbakedGeometry.Overrides(null, null, null, new String[] {"Gen", "Connector"});

    private static final float BODY_YAW = 180F;
    private static final int CONNECTOR_NORTH_YAW = 270;

    private static ObjUnbakedGeometry.Group group(
            HFRWavefrontObject obj, String[] parts, Material.Baked material, float yaw) {
        return ObjUnbakedGeometry.Group.rawSmooth(
                obj,
                parts,
                material,
                new Matrix4f().translate(0.5F, 0F, 0.5F).rotateY((float) Math.toRadians(yaw)));
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(BlockModel.base(block), prepared, RTGBlock.mask(state));
    }

    public record Root(Identifier carrier, HFRWavefrontObject obj, int mask)
            implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.SINGLE_SLOT);
            List<ObjUnbakedGeometry.Group> groups = new ArrayList<>(1 + Integer.bitCount(mask));
            groups.add(group(obj, GEN, material, BODY_YAW));
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if ((mask & (1 << dir.ordinal())) == 0) continue;
                groups.add(group(obj, CONNECTOR, material, Facing.yaw(dir, CONNECTOR_NORTH_YAW)));
            }
            return ObjUnbakedGeometry.bakeGroups(
                    groups, List.of(), 0F, ObjUnbakedGeometry.RAW_OFFSET_Y, 0F, NO_CULL);
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return mask;
        }
    }
}
