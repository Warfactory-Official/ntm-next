/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
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

import com.hbm.blocks.generic.ChainBlock;
import com.mojang.blaze3d.platform.Transparency;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public record ChainModel() implements SimpleBlockModel {

    @Override
    public BlockStateModel.UnbakedRoot root(Block block, BlockState state) {
        return new Root(
                BlockModel.base(block),
                state.getValue(ChainBlock.FACING),
                state.getValue(ChainBlock.END));
    }

    public record Root(Identifier carrier, Direction facing, boolean end) implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState state, ModelBaker baker, ResolvedModel carrier, TextureSlots slots) {
            Material.Baked material = BlockModel.slot(baker, carrier, slots, end ? "end" : "chain");
            var info =
                    BakedQuad.MaterialInfo.of(
                            material, Transparency.TRANSPARENT, -1, false, 0, true);
            List<ObjUnbakedGeometry.BoxQuad> output =
                    new ArrayList<>(facing == Direction.DOWN ? 4 : 2);
            switch (facing) {
                case DOWN -> {
                    plane(output, info, 0, 0, 1, 1, true);
                    plane(output, info, 0, 1, 1, 0, true);
                }
                case NORTH -> plane(output, info, 1, 0.95F, 0, 0.95F, false);
                case SOUTH -> plane(output, info, 0, 0.05F, 1, 0.05F, false);
                case WEST -> plane(output, info, 0.95F, 0, 0.95F, 1, false);
                case EAST -> plane(output, info, 0.05F, 1, 0.05F, 0, false);
                case UP -> throw new IllegalStateException();
            }
            return ObjUnbakedGeometry.bakeGroups(List.of(), output, 0F, 0F, 0F);
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return this;
        }
    }

    private static void plane(
            List<ObjUnbakedGeometry.BoxQuad> output,
            BakedQuad.MaterialInfo info,
            float ax,
            float az,
            float bx,
            float bz,
            boolean mirrorBack) {
        var sprite = info.sprite();
        long topLeft = UVPair.pack(sprite.getU(0), sprite.getV(0));
        long bottomLeft = UVPair.pack(sprite.getU(0), sprite.getV(1));
        long bottomRight = UVPair.pack(sprite.getU(1), sprite.getV(1));
        long topRight = UVPair.pack(sprite.getU(1), sprite.getV(0));
        var aTop = new Vector3f(ax, 1, az);
        var aBottom = new Vector3f(ax, 0, az);
        var bBottom = new Vector3f(bx, 0, bz);
        var bTop = new Vector3f(bx, 1, bz);
        Direction direction = Direction.getApproximateNearest(az - bz, 0, bx - ax);
        output.add(
                new ObjUnbakedGeometry.BoxQuad(
                        new BakedQuad(
                                aTop,
                                aBottom,
                                bBottom,
                                bTop,
                                topLeft,
                                bottomLeft,
                                bottomRight,
                                topRight,
                                direction,
                                info),
                        ObjUnbakedGeometry.WHITE,
                        null,
                        QuadLighting.OWN_BLOCK));
        output.add(
                new ObjUnbakedGeometry.BoxQuad(
                        mirrorBack
                                ? new BakedQuad(
                                        bTop,
                                        bBottom,
                                        aBottom,
                                        aTop,
                                        topLeft,
                                        bottomLeft,
                                        bottomRight,
                                        topRight,
                                        direction.getOpposite(),
                                        info)
                                : new BakedQuad(
                                        aBottom,
                                        aTop,
                                        bTop,
                                        bBottom,
                                        bottomLeft,
                                        topLeft,
                                        topRight,
                                        bottomRight,
                                        direction.getOpposite(),
                                        info),
                        ObjUnbakedGeometry.WHITE,
                        null,
                        QuadLighting.OWN_BLOCK));
    }
}
