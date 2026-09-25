// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.machine.storage.BlockFluidBarrel;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import java.util.ArrayList;
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

public record BarrelModel() implements BlockModel<HFRWavefrontObject> {

    private static final Identifier OBJ = Library.id("models/blocks/barrel.obj");
    private static final String[] BODY = {"Barrel"};
    private static final String[] CONNECTOR = {"Connector"};

    private static BlockModelRotation rotationFor(Direction dir) {
        return BlockModelRotation.get(Facing.yawRotation(dir, 90));
    }

    private static ObjUnbakedGeometry.Group connector(
            HFRWavefrontObject obj, Material.Baked material, Direction dir) {
        return new ObjUnbakedGeometry.Group(obj, CONNECTOR, material, rotationFor(dir));
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(BlockModel.base(block), prepared, state);
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

            List<ObjUnbakedGeometry.Group> groups = new ArrayList<>(5);
            groups.add(
                    ObjUnbakedGeometry.Group.opaque(
                            obj, BODY, material, BlockModelRotation.IDENTITY));
            if (state.hasProperty(BlockFluidBarrel.NORTH)) {
                if (state.getValue(BlockFluidBarrel.NORTH))
                    groups.add(connector(obj, material, Direction.NORTH));
                if (state.getValue(BlockFluidBarrel.EAST))
                    groups.add(connector(obj, material, Direction.EAST));
                if (state.getValue(BlockFluidBarrel.SOUTH))
                    groups.add(connector(obj, material, Direction.SOUTH));
                if (state.getValue(BlockFluidBarrel.WEST))
                    groups.add(connector(obj, material, Direction.WEST));
            }
            return ObjUnbakedGeometry.bakeGroups(groups, 0F, 0F, 0F);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return this;
        }
    }
}
