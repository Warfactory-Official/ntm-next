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

import com.hbm.blocks.machine.rbmk.RBMKBlock;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.math.Quadrant;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public record RBMKColumnModel(Kind kind) implements BlockModel<@Nullable HFRWavefrontObject> {

    private static final Identifier ELEMENT_OBJ =
            Identifier.parse("hbm:models/rbmk/rbmk_element.obj");

    private static final String[] INNER = {"Inner"};
    private static final String[] CAP = {"Cap"};

    private static final Matrix4fc CENTRED = new Matrix4f().translate(0.5F, 0F, 0.5F);
    private static final int ABOVE = QuadLighting.cell(0, 1, 0);

    private static QuadCollection build(
            TextureSlots slots,
            ModelBaker baker,
            ResolvedModel base,
            RBMKBlock.Lid lid,
            boolean topCell,
            Kind kind,
            @Nullable HFRWavefrontObject obj) {
        List<ObjUnbakedGeometry.Group> groups = new ArrayList<>();
        List<ObjUnbakedGeometry.BoxQuad> boxes = new ArrayList<>();

        Material.Baked top = BlockModel.slot(baker, base, slots, "top");
        Material.Baked side = BlockModel.slot(baker, base, slots, "side");

        switch (kind) {
            case FUEL -> {
                Material.Baked inner = BlockModel.slot(baker, base, slots, "inner");
                if (obj != null) {
                    groups.add(ObjUnbakedGeometry.Group.raw(obj, INNER, inner, CENTRED));
                    groups.add(ObjUnbakedGeometry.Group.raw(obj, CAP, top, CENTRED));
                }
                CuboidFace.UVs[] unit =
                        (new CuboidFace.UVs[] {
                            new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F),
                            new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F),
                            new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F),
                            new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F),
                            new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F),
                            new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F)
                        });
                boxFace(
                        boxes,
                        baker,
                        Direction.NORTH,
                        Boxes.UNIT_FROM,
                        Boxes.UNIT_TO,
                        unit,
                        side,
                        true,
                        QuadLighting.VANILLA);
                boxFace(
                        boxes,
                        baker,
                        Direction.SOUTH,
                        Boxes.UNIT_FROM,
                        Boxes.UNIT_TO,
                        unit,
                        side,
                        true,
                        QuadLighting.VANILLA);
                boxFace(
                        boxes,
                        baker,
                        Direction.WEST,
                        Boxes.UNIT_FROM,
                        Boxes.UNIT_TO,
                        unit,
                        side,
                        true,
                        QuadLighting.VANILLA);
                boxFace(
                        boxes,
                        baker,
                        Direction.EAST,
                        Boxes.UNIT_FROM,
                        Boxes.UNIT_TO,
                        unit,
                        side,
                        true,
                        QuadLighting.VANILLA);
                addLid(boxes, slots, baker, base, lid, topCell);
            }
            case CONTROL -> {
                Material.Baked bottom = BlockModel.slot(baker, base, slots, "bottom");
                addCube(boxes, baker, top, side, bottom);
                if (topCell) addPipes(boxes, slots, baker, base);
            }
            case BOILER -> {
                addCube(boxes, baker, top, side, top);
                if (topCell) {
                    if (lid.present()) addLid(boxes, slots, baker, base, lid, true);
                    else addPipes(boxes, slots, baker, base);
                }
            }
            default -> {
                addCube(boxes, baker, top, side, top);
                addLid(boxes, slots, baker, base, lid, topCell);
            }
        }

        return ObjUnbakedGeometry.bakeGroups(
                groups, boxes, 0F, ObjUnbakedGeometry.RAW_OFFSET_Y, 0F);
    }

    private static void addLid(
            List<ObjUnbakedGeometry.BoxQuad> into,
            TextureSlots slots,
            ModelBaker baker,
            ResolvedModel base,
            RBMKBlock.Lid lid,
            boolean topCell) {
        if (!topCell || !lid.present()) return;
        String prefix = lid == RBMKBlock.Lid.GLASS ? "glass" : "cover";
        Material.Baked lidTop = BlockModel.slot(baker, base, slots, prefix + "_top");
        Material.Baked lidSide = BlockModel.slot(baker, base, slots, prefix + "_side");
        final int origin = ABOVE;
        final Vector3f from = new Vector3f(0F * 16F, 1F * 16F, 0F * 16F);
        final Vector3f to = new Vector3f(1F * 16F, 1.25F * 16F, 1F * 16F);
        faces(
                into,
                baker,
                from,
                to,
                new CuboidFace.UVs[] {
                    new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F),
                    new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F),
                    new CuboidFace.UVs(0.0F, 12.0F, 16.0F, 16.0F),
                    new CuboidFace.UVs(0.0F, 12.0F, 16.0F, 16.0F),
                    new CuboidFace.UVs(0.0F, 12.0F, 16.0F, 16.0F),
                    new CuboidFace.UVs(0.0F, 12.0F, 16.0F, 16.0F)
                },
                lidTop,
                lidSide,
                lidTop,
                false,
                origin);
    }

    private static void addPipes(
            List<ObjUnbakedGeometry.BoxQuad> into,
            TextureSlots slots,
            ModelBaker baker,
            ResolvedModel base) {
        Material.Baked pipeTop = BlockModel.slot(baker, base, slots, "pipe_top");
        Material.Baked pipeSide = BlockModel.slot(baker, base, slots, "pipe_side");
        final int origin = ABOVE;
        final Vector3f from = new Vector3f(0.0625F * 16F, 1F * 16F, 0.0625F * 16F);
        final Vector3f to = new Vector3f(0.4375F * 16F, 1.125F * 16F, 0.4375F * 16F);
        faces(
                into,
                baker,
                from,
                to,
                new CuboidFace.UVs[] {
                    new CuboidFace.UVs(1.0F, 9.0F, 7.0F, 15.0F),
                    new CuboidFace.UVs(1.0F, 1.0F, 7.0F, 7.0F),
                    new CuboidFace.UVs(9.0F, 14.0F, 15.0F, 16.0F),
                    new CuboidFace.UVs(1.0F, 14.0F, 7.0F, 16.0F),
                    new CuboidFace.UVs(1.0F, 14.0F, 7.0F, 16.0F),
                    new CuboidFace.UVs(9.0F, 14.0F, 15.0F, 16.0F)
                },
                pipeTop,
                pipeSide,
                pipeTop,
                false,
                origin);
        final int origin2 = ABOVE;
        final Vector3f from2 = new Vector3f(0.0625F * 16F, 1F * 16F, 0.5625F * 16F);
        final Vector3f to2 = new Vector3f(0.4375F * 16F, 1.125F * 16F, 0.9375F * 16F);
        faces(
                into,
                baker,
                from2,
                to2,
                new CuboidFace.UVs[] {
                    new CuboidFace.UVs(1.0F, 1.0F, 7.0F, 7.0F),
                    new CuboidFace.UVs(1.0F, 9.0F, 7.0F, 15.0F),
                    new CuboidFace.UVs(9.0F, 14.0F, 15.0F, 16.0F),
                    new CuboidFace.UVs(1.0F, 14.0F, 7.0F, 16.0F),
                    new CuboidFace.UVs(9.0F, 14.0F, 15.0F, 16.0F),
                    new CuboidFace.UVs(1.0F, 14.0F, 7.0F, 16.0F)
                },
                pipeTop,
                pipeSide,
                pipeTop,
                false,
                origin2);
        final int origin3 = ABOVE;
        final Vector3f from3 = new Vector3f(0.5625F * 16F, 1F * 16F, 0.5625F * 16F);
        final Vector3f to3 = new Vector3f(0.9375F * 16F, 1.125F * 16F, 0.9375F * 16F);
        faces(
                into,
                baker,
                from3,
                to3,
                new CuboidFace.UVs[] {
                    new CuboidFace.UVs(9.0F, 1.0F, 15.0F, 7.0F),
                    new CuboidFace.UVs(9.0F, 9.0F, 15.0F, 15.0F),
                    new CuboidFace.UVs(1.0F, 14.0F, 7.0F, 16.0F),
                    new CuboidFace.UVs(9.0F, 14.0F, 15.0F, 16.0F),
                    new CuboidFace.UVs(9.0F, 14.0F, 15.0F, 16.0F),
                    new CuboidFace.UVs(1.0F, 14.0F, 7.0F, 16.0F)
                },
                pipeTop,
                pipeSide,
                pipeTop,
                false,
                origin3);
        final int origin4 = ABOVE;
        final Vector3f from4 = new Vector3f(0.5625F * 16F, 1F * 16F, 0.0625F * 16F);
        final Vector3f to4 = new Vector3f(0.9375F * 16F, 1.125F * 16F, 0.4375F * 16F);
        faces(
                into,
                baker,
                from4,
                to4,
                new CuboidFace.UVs[] {
                    new CuboidFace.UVs(9.0F, 9.0F, 15.0F, 15.0F),
                    new CuboidFace.UVs(9.0F, 1.0F, 15.0F, 7.0F),
                    new CuboidFace.UVs(1.0F, 14.0F, 7.0F, 16.0F),
                    new CuboidFace.UVs(9.0F, 14.0F, 15.0F, 16.0F),
                    new CuboidFace.UVs(1.0F, 14.0F, 7.0F, 16.0F),
                    new CuboidFace.UVs(9.0F, 14.0F, 15.0F, 16.0F)
                },
                pipeTop,
                pipeSide,
                pipeTop,
                false,
                origin4);
    }

    private static void addCube(
            List<ObjUnbakedGeometry.BoxQuad> into,
            ModelBaker baker,
            Material.Baked top,
            Material.Baked side,
            Material.Baked bottom) {

        final Vector3f from = new Vector3f(0F * 16F, 0F * 16F, 0F * 16F);
        final Vector3f to = new Vector3f(1F * 16F, 1F * 16F, 1F * 16F);
        faces(
                into,
                baker,
                from,
                to,
                new CuboidFace.UVs[] {
                    new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F),
                    new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F),
                    new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F),
                    new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F),
                    new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F),
                    new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F)
                },
                top,
                side,
                bottom,
                true,
                QuadLighting.VANILLA);
    }

    private static void faces(
            List<ObjUnbakedGeometry.BoxQuad> into,
            ModelBaker baker,
            Vector3fc from,
            Vector3fc to,
            CuboidFace.UVs[] uvs,
            Material.Baked top,
            Material.Baked side,
            Material.Baked bottom,
            boolean culled,
            int origin) {
        boxFace(into, baker, Direction.UP, from, to, uvs, top, culled, origin);
        boxFace(into, baker, Direction.DOWN, from, to, uvs, bottom, culled, origin);
        boxFace(into, baker, Direction.NORTH, from, to, uvs, side, culled, origin);
        boxFace(into, baker, Direction.SOUTH, from, to, uvs, side, culled, origin);
        boxFace(into, baker, Direction.WEST, from, to, uvs, side, culled, origin);
        boxFace(into, baker, Direction.EAST, from, to, uvs, side, culled, origin);
    }

    private static void boxFace(
            List<ObjUnbakedGeometry.BoxQuad> into,
            ModelBaker baker,
            Direction dir,
            Vector3fc from,
            Vector3fc to,
            CuboidFace.UVs[] uvs,
            Material.Baked material,
            boolean culled,
            int origin) {
        BakedQuad quad =
                Boxes.face(
                        baker,
                        from,
                        to,
                        dir,
                        material,
                        uvs[dir.ordinal()],
                        Quadrant.R0,
                        CuboidFace.NO_TINT,
                        Boxes.IDENTITY);
        into.add(
                new ObjUnbakedGeometry.BoxQuad(
                        quad, ObjUnbakedGeometry.WHITE, culled ? dir : null, origin));
    }

    @Override
    public @Nullable HFRWavefrontObject prepare() {
        return kind == Kind.FUEL ? Meshes.load(ELEMENT_OBJ) : null;
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            @Nullable HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(BlockModel.base(block), kind, prepared);
    }

    public enum Kind {
        PLAIN(false),
        FUEL(false),
        CONTROL(true),
        BOILER(true);

        public final boolean piped;

        Kind(boolean piped) {
            this.piped = piped;
        }
    }

    public record Root(Identifier carrier, Kind kind, @Nullable HFRWavefrontObject obj)
            implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState state, ModelBaker baker, ResolvedModel carrier, TextureSlots slots) {
            return build(
                    slots,
                    baker,
                    carrier,
                    RBMKBlock.lidOf(state),
                    state.getValue(RBMKBlock.PART) == RBMKBlock.Part.TOP,
                    kind,
                    obj);
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return RBMKBlock.lidOf(state).ordinal()
                    | (state.getValue(RBMKBlock.PART).ordinal() << 2);
        }
    }
}
