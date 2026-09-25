// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.machine.MachineFan;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.mojang.math.OctahedralGroup;
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

public record FanModel() implements BlockModel<HFRWavefrontObject> {

    public static final Identifier OBJ = Library.id("models/machines/fan.obj");
    public static final Identifier BASE_MODEL = Library.id("block/fan_base");

    public static final String BLADES = "Blades";
    private static final String[] FRAME = {"Frame"};

    private static OctahedralGroup rotationFor(Direction facing) {
        return switch (facing) {
            case DOWN -> Facing.rx(180);
            case UP -> OctahedralGroup.IDENTITY;
            case NORTH -> Facing.rx(-90);
            case SOUTH -> Facing.rx(90);
            case WEST -> Facing.rz(90);
            case EAST -> Facing.rz(-90);
        };
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(prepared, state);
    }

    public record Root(HFRWavefrontObject obj, BlockState state) implements CarrierRoot {

        @Override
        public Identifier carrier() {
            return BASE_MODEL;
        }

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.SINGLE_SLOT);
            Direction facing = state.getValue(MachineFan.FACING);
            return ObjUnbakedGeometry.bakeGroups(
                    obj,
                    FRAME,
                    material,
                    BlockModelRotation.get(rotationFor(facing)),
                    0F,
                    0F,
                    0F,
                    false);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return state.getValue(MachineFan.FACING);
        }
    }
}
