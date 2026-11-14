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

import com.hbm.blockentity.door.DoorType;
import com.hbm.blocks.generic.GenericDoorBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.client.render.DoorMotion;
import com.hbm.client.render.DoorRenderDefinition;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.mojang.math.OctahedralGroup;
import java.util.List;
import java.util.Objects;
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

public record DoorModel() implements SimpleBlockModel {
    public static final String SHEET_SLOT = "sheet";

    @Override
    public BlockStateModel.UnbakedRoot root(Block block, BlockState state) {
        return Root.of(state);
    }

    public record Root(Identifier carrier, HFRWavefrontObject obj, DoorType kind, BlockState state)
            implements CarrierRoot {

        public static Root of(BlockState state) {
            GenericDoorBlock block = (GenericDoorBlock) state.getBlock();
            return new Root(
                    BlockModel.base(block),
                    block.kind.getRenderDefinition().getModel(),
                    block.kind,
                    state.setValue(GenericDoorBlock.OPEN, Boolean.FALSE));
        }

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            DoorRenderDefinition look = kind.getRenderDefinition();
            if (look.getStaticParts().length == 0) return new QuadCollection.Builder().build();
            sheetsMatch(look, carrier, slots);
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, SHEET_SLOT + textureSlot());
            OctahedralGroup facing =
                    Facing.yawRotation(state.getValue(MultiblockCoreBlock.FACING), 90);
            OctahedralGroup rotation = facing.compose(Facing.ry(look.getYaw()));
            var offset = look.getOffset();
            DoorMotion motion = look.getMotion();
            ObjUnbakedGeometry.Overrides overrides =
                    motion != null && !motion.backfaceCulled()
                            ? new ObjUnbakedGeometry.Overrides(
                                    null, null, null, look.getStaticParts())
                            : ObjUnbakedGeometry.Overrides.NONE;
            return ObjUnbakedGeometry.bakeGroups(
                    List.of(
                            ObjUnbakedGeometry.Group.opaque(
                                    obj,
                                    look.getStaticParts(),
                                    material,
                                    BlockModelRotation.get(rotation))),
                    List.of(),
                    offset.x(),
                    offset.y(),
                    offset.z(),
                    overrides);
        }

        private static void sheetsMatch(
                DoorRenderDefinition look, ResolvedModel carrier, TextureSlots slots) {
            var sheets = look.getTextures();
            for (int k = 0; k <= sheets.size(); k++) {
                var material = slots.getMaterial(SHEET_SLOT + k);
                Identifier named = material == null ? null : material.sprite();
                Identifier wanted = k == sheets.size() ? null : sprite(sheets.get(k));
                if (!Objects.equals(named, wanted)) {
                    throw new IllegalStateException(
                            carrier.debugName()
                                    + " names "
                                    + named
                                    + " in slot "
                                    + SHEET_SLOT
                                    + k
                                    + ", where the door's sheet list has "
                                    + wanted);
                }
            }
        }

        private static Identifier sprite(Identifier texture) {
            String path = texture.getPath();
            return Identifier.fromNamespaceAndPath(
                    texture.getNamespace(),
                    path.substring("textures/".length(), path.length() - ".png".length()));
        }

        private int textureSlot() {
            return kind.getRenderDefinition().getTextureIndex(GenericDoorBlock.skinIndex(state));
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return (state.getValue(MultiblockCoreBlock.FACING).get3DDataValue() << 3)
                    | (kind.getRenderDefinition().getStaticParts().length == 0 ? 0 : textureSlot());
        }
    }
}
