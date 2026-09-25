// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.network.RadioTorchBlock;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.math.OctahedralGroup;
import java.util.List;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
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

public record RadioTorchModel() implements BlockModel<HFRWavefrontObject> {

    static final Identifier OBJ = Library.id("models/blocks/rtty.obj");

    public static Identifier baseModel(RadioTorchBlock.Kind kind, BlockState state) {
        String texture =
                switch (kind) {
                    case SENDER ->
                            state.getValue(RadioTorchBlock.LIT)
                                    ? "rtty_sender_on"
                                    : "rtty_sender_off";
                    case RECEIVER ->
                            state.getValue(RadioTorchBlock.LIT) ? "rtty_rec_on" : "rtty_rec_off";
                    case LOGIC ->
                            state.getValue(RadioTorchBlock.LIT)
                                    ? "rtty_logic_on"
                                    : "rtty_logic_off";
                    case COUNTER -> "rtty_counter";
                    case READER -> "rtty_reader";
                    case CONTROLLER -> "rtty_controller";
                };
        return Library.id("block/" + texture + "_base");
    }

    static OctahedralGroup rotationFor(Direction facing) {
        return switch (facing) {
            case DOWN -> OctahedralGroup.BLOCK_ROT_Z_180;
            case UP -> OctahedralGroup.IDENTITY;
            case NORTH -> OctahedralGroup.BLOCK_ROT_Y_90.compose(OctahedralGroup.BLOCK_ROT_Z_90);
            case SOUTH -> OctahedralGroup.BLOCK_ROT_Y_270.compose(OctahedralGroup.BLOCK_ROT_Z_90);
            case WEST -> OctahedralGroup.BLOCK_ROT_Y_180.compose(OctahedralGroup.BLOCK_ROT_Z_90);
            case EAST -> OctahedralGroup.BLOCK_ROT_Z_90;
        };
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.faceNormals(OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(baseModel(((RadioTorchBlock) block).kind(), state), prepared, state);
    }

    public record Root(Identifier carrier, HFRWavefrontObject obj, BlockState state)
            implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.SINGLE_SLOT);

            return ObjUnbakedGeometry.bakeGroups(
                    obj,
                    null,
                    material,
                    BlockModelRotation.get(rotationFor(state.getValue(RadioTorchBlock.FACING))),
                    0F,
                    0.5F,
                    0F,
                    false,
                    false);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return List.of(carrier, state.getValue(RadioTorchBlock.FACING));
        }
    }
}
