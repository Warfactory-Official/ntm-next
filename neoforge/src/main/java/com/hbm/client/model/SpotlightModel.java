// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.machine.BlockSpotlight;
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
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public record SpotlightModel(BlockSpotlight.LightType type)
        implements BlockModel<SpotlightModel.Mesh> {

    static final float OFFSET_X = -0.5F;
    static final float OFFSET_Y = 0.5F;
    static final float OFFSET_Z = 0F;

    private static Identifier objFor(BlockSpotlight.LightType type) {
        return Library.id(
                switch (type) {
                    case INCANDESCENT -> "models/lights/cage_lamp.obj";
                    case FLUORESCENT -> "models/lights/fluorescent_lamp.obj";
                    case HALOGEN -> "models/lights/flood_lamp.obj";
                });
    }

    public static String[] partsFor(BlockSpotlight.LightType type) {
        return switch (type) {
            case INCANDESCENT -> new String[] {"CageLamp"};
            case FLUORESCENT -> new String[] {"FluoroSingle"};
            case HALOGEN -> new String[] {"FloodLamp"};
        };
    }

    public static Identifier baseModel(BlockSpotlight.LightType type, BlockState state) {
        String name =
                switch (type) {
                    case INCANDESCENT -> "spotlight_incandescent";
                    case FLUORESCENT -> "spotlight_fluoro";
                    case HALOGEN -> "spotlight_halogen";
                };
        return Library.id(
                "block/" + name + (state.getValue(BlockSpotlight.LIT) ? "" : "_off") + "_base");
    }

    static OctahedralGroup rotationFor(Direction facing) {
        return switch (facing) {
            case DOWN -> OctahedralGroup.BLOCK_ROT_Z_90;
            case UP -> OctahedralGroup.BLOCK_ROT_Z_270;
            case NORTH -> OctahedralGroup.BLOCK_ROT_Y_270;
            case SOUTH -> OctahedralGroup.BLOCK_ROT_Y_90;
            case WEST -> OctahedralGroup.BLOCK_ROT_Y_180;
            case EAST -> OctahedralGroup.IDENTITY;
        };
    }

    @Override
    public Mesh prepare() {
        return new Mesh(Meshes.faceNormals(objFor(type)), partsFor(type));
    }

    @Override
    public BlockStateModel.UnbakedRoot root(Mesh prepared, Block block, BlockState state) {
        if (type == BlockSpotlight.LightType.FLUORESCENT) {
            return new FluoroModel.Root(
                    baseModel(type, state), state.getValue(DirectionalBlock.FACING));
        }
        return new Root(baseModel(type, state), prepared.obj(), prepared.parts(), state);
    }

    public record Mesh(HFRWavefrontObject obj, String[] parts) {}

    public record Root(Identifier carrier, HFRWavefrontObject obj, String[] parts, BlockState state)
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
                    parts,
                    material,
                    BlockModelRotation.get(rotationFor(state.getValue(DirectionalBlock.FACING))),
                    OFFSET_X,
                    OFFSET_Y,
                    OFFSET_Z,
                    false,
                    false);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return List.of(carrier, state.getValue(DirectionalBlock.FACING));
        }
    }
}
