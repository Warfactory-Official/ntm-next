// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
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

public record DeuteriumTowerModel() implements BlockModel<HFRWavefrontObject> {

    private static final Identifier OBJ = Library.id("models/machines/machine_deuterium_tower.obj");
    private static final ObjUnbakedGeometry.Overrides NO_CULL =
            new ObjUnbakedGeometry.Overrides(null, null, null, new String[] {"Cube.007_Cube.009"});
    private static final float BODY_YAW = 180F;
    private static final int NORTH_YAW = 0;

    private static Matrix4fc matrixFor(Direction facing) {

        float tx;
        float tz;
        switch (facing) {
            case NORTH -> {
                tx = 0F;
                tz = -1F;
            }
            case SOUTH -> {
                tx = 1F;
                tz = 0F;
            }
            case WEST -> {
                tx = 1F;
                tz = -1F;
            }
            case EAST -> {
                tx = 0F;
                tz = 0F;
            }
            default ->
                    throw new IllegalArgumentException(
                            "the tower's facing is horizontal: " + facing);
        }
        return new Matrix4f()
                .rotateY((float) Math.toRadians(BODY_YAW))
                .rotateY((float) Math.toRadians(Facing.yaw(facing, NORTH_YAW)))
                .translate(tx, 0F, tz);
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(
                BlockModel.base(block),
                prepared,
                matrixFor(state.getValue(BlockMultiblockCore.FACING)));
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
            ObjUnbakedGeometry.Group group =
                    ObjUnbakedGeometry.Group.rawSmooth(obj, null, material, matrix);
            return ObjUnbakedGeometry.bakeGroups(
                    List.of(group), List.of(), 0F, ObjUnbakedGeometry.RAW_OFFSET_Y, 0F, NO_CULL);
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return matrix;
        }
    }
}
