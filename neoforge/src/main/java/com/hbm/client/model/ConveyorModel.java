// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.network.ConveyorBend;
import com.hbm.blocks.network.ConveyorBendableBlock;
import com.hbm.blocks.network.ConveyorBlockBase;
import com.hbm.blocks.network.ConveyorLiftBlock;
import com.hbm.blocks.network.ConveyorLiftPart;
import com.mojang.math.OctahedralGroup;
import com.mojang.math.Quadrant;
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

    private static String beltSlot(Direction facing, Direction face) {
        return face.getAxis().isHorizontal() && face.getAxis() != facing.getAxis() ? SIDE : BELT;
    }

    static int[] beltRot(Direction facing) {
        return switch (facing) {
            case SOUTH -> (new int[] {0, 3, 0, 3, 0, 0});
            case EAST -> (new int[] {1, 1, 0, 0, 0, 3});
            case WEST -> (new int[] {2, 2, 0, 0, 3, 0});
            case NORTH -> (new int[] {3, 0, 3, 0, 0, 0});
            default -> throw new IllegalArgumentException("Vertical conveyor facing " + facing);
        };
    }

    private static int[] liftRot(Direction facing, boolean head) {
        int[] rot = beltRot(facing);
        rot[Direction.DOWN.ordinal()] = 0;
        if (!head) rot[facing.ordinal()] = 0;
        return rot;
    }

    private static int[] lipRot(ModelState turn, Direction local) {
        Vector3f side =
                turn.transformation()
                        .getMatrix()
                        .transformDirection(
                                new Vector3f(local.getStepX(), local.getStepY(), local.getStepZ()));
        return switch (Direction.getApproximateNearest(side.x, side.y, side.z)) {
            case WEST -> (new int[] {1, 1, 0, 0, 0, 0});
            case EAST -> (new int[] {2, 2, 0, 0, 0, 0});
            case NORTH -> (new int[] {0, 3, 0, 0, 0, 0});
            case SOUTH -> (new int[] {3, 0, 0, 0, 0, 0});
            default -> throw new IllegalStateException("Vertical conveyor lift lip");
        };
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

    public static void faces(
            List<ObjUnbakedGeometry.BoxQuad> builder,
            ModelBaker baker,
            ResolvedModel carrier,
            TextureSlots slots,
            ModelState turn,
            SlotBySide slotBySide,
            @Nullable Direction culled,
            Vector3f from,
            Vector3f to,
            UvRotation[] uvs) {
        for (Direction local : Direction.VALUES) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, slotBySide.slot(local));
            UvRotation uv = uvs[local.ordinal()];
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

    @FunctionalInterface
    public interface SlotBySide {
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
                final Vector3f from = new Vector3f(0F, 0F, 0F);
                final Vector3f to = new Vector3f(16F, LIP, 16F);
                faces(
                        builder,
                        baker,
                        carrier,
                        slots,
                        turn,
                        ConveyorModel::beltSlot,
                        Direction.DOWN,
                        from,
                        to,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(16.0F, 0.0F, 0.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(16.0F, 4.0F, 0.0F, 0.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(0.0F, 12.0F, 16.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(0.0F, 12.0F, 16.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(0.0F, 12.0F, 16.0F, 16.0F), Quadrant.R0)
                        });
                return;
            }

            String curve = bend == ConveyorBend.LEFT ? CURVE_LEFT : CURVE_RIGHT;
            final Vector3f from2 = new Vector3f(0F, 0F, 0F);
            final Vector3f to2 = new Vector3f(16F, LIP, 16F);
            faces(
                    builder,
                    baker,
                    carrier,
                    slots,
                    turn,
                    local -> local.getAxis().isVertical() ? curve : SIDE,
                    Direction.DOWN,
                    from2,
                    to2,
                    new UvRotation[] {
                        new UvRotation(new CuboidFace.UVs(16.0F, 0.0F, 0.0F, 16.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(0.0F, 12.0F, 16.0F, 16.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(0.0F, 12.0F, 16.0F, 16.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(0.0F, 12.0F, 16.0F, 16.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(0.0F, 12.0F, 16.0F, 16.0F), Quadrant.R0)
                    });
        }

        private void lift(
                List<ObjUnbakedGeometry.BoxQuad> builder,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots,
                ModelState turn) {
            if (part == ConveyorLiftPart.TOP) {
                Vector3f a = new Vector3f(0F / 16F - 0.5F, 0F / 16F - 0.5F, 0F / 16F - 0.5F);
                Vector3f b =
                        new Vector3f(INNER_MIN / 16F - 0.5F, HEAD / 16F - 0.5F, 16F / 16F - 0.5F);
                turn.transformation()
                        .getMatrix()
                        .transformPosition(a)
                        .add(0.5F, 0.5F, 0.5F)
                        .mul(16F);
                turn.transformation()
                        .getMatrix()
                        .transformPosition(b)
                        .add(0.5F, 0.5F, 0.5F)
                        .mul(16F);
                final Vector3f from =
                        new Vector3f(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z));
                final Vector3f to =
                        new Vector3f(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
                faces(
                        builder,
                        baker,
                        carrier,
                        slots,
                        Boxes.IDENTITY,
                        local -> CONCRETE,
                        null,
                        from,
                        to,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(from.x(), to.z(), to.x(), from.z()),
                                    Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(from.x(), from.z(), to.x(), to.z()),
                                    Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(
                                            from.x(), 16F - to.y(), to.x(), 16F - from.y()),
                                    Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(
                                            from.x(), 16F - to.y(), to.x(), 16F - from.y()),
                                    Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(
                                            from.z(), 16F - to.y(), to.z(), 16F - from.y()),
                                    Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(
                                            from.z(), 16F - to.y(), to.z(), 16F - from.y()),
                                    Quadrant.R0)
                        });
                Vector3f a2 =
                        new Vector3f(INNER_MAX / 16F - 0.5F, 0F / 16F - 0.5F, 0F / 16F - 0.5F);
                Vector3f b2 = new Vector3f(16F / 16F - 0.5F, HEAD / 16F - 0.5F, 16F / 16F - 0.5F);
                turn.transformation()
                        .getMatrix()
                        .transformPosition(a2)
                        .add(0.5F, 0.5F, 0.5F)
                        .mul(16F);
                turn.transformation()
                        .getMatrix()
                        .transformPosition(b2)
                        .add(0.5F, 0.5F, 0.5F)
                        .mul(16F);
                final Vector3f from2 =
                        new Vector3f(
                                Math.min(a2.x, b2.x), Math.min(a2.y, b2.y), Math.min(a2.z, b2.z));
                final Vector3f to2 =
                        new Vector3f(
                                Math.max(a2.x, b2.x), Math.max(a2.y, b2.y), Math.max(a2.z, b2.z));
                faces(
                        builder,
                        baker,
                        carrier,
                        slots,
                        Boxes.IDENTITY,
                        local2 -> CONCRETE,
                        null,
                        from2,
                        to2,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(from2.x(), to2.z(), to2.x(), from2.z()),
                                    Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(from2.x(), from2.z(), to2.x(), to2.z()),
                                    Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(
                                            from2.x(), 16F - to2.y(), to2.x(), 16F - from2.y()),
                                    Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(
                                            from2.x(), 16F - to2.y(), to2.x(), 16F - from2.y()),
                                    Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(
                                            from2.z(), 16F - to2.y(), to2.z(), 16F - from2.y()),
                                    Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(
                                            from2.z(), 16F - to2.y(), to2.z(), 16F - from2.y()),
                                    Quadrant.R0)
                        });
                final int[] rot3 = liftRot(facing, true);
                Vector3f a3 =
                        new Vector3f(INNER_MIN / 16F - 0.5F, 0F / 16F - 0.5F, 0F / 16F - 0.5F);
                Vector3f b3 =
                        new Vector3f(INNER_MAX / 16F - 0.5F, LIP / 16F - 0.5F, STRIP / 16F - 0.5F);
                turn.transformation()
                        .getMatrix()
                        .transformPosition(a3)
                        .add(0.5F, 0.5F, 0.5F)
                        .mul(16F);
                turn.transformation()
                        .getMatrix()
                        .transformPosition(b3)
                        .add(0.5F, 0.5F, 0.5F)
                        .mul(16F);
                final Vector3f from3 =
                        new Vector3f(
                                Math.min(a3.x, b3.x), Math.min(a3.y, b3.y), Math.min(a3.z, b3.z));
                final Vector3f to3 =
                        new Vector3f(
                                Math.max(a3.x, b3.x), Math.max(a3.y, b3.y), Math.max(a3.z, b3.z));
                faces(
                        builder,
                        baker,
                        carrier,
                        slots,
                        Boxes.IDENTITY,
                        face -> beltSlot(facing, face),
                        null,
                        from3,
                        to3,
                        new UvRotation[] {
                            switch (rot3[0]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from3.x(), to3.z(), to3.x(), from3.z()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - to3.z(),
                                                        to3.x(),
                                                        16F - from3.z(),
                                                        from3.x()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        to3.z(),
                                                        16F - to3.x(),
                                                        from3.z(),
                                                        16F - from3.x()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from3.x(),
                                                        16F - to3.z(),
                                                        16F - to3.x(),
                                                        16F - from3.z()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot3[0] + " outside 0..3");
                            },
                            switch (rot3[1]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from3.x(), from3.z(), to3.x(), to3.z()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from3.z(),
                                                        16F - to3.x(),
                                                        to3.z(),
                                                        16F - from3.x()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from3.z(),
                                                        to3.x(),
                                                        16F - to3.z(),
                                                        from3.x()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from3.x(),
                                                        16F - from3.z(),
                                                        16F - to3.x(),
                                                        16F - to3.z()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot3[1] + " outside 0..3");
                            },
                            switch (rot3[2]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from3.x(),
                                                        16F - to3.y(),
                                                        to3.x(),
                                                        16F - from3.y()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from3.y(),
                                                        from3.x(),
                                                        16F - to3.y(),
                                                        to3.x()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from3.y(),
                                                        16F - from3.x(),
                                                        to3.y(),
                                                        16F - to3.x()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from3.x(),
                                                        to3.y(),
                                                        16F - to3.x(),
                                                        from3.y()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot3[2] + " outside 0..3");
                            },
                            switch (rot3[3]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from3.x(),
                                                        16F - to3.y(),
                                                        to3.x(),
                                                        16F - from3.y()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from3.y(),
                                                        16F - to3.x(),
                                                        to3.y(),
                                                        16F - from3.x()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from3.y(),
                                                        to3.x(),
                                                        16F - to3.y(),
                                                        from3.x()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from3.x(),
                                                        to3.y(),
                                                        16F - to3.x(),
                                                        from3.y()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot3[3] + " outside 0..3");
                            },
                            switch (rot3[4]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from3.z(),
                                                        16F - to3.y(),
                                                        to3.z(),
                                                        16F - from3.y()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from3.y(),
                                                        16F - to3.z(),
                                                        to3.y(),
                                                        16F - from3.z()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from3.y(),
                                                        to3.z(),
                                                        16F - to3.y(),
                                                        from3.z()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from3.z(),
                                                        to3.y(),
                                                        16F - to3.z(),
                                                        from3.y()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot3[4] + " outside 0..3");
                            },
                            switch (rot3[5]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from3.z(),
                                                        16F - to3.y(),
                                                        to3.z(),
                                                        16F - from3.y()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from3.y(),
                                                        from3.z(),
                                                        16F - to3.y(),
                                                        to3.z()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from3.y(),
                                                        16F - from3.z(),
                                                        to3.y(),
                                                        16F - to3.z()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from3.z(),
                                                        to3.y(),
                                                        16F - to3.z(),
                                                        from3.y()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot3[5] + " outside 0..3");
                            }
                        });
                return;
            }

            if (part == ConveyorLiftPart.BOTTOM) {
                final int[] rot4 = lipRot(turn, Direction.WEST);
                Vector3f a4 =
                        new Vector3f(0F / 16F - 0.5F, 0F / 16F - 0.5F, INNER_MIN / 16F - 0.5F);
                Vector3f b4 =
                        new Vector3f(
                                INNER_MIN / 16F - 0.5F, LIP / 16F - 0.5F, INNER_MAX / 16F - 0.5F);
                turn.transformation()
                        .getMatrix()
                        .transformPosition(a4)
                        .add(0.5F, 0.5F, 0.5F)
                        .mul(16F);
                turn.transformation()
                        .getMatrix()
                        .transformPosition(b4)
                        .add(0.5F, 0.5F, 0.5F)
                        .mul(16F);
                final Vector3f from4 =
                        new Vector3f(
                                Math.min(a4.x, b4.x), Math.min(a4.y, b4.y), Math.min(a4.z, b4.z));
                final Vector3f to4 =
                        new Vector3f(
                                Math.max(a4.x, b4.x), Math.max(a4.y, b4.y), Math.max(a4.z, b4.z));
                faces(
                        builder,
                        baker,
                        carrier,
                        slots,
                        Boxes.IDENTITY,
                        local3 -> BELT,
                        null,
                        from4,
                        to4,
                        new UvRotation[] {
                            switch (rot4[0]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from4.x(), to4.z(), to4.x(), from4.z()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - to4.z(),
                                                        to4.x(),
                                                        16F - from4.z(),
                                                        from4.x()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        to4.z(),
                                                        16F - to4.x(),
                                                        from4.z(),
                                                        16F - from4.x()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from4.x(),
                                                        16F - to4.z(),
                                                        16F - to4.x(),
                                                        16F - from4.z()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot4[0] + " outside 0..3");
                            },
                            switch (rot4[1]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from4.x(), from4.z(), to4.x(), to4.z()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from4.z(),
                                                        16F - to4.x(),
                                                        to4.z(),
                                                        16F - from4.x()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from4.z(),
                                                        to4.x(),
                                                        16F - to4.z(),
                                                        from4.x()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from4.x(),
                                                        16F - from4.z(),
                                                        16F - to4.x(),
                                                        16F - to4.z()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot4[1] + " outside 0..3");
                            },
                            switch (rot4[2]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from4.x(),
                                                        16F - to4.y(),
                                                        to4.x(),
                                                        16F - from4.y()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from4.y(),
                                                        from4.x(),
                                                        16F - to4.y(),
                                                        to4.x()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from4.y(),
                                                        16F - from4.x(),
                                                        to4.y(),
                                                        16F - to4.x()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from4.x(),
                                                        to4.y(),
                                                        16F - to4.x(),
                                                        from4.y()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot4[2] + " outside 0..3");
                            },
                            switch (rot4[3]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from4.x(),
                                                        16F - to4.y(),
                                                        to4.x(),
                                                        16F - from4.y()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from4.y(),
                                                        16F - to4.x(),
                                                        to4.y(),
                                                        16F - from4.x()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from4.y(),
                                                        to4.x(),
                                                        16F - to4.y(),
                                                        from4.x()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from4.x(),
                                                        to4.y(),
                                                        16F - to4.x(),
                                                        from4.y()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot4[3] + " outside 0..3");
                            },
                            switch (rot4[4]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from4.z(),
                                                        16F - to4.y(),
                                                        to4.z(),
                                                        16F - from4.y()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from4.y(),
                                                        16F - to4.z(),
                                                        to4.y(),
                                                        16F - from4.z()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from4.y(),
                                                        to4.z(),
                                                        16F - to4.y(),
                                                        from4.z()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from4.z(),
                                                        to4.y(),
                                                        16F - to4.z(),
                                                        from4.y()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot4[4] + " outside 0..3");
                            },
                            switch (rot4[5]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from4.z(),
                                                        16F - to4.y(),
                                                        to4.z(),
                                                        16F - from4.y()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from4.y(),
                                                        from4.z(),
                                                        16F - to4.y(),
                                                        to4.z()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from4.y(),
                                                        16F - from4.z(),
                                                        to4.y(),
                                                        16F - to4.z()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from4.z(),
                                                        to4.y(),
                                                        16F - to4.z(),
                                                        from4.y()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot4[5] + " outside 0..3");
                            }
                        });
                final int[] rot5 = lipRot(turn, Direction.EAST);
                Vector3f a5 =
                        new Vector3f(
                                INNER_MAX / 16F - 0.5F, 0F / 16F - 0.5F, INNER_MIN / 16F - 0.5F);
                Vector3f b5 =
                        new Vector3f(16F / 16F - 0.5F, LIP / 16F - 0.5F, INNER_MAX / 16F - 0.5F);
                turn.transformation()
                        .getMatrix()
                        .transformPosition(a5)
                        .add(0.5F, 0.5F, 0.5F)
                        .mul(16F);
                turn.transformation()
                        .getMatrix()
                        .transformPosition(b5)
                        .add(0.5F, 0.5F, 0.5F)
                        .mul(16F);
                final Vector3f from5 =
                        new Vector3f(
                                Math.min(a5.x, b5.x), Math.min(a5.y, b5.y), Math.min(a5.z, b5.z));
                final Vector3f to5 =
                        new Vector3f(
                                Math.max(a5.x, b5.x), Math.max(a5.y, b5.y), Math.max(a5.z, b5.z));
                faces(
                        builder,
                        baker,
                        carrier,
                        slots,
                        Boxes.IDENTITY,
                        local4 -> BELT,
                        null,
                        from5,
                        to5,
                        new UvRotation[] {
                            switch (rot5[0]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from5.x(), to5.z(), to5.x(), from5.z()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - to5.z(),
                                                        to5.x(),
                                                        16F - from5.z(),
                                                        from5.x()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        to5.z(),
                                                        16F - to5.x(),
                                                        from5.z(),
                                                        16F - from5.x()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from5.x(),
                                                        16F - to5.z(),
                                                        16F - to5.x(),
                                                        16F - from5.z()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot5[0] + " outside 0..3");
                            },
                            switch (rot5[1]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from5.x(), from5.z(), to5.x(), to5.z()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from5.z(),
                                                        16F - to5.x(),
                                                        to5.z(),
                                                        16F - from5.x()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from5.z(),
                                                        to5.x(),
                                                        16F - to5.z(),
                                                        from5.x()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from5.x(),
                                                        16F - from5.z(),
                                                        16F - to5.x(),
                                                        16F - to5.z()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot5[1] + " outside 0..3");
                            },
                            switch (rot5[2]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from5.x(),
                                                        16F - to5.y(),
                                                        to5.x(),
                                                        16F - from5.y()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from5.y(),
                                                        from5.x(),
                                                        16F - to5.y(),
                                                        to5.x()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from5.y(),
                                                        16F - from5.x(),
                                                        to5.y(),
                                                        16F - to5.x()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from5.x(),
                                                        to5.y(),
                                                        16F - to5.x(),
                                                        from5.y()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot5[2] + " outside 0..3");
                            },
                            switch (rot5[3]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from5.x(),
                                                        16F - to5.y(),
                                                        to5.x(),
                                                        16F - from5.y()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from5.y(),
                                                        16F - to5.x(),
                                                        to5.y(),
                                                        16F - from5.x()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from5.y(),
                                                        to5.x(),
                                                        16F - to5.y(),
                                                        from5.x()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from5.x(),
                                                        to5.y(),
                                                        16F - to5.x(),
                                                        from5.y()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot5[3] + " outside 0..3");
                            },
                            switch (rot5[4]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from5.z(),
                                                        16F - to5.y(),
                                                        to5.z(),
                                                        16F - from5.y()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from5.y(),
                                                        16F - to5.z(),
                                                        to5.y(),
                                                        16F - from5.z()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from5.y(),
                                                        to5.z(),
                                                        16F - to5.y(),
                                                        from5.z()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from5.z(),
                                                        to5.y(),
                                                        16F - to5.z(),
                                                        from5.y()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot5[4] + " outside 0..3");
                            },
                            switch (rot5[5]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from5.z(),
                                                        16F - to5.y(),
                                                        to5.z(),
                                                        16F - from5.y()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from5.y(),
                                                        from5.z(),
                                                        16F - to5.y(),
                                                        to5.z()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from5.y(),
                                                        16F - from5.z(),
                                                        to5.y(),
                                                        16F - to5.z()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from5.z(),
                                                        to5.y(),
                                                        16F - to5.z(),
                                                        from5.y()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot5[5] + " outside 0..3");
                            }
                        });
                final int[] rot6 = lipRot(turn, Direction.SOUTH);
                Vector3f a6 =
                        new Vector3f(
                                INNER_MIN / 16F - 0.5F, 0F / 16F - 0.5F, INNER_MAX / 16F - 0.5F);
                Vector3f b6 =
                        new Vector3f(INNER_MAX / 16F - 0.5F, LIP / 16F - 0.5F, 16F / 16F - 0.5F);
                turn.transformation()
                        .getMatrix()
                        .transformPosition(a6)
                        .add(0.5F, 0.5F, 0.5F)
                        .mul(16F);
                turn.transformation()
                        .getMatrix()
                        .transformPosition(b6)
                        .add(0.5F, 0.5F, 0.5F)
                        .mul(16F);
                final Vector3f from6 =
                        new Vector3f(
                                Math.min(a6.x, b6.x), Math.min(a6.y, b6.y), Math.min(a6.z, b6.z));
                final Vector3f to6 =
                        new Vector3f(
                                Math.max(a6.x, b6.x), Math.max(a6.y, b6.y), Math.max(a6.z, b6.z));
                faces(
                        builder,
                        baker,
                        carrier,
                        slots,
                        Boxes.IDENTITY,
                        local5 -> BELT,
                        null,
                        from6,
                        to6,
                        new UvRotation[] {
                            switch (rot6[0]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from6.x(), to6.z(), to6.x(), from6.z()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - to6.z(),
                                                        to6.x(),
                                                        16F - from6.z(),
                                                        from6.x()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        to6.z(),
                                                        16F - to6.x(),
                                                        from6.z(),
                                                        16F - from6.x()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from6.x(),
                                                        16F - to6.z(),
                                                        16F - to6.x(),
                                                        16F - from6.z()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot6[0] + " outside 0..3");
                            },
                            switch (rot6[1]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from6.x(), from6.z(), to6.x(), to6.z()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from6.z(),
                                                        16F - to6.x(),
                                                        to6.z(),
                                                        16F - from6.x()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from6.z(),
                                                        to6.x(),
                                                        16F - to6.z(),
                                                        from6.x()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from6.x(),
                                                        16F - from6.z(),
                                                        16F - to6.x(),
                                                        16F - to6.z()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot6[1] + " outside 0..3");
                            },
                            switch (rot6[2]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from6.x(),
                                                        16F - to6.y(),
                                                        to6.x(),
                                                        16F - from6.y()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from6.y(),
                                                        from6.x(),
                                                        16F - to6.y(),
                                                        to6.x()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from6.y(),
                                                        16F - from6.x(),
                                                        to6.y(),
                                                        16F - to6.x()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from6.x(),
                                                        to6.y(),
                                                        16F - to6.x(),
                                                        from6.y()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot6[2] + " outside 0..3");
                            },
                            switch (rot6[3]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from6.x(),
                                                        16F - to6.y(),
                                                        to6.x(),
                                                        16F - from6.y()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from6.y(),
                                                        16F - to6.x(),
                                                        to6.y(),
                                                        16F - from6.x()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from6.y(),
                                                        to6.x(),
                                                        16F - to6.y(),
                                                        from6.x()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from6.x(),
                                                        to6.y(),
                                                        16F - to6.x(),
                                                        from6.y()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot6[3] + " outside 0..3");
                            },
                            switch (rot6[4]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from6.z(),
                                                        16F - to6.y(),
                                                        to6.z(),
                                                        16F - from6.y()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from6.y(),
                                                        16F - to6.z(),
                                                        to6.y(),
                                                        16F - from6.z()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from6.y(),
                                                        to6.z(),
                                                        16F - to6.y(),
                                                        from6.z()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from6.z(),
                                                        to6.y(),
                                                        16F - to6.z(),
                                                        from6.y()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot6[4] + " outside 0..3");
                            },
                            switch (rot6[5]) {
                                case 0 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from6.z(),
                                                        16F - to6.y(),
                                                        to6.z(),
                                                        16F - from6.y()),
                                                Quadrant.R0));
                                case 1 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from6.y(),
                                                        from6.z(),
                                                        16F - to6.y(),
                                                        to6.z()),
                                                Quadrant.R90));
                                case 2 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        from6.y(),
                                                        16F - from6.z(),
                                                        to6.y(),
                                                        16F - to6.z()),
                                                Quadrant.R90));
                                case 3 ->
                                        (new UvRotation(
                                                new CuboidFace.UVs(
                                                        16F - from6.z(),
                                                        to6.y(),
                                                        16F - to6.z(),
                                                        from6.y()),
                                                Quadrant.R0));
                                default ->
                                        throw new IllegalArgumentException(
                                                "uvRotate value " + rot6[5] + " outside 0..3");
                            }
                        });
            }

            Vector3f a7 = new Vector3f(0F / 16F - 0.5F, 0F / 16F - 0.5F, 0F / 16F - 0.5F);
            Vector3f b7 = new Vector3f(16F / 16F - 0.5F, 16F / 16F - 0.5F, INNER_MIN / 16F - 0.5F);
            turn.transformation().getMatrix().transformPosition(a7).add(0.5F, 0.5F, 0.5F).mul(16F);
            turn.transformation().getMatrix().transformPosition(b7).add(0.5F, 0.5F, 0.5F).mul(16F);
            final Vector3f from7 =
                    new Vector3f(Math.min(a7.x, b7.x), Math.min(a7.y, b7.y), Math.min(a7.z, b7.z));
            final Vector3f to7 =
                    new Vector3f(Math.max(a7.x, b7.x), Math.max(a7.y, b7.y), Math.max(a7.z, b7.z));
            faces(
                    builder,
                    baker,
                    carrier,
                    slots,
                    Boxes.IDENTITY,
                    local6 -> CONCRETE,
                    null,
                    from7,
                    to7,
                    new UvRotation[] {
                        new UvRotation(
                                new CuboidFace.UVs(from7.x(), to7.z(), to7.x(), from7.z()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(from7.x(), from7.z(), to7.x(), to7.z()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(
                                        from7.x(), 16F - to7.y(), to7.x(), 16F - from7.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(
                                        from7.x(), 16F - to7.y(), to7.x(), 16F - from7.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(
                                        from7.z(), 16F - to7.y(), to7.z(), 16F - from7.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(
                                        from7.z(), 16F - to7.y(), to7.z(), 16F - from7.y()),
                                Quadrant.R0)
                    });
            Vector3f a8 = new Vector3f(0F / 16F - 0.5F, 0F / 16F - 0.5F, INNER_MAX / 16F - 0.5F);
            Vector3f b8 = new Vector3f(INNER_MIN / 16F - 0.5F, 16F / 16F - 0.5F, 16F / 16F - 0.5F);
            turn.transformation().getMatrix().transformPosition(a8).add(0.5F, 0.5F, 0.5F).mul(16F);
            turn.transformation().getMatrix().transformPosition(b8).add(0.5F, 0.5F, 0.5F).mul(16F);
            final Vector3f from8 =
                    new Vector3f(Math.min(a8.x, b8.x), Math.min(a8.y, b8.y), Math.min(a8.z, b8.z));
            final Vector3f to8 =
                    new Vector3f(Math.max(a8.x, b8.x), Math.max(a8.y, b8.y), Math.max(a8.z, b8.z));
            faces(
                    builder,
                    baker,
                    carrier,
                    slots,
                    Boxes.IDENTITY,
                    local7 -> CONCRETE,
                    null,
                    from8,
                    to8,
                    new UvRotation[] {
                        new UvRotation(
                                new CuboidFace.UVs(from8.x(), to8.z(), to8.x(), from8.z()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(from8.x(), from8.z(), to8.x(), to8.z()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(
                                        from8.x(), 16F - to8.y(), to8.x(), 16F - from8.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(
                                        from8.x(), 16F - to8.y(), to8.x(), 16F - from8.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(
                                        from8.z(), 16F - to8.y(), to8.z(), 16F - from8.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(
                                        from8.z(), 16F - to8.y(), to8.z(), 16F - from8.y()),
                                Quadrant.R0)
                    });
            Vector3f a9 =
                    new Vector3f(INNER_MAX / 16F - 0.5F, 0F / 16F - 0.5F, INNER_MAX / 16F - 0.5F);
            Vector3f b9 = new Vector3f(16F / 16F - 0.5F, 16F / 16F - 0.5F, 16F / 16F - 0.5F);
            turn.transformation().getMatrix().transformPosition(a9).add(0.5F, 0.5F, 0.5F).mul(16F);
            turn.transformation().getMatrix().transformPosition(b9).add(0.5F, 0.5F, 0.5F).mul(16F);
            final Vector3f from9 =
                    new Vector3f(Math.min(a9.x, b9.x), Math.min(a9.y, b9.y), Math.min(a9.z, b9.z));
            final Vector3f to9 =
                    new Vector3f(Math.max(a9.x, b9.x), Math.max(a9.y, b9.y), Math.max(a9.z, b9.z));
            faces(
                    builder,
                    baker,
                    carrier,
                    slots,
                    Boxes.IDENTITY,
                    local8 -> CONCRETE,
                    null,
                    from9,
                    to9,
                    new UvRotation[] {
                        new UvRotation(
                                new CuboidFace.UVs(from9.x(), to9.z(), to9.x(), from9.z()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(from9.x(), from9.z(), to9.x(), to9.z()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(
                                        from9.x(), 16F - to9.y(), to9.x(), 16F - from9.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(
                                        from9.x(), 16F - to9.y(), to9.x(), 16F - from9.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(
                                        from9.z(), 16F - to9.y(), to9.z(), 16F - from9.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(
                                        from9.z(), 16F - to9.y(), to9.z(), 16F - from9.y()),
                                Quadrant.R0)
                    });
            final int[] rot10 = liftRot(facing, false);
            Vector3f a10 =
                    new Vector3f(INNER_MIN / 16F - 0.5F, 0F / 16F - 0.5F, INNER_MIN / 16F - 0.5F);
            Vector3f b10 =
                    new Vector3f(INNER_MAX / 16F - 0.5F, 16F / 16F - 0.5F, STRIP / 16F - 0.5F);
            turn.transformation().getMatrix().transformPosition(a10).add(0.5F, 0.5F, 0.5F).mul(16F);
            turn.transformation().getMatrix().transformPosition(b10).add(0.5F, 0.5F, 0.5F).mul(16F);
            final Vector3f from10 =
                    new Vector3f(
                            Math.min(a10.x, b10.x), Math.min(a10.y, b10.y), Math.min(a10.z, b10.z));
            final Vector3f to10 =
                    new Vector3f(
                            Math.max(a10.x, b10.x), Math.max(a10.y, b10.y), Math.max(a10.z, b10.z));
            faces(
                    builder,
                    baker,
                    carrier,
                    slots,
                    Boxes.IDENTITY,
                    local9 -> BELT,
                    null,
                    from10,
                    to10,
                    new UvRotation[] {
                        switch (rot10[0]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    from10.x(), to10.z(), to10.x(), from10.z()),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    16F - to10.z(),
                                                    to10.x(),
                                                    16F - from10.z(),
                                                    from10.x()),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    to10.z(),
                                                    16F - to10.x(),
                                                    from10.z(),
                                                    16F - from10.x()),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    16F - from10.x(),
                                                    16F - to10.z(),
                                                    16F - to10.x(),
                                                    16F - from10.z()),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + rot10[0] + " outside 0..3");
                        },
                        switch (rot10[1]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    from10.x(), from10.z(), to10.x(), to10.z()),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    from10.z(),
                                                    16F - to10.x(),
                                                    to10.z(),
                                                    16F - from10.x()),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    16F - from10.z(),
                                                    to10.x(),
                                                    16F - to10.z(),
                                                    from10.x()),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    16F - from10.x(),
                                                    16F - from10.z(),
                                                    16F - to10.x(),
                                                    16F - to10.z()),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + rot10[1] + " outside 0..3");
                        },
                        switch (rot10[2]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    from10.x(),
                                                    16F - to10.y(),
                                                    to10.x(),
                                                    16F - from10.y()),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    16F - from10.y(),
                                                    from10.x(),
                                                    16F - to10.y(),
                                                    to10.x()),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    from10.y(),
                                                    16F - from10.x(),
                                                    to10.y(),
                                                    16F - to10.x()),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    16F - from10.x(),
                                                    to10.y(),
                                                    16F - to10.x(),
                                                    from10.y()),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + rot10[2] + " outside 0..3");
                        },
                        switch (rot10[3]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    from10.x(),
                                                    16F - to10.y(),
                                                    to10.x(),
                                                    16F - from10.y()),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    from10.y(),
                                                    16F - to10.x(),
                                                    to10.y(),
                                                    16F - from10.x()),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    16F - from10.y(),
                                                    to10.x(),
                                                    16F - to10.y(),
                                                    from10.x()),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    16F - from10.x(),
                                                    to10.y(),
                                                    16F - to10.x(),
                                                    from10.y()),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + rot10[3] + " outside 0..3");
                        },
                        switch (rot10[4]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    from10.z(),
                                                    16F - to10.y(),
                                                    to10.z(),
                                                    16F - from10.y()),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    from10.y(),
                                                    16F - to10.z(),
                                                    to10.y(),
                                                    16F - from10.z()),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    16F - from10.y(),
                                                    to10.z(),
                                                    16F - to10.y(),
                                                    from10.z()),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    16F - from10.z(),
                                                    to10.y(),
                                                    16F - to10.z(),
                                                    from10.y()),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + rot10[4] + " outside 0..3");
                        },
                        switch (rot10[5]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    from10.z(),
                                                    16F - to10.y(),
                                                    to10.z(),
                                                    16F - from10.y()),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    16F - from10.y(),
                                                    from10.z(),
                                                    16F - to10.y(),
                                                    to10.z()),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    from10.y(),
                                                    16F - from10.z(),
                                                    to10.y(),
                                                    16F - to10.z()),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(
                                                    16F - from10.z(),
                                                    to10.y(),
                                                    16F - to10.z(),
                                                    from10.y()),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + rot10[5] + " outside 0..3");
                        }
                    });

            if (part == ConveyorLiftPart.BOTTOM) {
                Vector3f a11 =
                        new Vector3f(INNER_MIN / 16F - 0.5F, 0F / 16F - 0.5F, STRIP / 16F - 0.5F);
                Vector3f b11 =
                        new Vector3f(
                                INNER_MAX / 16F - 0.5F, LIP / 16F - 0.5F, INNER_MAX / 16F - 0.5F);
                turn.transformation()
                        .getMatrix()
                        .transformPosition(a11)
                        .add(0.5F, 0.5F, 0.5F)
                        .mul(16F);
                turn.transformation()
                        .getMatrix()
                        .transformPosition(b11)
                        .add(0.5F, 0.5F, 0.5F)
                        .mul(16F);
                final Vector3f from11 =
                        new Vector3f(
                                Math.min(a11.x, b11.x),
                                Math.min(a11.y, b11.y),
                                Math.min(a11.z, b11.z));
                final Vector3f to11 =
                        new Vector3f(
                                Math.max(a11.x, b11.x),
                                Math.max(a11.y, b11.y),
                                Math.max(a11.z, b11.z));
                faces(
                        builder,
                        baker,
                        carrier,
                        slots,
                        Boxes.IDENTITY,
                        local10 -> IRON,
                        null,
                        from11,
                        to11,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(from11.x(), to11.z(), to11.x(), from11.z()),
                                    Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(from11.x(), from11.z(), to11.x(), to11.z()),
                                    Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(
                                            from11.x(), 16F - to11.y(), to11.x(), 16F - from11.y()),
                                    Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(
                                            from11.x(), 16F - to11.y(), to11.x(), 16F - from11.y()),
                                    Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(
                                            from11.z(), 16F - to11.y(), to11.z(), 16F - from11.y()),
                                    Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(
                                            from11.z(), 16F - to11.y(), to11.z(), 16F - from11.y()),
                                    Quadrant.R0)
                        });
            }
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return this;
        }
    }
}
