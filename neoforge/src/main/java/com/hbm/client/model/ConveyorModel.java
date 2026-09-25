// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.network.ConveyorBend;
import com.hbm.blocks.network.ConveyorBendableBlock;
import com.hbm.blocks.network.ConveyorBlockBase;
import com.hbm.blocks.network.ConveyorLiftBlock;
import com.hbm.blocks.network.ConveyorLiftPart;
import com.mojang.math.OctahedralGroup;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
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
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public record ConveyorModel() implements SimpleBlockModel {

    private static final float LIP = 4F;
    private static final float INNER_MIN = 4F;
    private static final float INNER_MAX = 12F;
    private static final float STRIP = 6F;
    private static final float HEAD = 8F;

    static final String BELT = "belt";
    static final String SIDE = "side";
    static final String CONCRETE = "concrete";
    private static final String CURVE_LEFT = "curve_left";
    private static final String CURVE_RIGHT = "curve_right";
    private static final String IRON = "iron";

    static ModelState turn(Direction facing) {
        return BlockModelRotation.get(
                switch (facing) {
                    case EAST -> OctahedralGroup.BLOCK_ROT_Y_90;
                    case SOUTH -> OctahedralGroup.BLOCK_ROT_Y_180;
                    case WEST -> OctahedralGroup.BLOCK_ROT_Y_270;
                    default -> OctahedralGroup.IDENTITY;
                });
    }

    private static String beltSlot(Direction local) {
        return local == Direction.WEST || local == Direction.EAST ? SIDE : BELT;
    }

    @Override
    public BlockStateModel.UnbakedRoot root(Block block, BlockState state) {
        Direction facing = state.getValue(ConveyorBlockBase.FACING);

        if (state.hasProperty(ConveyorLiftBlock.PART)) {
            return new Root(
                    BlockModel.base(block), facing, null, state.getValue(ConveyorLiftBlock.PART));
        }
        return new Root(
                BlockModel.base(block), facing, state.getValue(ConveyorBendableBlock.BEND), null);
    }

    static void box(
            List<ObjUnbakedGeometry.BoxQuad> builder,
            ModelBaker baker,
            ResolvedModel carrier,
            TextureSlots slots,
            ModelState turn,
            String slot,
            ConveyorUv.Uv[] windows,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1) {
        box(
                builder,
                baker,
                carrier,
                slots,
                turn,
                local -> slot,
                windows,
                null,
                x0,
                y0,
                z0,
                x1,
                y1,
                z1);
    }

    static void box(
            List<ObjUnbakedGeometry.BoxQuad> builder,
            ModelBaker baker,
            ResolvedModel carrier,
            TextureSlots slots,
            ModelState turn,
            SlotBySide slotBySide,
            ConveyorUv.Uv[] windows,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1) {
        box(
                builder,
                baker,
                carrier,
                slots,
                turn,
                slotBySide,
                windows,
                null,
                x0,
                y0,
                z0,
                x1,
                y1,
                z1);
    }

    static void box(
            List<ObjUnbakedGeometry.BoxQuad> builder,
            ModelBaker baker,
            ResolvedModel carrier,
            TextureSlots slots,
            ModelState turn,
            SlotBySide slotBySide,
            ConveyorUv.Uv[] windows,
            @Nullable Direction culled,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1) {
        Vector3f from = new Vector3f(x0, y0, z0);
        Vector3f to = new Vector3f(x1, y1, z1);

        for (Direction local : Direction.VALUES) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, slotBySide.slot(local));
            ConveyorUv.Uv uv = windows[local.ordinal()];
            BakedQuad quad =
                    Boxes.face(
                            baker,
                            from,
                            to,
                            local,
                            material,
                            uv.uvs(),
                            uv.quadrant(),
                            CuboidFace.NO_TINT,
                            turn);

            builder.add(
                    new ObjUnbakedGeometry.BoxQuad(
                            quad,
                            ObjUnbakedGeometry.WHITE,
                            local == culled ? local : null,
                            QuadLighting.cell(0, 0, 0)));
        }
    }

    private static int[] rot(int down, int up, int north, int south, int west, int east) {
        int[] rot = new int[6];
        rot[Direction.DOWN.ordinal()] = down;
        rot[Direction.UP.ordinal()] = up;
        rot[Direction.NORTH.ordinal()] = north;
        rot[Direction.SOUTH.ordinal()] = south;
        rot[Direction.WEST.ordinal()] = west;
        rot[Direction.EAST.ordinal()] = east;
        return rot;
    }

    @FunctionalInterface
    interface SlotBySide {
        String slot(Direction local);
    }

    public record Root(
            Identifier carrier, Direction facing, ConveyorBend bend, ConveyorLiftPart part)
            implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState state, ModelBaker baker, ResolvedModel carrier, TextureSlots slots) {
            List<ObjUnbakedGeometry.BoxQuad> builder = new ArrayList<>();
            ModelState turn = turn(facing);

            if (part != null) lift(builder, baker, carrier, slots, turn);
            else belt(builder, baker, carrier, slots, turn);

            return ObjUnbakedGeometry.bakeGroups(List.of(), builder, 0F, 0F, 0F);
        }

        private void belt(
                List<ObjUnbakedGeometry.BoxQuad> builder,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots,
                ModelState turn) {
            if (bend == ConveyorBend.STRAIGHT) {

                box(
                        builder,
                        baker,
                        carrier,
                        slots,
                        turn,
                        ConveyorModel::beltSlot,
                        ConveyorUv.W0,
                        Direction.DOWN,
                        0F,
                        0F,
                        0F,
                        16F,
                        LIP,
                        16F);
                return;
            }

            String curve = bend == ConveyorBend.LEFT ? CURVE_LEFT : CURVE_RIGHT;
            box(
                    builder,
                    baker,
                    carrier,
                    slots,
                    turn,
                    local -> local.getAxis().isVertical() ? curve : SIDE,
                    ConveyorUv.W1,
                    Direction.DOWN,
                    0F,
                    0F,
                    0F,
                    16F,
                    LIP,
                    16F);
        }

        private void lift(
                List<ObjUnbakedGeometry.BoxQuad> builder,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots,
                ModelState turn) {
            if (part == ConveyorLiftPart.TOP) {
                box(
                        builder,
                        baker,
                        carrier,
                        slots,
                        turn,
                        CONCRETE,
                        ConveyorUv.W2,
                        0F,
                        0F,
                        0F,
                        INNER_MIN,
                        HEAD,
                        16F);
                box(
                        builder,
                        baker,
                        carrier,
                        slots,
                        turn,
                        CONCRETE,
                        ConveyorUv.W3,
                        INNER_MAX,
                        0F,
                        0F,
                        16F,
                        HEAD,
                        16F);
                box(
                        builder,
                        baker,
                        carrier,
                        slots,
                        turn,
                        ConveyorModel::beltSlot,
                        ConveyorUv.W4,
                        INNER_MIN,
                        0F,
                        0F,
                        INNER_MAX,
                        LIP,
                        STRIP);
                return;
            }

            if (part == ConveyorLiftPart.BOTTOM) {

                box(
                        builder,
                        baker,
                        carrier,
                        slots,
                        turn,
                        BELT,
                        ConveyorUv.W5,
                        0F,
                        0F,
                        INNER_MIN,
                        INNER_MIN,
                        LIP,
                        INNER_MAX);
                box(
                        builder,
                        baker,
                        carrier,
                        slots,
                        turn,
                        BELT,
                        ConveyorUv.W6,
                        INNER_MAX,
                        0F,
                        INNER_MIN,
                        16F,
                        LIP,
                        INNER_MAX);
                box(
                        builder,
                        baker,
                        carrier,
                        slots,
                        turn,
                        BELT,
                        ConveyorUv.W7,
                        INNER_MIN,
                        0F,
                        INNER_MAX,
                        INNER_MAX,
                        LIP,
                        16F);
            }

            box(
                    builder,
                    baker,
                    carrier,
                    slots,
                    turn,
                    CONCRETE,
                    ConveyorUv.W8,
                    0F,
                    0F,
                    0F,
                    16F,
                    16F,
                    INNER_MIN);
            box(
                    builder,
                    baker,
                    carrier,
                    slots,
                    turn,
                    CONCRETE,
                    ConveyorUv.W9,
                    0F,
                    0F,
                    INNER_MAX,
                    INNER_MIN,
                    16F,
                    16F);
            box(
                    builder,
                    baker,
                    carrier,
                    slots,
                    turn,
                    CONCRETE,
                    ConveyorUv.W10,
                    INNER_MAX,
                    0F,
                    INNER_MAX,
                    16F,
                    16F,
                    16F);
            box(
                    builder,
                    baker,
                    carrier,
                    slots,
                    turn,
                    BELT,
                    ConveyorUv.W11,
                    INNER_MIN,
                    0F,
                    INNER_MIN,
                    INNER_MAX,
                    16F,
                    STRIP);

            if (part == ConveyorLiftPart.BOTTOM) {
                box(
                        builder,
                        baker,
                        carrier,
                        slots,
                        turn,
                        IRON,
                        ConveyorUv.W12,
                        INNER_MIN,
                        0F,
                        STRIP,
                        INNER_MAX,
                        LIP,
                        INNER_MAX);
            }
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return this;
        }
    }
}
