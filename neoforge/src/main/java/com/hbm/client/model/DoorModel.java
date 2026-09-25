// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.generic.BlockDoorGeneric;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.render.DoorRenderer;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.DoorDecl;
import com.hbm.util.Facing;
import com.mojang.math.OctahedralGroup;
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

public record DoorModel() implements SimpleBlockModel {

    public static final String SHEET_SLOT = "sheet";

    @Override
    public BlockStateModel.UnbakedRoot root(Block block, BlockState state) {
        return Root.of(state);
    }

    public record Root(Identifier carrier, HFRWavefrontObject obj, DoorDecl decl, BlockState state)
            implements CarrierRoot {

        public static Root of(BlockState state) {
            BlockDoorGeneric block = (BlockDoorGeneric) state.getBlock();

            return new Root(
                    BlockModel.base(block),
                    block.type.getModel(),
                    block.type,
                    state.setValue(BlockDoorGeneric.OPEN, Boolean.FALSE));
        }

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            if (decl.getStaticParts().length == 0) return new QuadCollection.Builder().build();

            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, SHEET_SLOT + textureSlot());

            OctahedralGroup facing =
                    Facing.yawRotation(state.getValue(BlockMultiblockCore.FACING), 90);

            OctahedralGroup rotation = facing.compose(Facing.ry(decl.getStaticYaw()));
            float[] offset = decl.getStaticOffset();
            DoorRenderer sedna = decl.getSEDNARenderer();

            ObjUnbakedGeometry.Overrides overrides =
                    sedna != null && !sedna.culls()
                            ? new ObjUnbakedGeometry.Overrides(
                                    null, null, null, decl.getStaticParts())
                            : ObjUnbakedGeometry.Overrides.NONE;
            return ObjUnbakedGeometry.bakeGroups(
                    List.of(
                            ObjUnbakedGeometry.Group.opaque(
                                    obj,
                                    decl.getStaticParts(),
                                    material,
                                    BlockModelRotation.get(rotation))),
                    List.of(),
                    offset[0],
                    offset[1],
                    offset[2],
                    overrides);
        }

        private int textureSlot() {
            String part = decl.getStaticParts()[0];
            int skin = state.getValue(BlockDoorGeneric.SKIN) % Math.max(1, decl.getSkinCount());
            return decl.getDistinctTextures(part).indexOf(decl.getTextureForPart(skin, part));
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return (state.getValue(BlockMultiblockCore.FACING).get3DDataValue() << 3)
                    | (decl.getStaticParts().length == 0 ? 0 : textureSlot());
        }
    }
}
