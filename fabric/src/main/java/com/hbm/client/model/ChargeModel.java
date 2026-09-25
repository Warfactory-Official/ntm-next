// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.bomb.BlockChargeBase;
import com.hbm.render.loader.HFRWavefrontObject;
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

public record ChargeModel(Identifier mesh) implements BlockModel<HFRWavefrontObject> {

    public static Matrix4fc matrixFor(Direction facing) {
        Matrix4f matrix = new Matrix4f().translate(0.5F, 0.5F, 0.5F);
        return switch (facing) {
            case DOWN -> matrix.rotateZ((float) Math.PI);
            case UP -> matrix;
            case NORTH -> matrix.rotateY((float) (Math.PI / 2)).rotateZ((float) (-Math.PI / 2));
            case SOUTH -> matrix.rotateY((float) (-Math.PI / 2)).rotateZ((float) (-Math.PI / 2));
            case WEST -> matrix.rotateY((float) Math.PI).rotateZ((float) (-Math.PI / 2));
            case EAST -> matrix.rotateZ((float) (-Math.PI / 2));
        };
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(mesh);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(
                BlockModel.base(block),
                prepared,
                matrixFor(state.getValue(BlockChargeBase.FACING)));
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
            return ObjUnbakedGeometry.bakeGroups(
                    List.of(ObjUnbakedGeometry.Group.raw(obj, null, material, matrix)),
                    0F,
                    ObjUnbakedGeometry.RAW_OFFSET_Y,
                    0F);
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return matrix;
        }
    }
}
