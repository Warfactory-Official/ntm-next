// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.network.DroneWaypointBlock;
import com.hbm.render.loader.HFRWavefrontObject;
import java.util.List;
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

public record DroneWaypointModel(Identifier base) implements BlockModel<HFRWavefrontObject> {

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.faceNormals(RadioTorchModel.OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(base, prepared, state);
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
                    BlockModelRotation.get(
                            RadioTorchModel.rotationFor(state.getValue(DroneWaypointBlock.FACING))),
                    0F,
                    0.5F,
                    0F,
                    false,
                    false);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return List.of(carrier, state.getValue(DroneWaypointBlock.FACING));
        }
    }
}
