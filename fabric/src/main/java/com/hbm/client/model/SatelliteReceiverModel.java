// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.render.loader.GroupObject;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public record SatelliteReceiverModel() implements BlockModel<HFRWavefrontObject> {

    private static final Shape[] SHAPES = {
        new Shape(0, 0, 0F, 0F, 0F, 12F, 16F, 12F, -6F, 8F, -6F, 0F, 0F, 0F),
        new Shape(10, 28, 3F, 9F, -8F, 8F, 8F, 2F, -3F, 6F, 0F, -0.2617994F, -0.4363323F, 0F),
        new Shape(0, 39, 3F, 7F, -10F, 8F, 2F, 3F, -3F, 6F, 0F, -0.2617994F, -0.4363323F, 0F),
        new Shape(0, 28, 1F, 9F, -10F, 2F, 8F, 3F, -3F, 6F, 0F, -0.2617994F, -0.4363323F, 0F),
        new Shape(0, 28, 11F, 9F, -10F, 2F, 8F, 3F, -3F, 6F, 0F, -0.2617994F, -0.4363323F, 0F),
        new Shape(0, 39, 3F, 17F, -10F, 8F, 2F, 3F, -3F, 6F, 0F, -0.2617994F, -0.4363323F, 0F),
        new Shape(0, 44, 6F, 12F, -11F, 2F, 2F, 3F, -3F, 6F, 0F, -0.2617994F, -0.4363323F, 0F),
        new Shape(0, 49, 6.5F, 12.5F, -14F, 1F, 1F, 3F, -3F, 6F, 0F, -0.2617994F, -0.4363323F, 0F),
        new Shape(0, 53, 6F, 12F, -16F, 2F, 2F, 2F, -3F, 6F, 0F, -0.2617994F, -0.4363323F, 0F),
    };

    private static String[] groupName(int shape) {
        return new String[] {"shape" + shape};
    }

    private static float[] corners(ModelPart.Cube cube) {
        float[] out = new float[cube.polygons.length * 4 * 5];
        int at = 0;
        for (ModelPart.Polygon polygon : cube.polygons) {
            for (ModelPart.Vertex corner : polygon.vertices()) {
                out[at] = corner.x();
                out[at + 1] = corner.y();
                out[at + 2] = corner.z();
                out[at + 3] = corner.u();
                out[at + 4] = corner.v();
                at += 5;
            }
        }
        return out;
    }

    public static float yawFor(Direction facing) {
        return Facing.yawCcw(facing, 0);
    }

    public static Matrix4fc matrixFor(Direction facing) {
        return new Matrix4f()
                .translate(0.5F, 1.5F, 0.5F)
                .rotateZ((float) Math.toRadians(180))
                .rotateY((float) Math.toRadians(yawFor(facing)));
    }

    @Override
    public HFRWavefrontObject prepare() {
        List<GroupObject> groups = new ArrayList<>(SHAPES.length);
        for (int i = 0; i < SHAPES.length; i++) {
            Shape shape = SHAPES[i];
            ModelPart.Cube cube =
                    new ModelPart.Cube(
                            shape.u(),
                            shape.v(),
                            shape.x(),
                            shape.y(),
                            shape.z(),
                            shape.w(),
                            shape.h(),
                            shape.d(),
                            0F,
                            0F,
                            0F,
                            true,
                            64F,
                            64F,
                            EnumSet.allOf(Direction.class));

            groups.add(GroupObject.ofPosUv("shape" + i, corners(cube)));
        }
        return new HFRWavefrontObject("satellite_receiver boxes", groups);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(
                BlockModel.base(block),
                prepared,
                state.getValue(HorizontalDirectionalBlock.FACING));
    }

    private record Shape(
            int u,
            int v,
            float x,
            float y,
            float z,
            float w,
            float h,
            float d,
            float pivotX,
            float pivotY,
            float pivotZ,
            float rotX,
            float rotY,
            float rotZ) {}

    public record Root(Identifier carrier, HFRWavefrontObject mesh, Direction facing)
            implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.SINGLE_SLOT);
            Matrix4fc facingMatrix = matrixFor(facing);

            List<ObjUnbakedGeometry.Group> groups = new ArrayList<>(SHAPES.length);
            for (int i = 0; i < SHAPES.length; i++) {
                Shape shape = SHAPES[i];
                Matrix4f matrix =
                        new Matrix4f(facingMatrix)
                                .scale(0.0625F)
                                .translate(shape.pivotX(), shape.pivotY(), shape.pivotZ())
                                .rotateZ(shape.rotZ())
                                .rotateY(shape.rotY())
                                .rotateX(shape.rotX());
                groups.add(ObjUnbakedGeometry.Group.raw(mesh, groupName(i), material, matrix));
            }
            return ObjUnbakedGeometry.bakeGroups(groups, 0F, ObjUnbakedGeometry.RAW_OFFSET_Y, 0F);
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return facing;
        }
    }
}
