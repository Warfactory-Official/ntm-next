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

import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.RegistryUtil;
import java.util.Arrays;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public record FluidPipeModel() implements BlockModel<HFRWavefrontObject> {

    public static final Identifier BASE = RegistryUtil.id("block/fluid_pipe_base");
    private static final Identifier OBJ = RegistryUtil.id("models/blocks/pipe_neo.obj");
    private static final float OFFSET_Y = 0.5F;

    private static final int NEG_Z = 1, POS_Z = 2, NEG_Y = 4, POS_Y = 8, NEG_X = 16, POS_X = 32;
    private static final String[][] PARTS_BY_MASK =
            BlockModel.partsByMask(FluidPipeModel::selectParts);

    private static String[] selectParts(int mask) {
        if (mask == 0) return new String[] {"pX", "nX", "pY", "nY", "pZ", "nZ"};
        if (mask == POS_X || mask == NEG_X) return new String[] {"pX", "nX"};
        if (mask == POS_Y || mask == NEG_Y) return new String[] {"pY", "nY"};
        if (mask == POS_Z || mask == NEG_Z) return new String[] {"pZ", "nZ"};
        boolean pX = (mask & POS_X) != 0, nX = (mask & NEG_X) != 0;
        boolean pY = (mask & POS_Y) != 0, nY = (mask & NEG_Y) != 0;
        boolean pZ = (mask & POS_Z) != 0, nZ = (mask & NEG_Z) != 0;

        String[] parts = new String[14];
        int i = 0;
        if (pX) parts[i++] = "pX";
        if (nX) parts[i++] = "nX";
        if (pY) parts[i++] = "pY";
        if (nY) parts[i++] = "nY";
        if (pZ) parts[i++] = "nZ";
        if (nZ) parts[i++] = "pZ";

        if (!pX && !pY && !pZ) parts[i++] = "ppn";
        if (!pX && !pY && !nZ) parts[i++] = "ppp";
        if (!nX && !pY && !pZ) parts[i++] = "npn";
        if (!nX && !pY && !nZ) parts[i++] = "npp";
        if (!pX && !nY && !pZ) parts[i++] = "pnn";
        if (!pX && !nY && !nZ) parts[i++] = "pnp";
        if (!nX && !nY && !pZ) parts[i++] = "nnn";
        if (!nX && !nY && !nZ) parts[i++] = "nnp";
        return Arrays.copyOf(parts, i);
    }

    public static String[] partsFor(BlockState state) {
        int mask = 0;
        if (state.getValue(BlockStateProperties.NORTH)) mask |= NEG_Z;
        if (state.getValue(BlockStateProperties.SOUTH)) mask |= POS_Z;
        if (state.getValue(BlockStateProperties.DOWN)) mask |= NEG_Y;
        if (state.getValue(BlockStateProperties.UP)) mask |= POS_Y;
        if (state.getValue(BlockStateProperties.WEST)) mask |= NEG_X;
        if (state.getValue(BlockStateProperties.EAST)) mask |= POS_X;
        return PARTS_BY_MASK[mask];
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(BlockModel.base(block), prepared, partsFor(state));
    }

    public record Root(Identifier carrier, HFRWavefrontObject obj, String[] parts)
            implements CarrierRoot {

        public Root(HFRWavefrontObject obj, String[] parts) {
            this(BASE, obj, parts);
        }

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.SINGLE_SLOT);
            Material.Baked overlay =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.OVERLAY_SLOT);
            return ObjUnbakedGeometry.bakeGroupsWithOverlay(
                    obj,
                    parts,
                    material,
                    overlay,
                    BlockModelRotation.IDENTITY,
                    0F,
                    OFFSET_Y,
                    0F,
                    1);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return parts;
        }
    }
}
