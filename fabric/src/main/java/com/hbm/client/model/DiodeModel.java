// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.network.CableDiodeBlock;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
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
import org.joml.Vector3f;

public record DiodeModel() implements BlockModel<HFRWavefrontObject> {

    public static final Identifier BASE = Library.id("block/cable_diode_base");
    private static final Identifier OBJ = Library.id("models/blocks/cable_neo.obj");
    private static final float OFFSET_Y = 0.5F;

    private static final float WIDTH = 0.875F * 16F;
    private static final float PAD_MIN = 0.125F * 16F;
    private static final float PAD_MAX = 0.875F * 16F;

    private static final int NORTH = 1, EAST = 2, SOUTH = 4, WEST = 8, UP = 16, DOWN = 32;
    private static final String[][] PARTS_BY_MASK = BlockModel.partsByMask(DiodeModel::selectParts);

    private static String[] selectParts(int mask) {
        String[] parts = new String[Integer.bitCount(mask)];
        int i = 0;
        if ((mask & EAST) != 0) parts[i++] = "posX";
        if ((mask & WEST) != 0) parts[i++] = "negX";
        if ((mask & UP) != 0) parts[i++] = "posY";
        if ((mask & DOWN) != 0) parts[i++] = "negY";
        if ((mask & SOUTH) != 0) parts[i++] = "negZ";
        if ((mask & NORTH) != 0) parts[i++] = "posZ";
        return parts;
    }

    public static String[] partsFor(BlockState state) {
        int mask = 0;
        if (state.getValue(CableDiodeBlock.connectionFor(Direction.NORTH))) mask |= NORTH;
        if (state.getValue(CableDiodeBlock.connectionFor(Direction.EAST))) mask |= EAST;
        if (state.getValue(CableDiodeBlock.connectionFor(Direction.SOUTH))) mask |= SOUTH;
        if (state.getValue(CableDiodeBlock.connectionFor(Direction.WEST))) mask |= WEST;
        if (state.getValue(CableDiodeBlock.connectionFor(Direction.UP))) mask |= UP;
        if (state.getValue(CableDiodeBlock.connectionFor(Direction.DOWN))) mask |= DOWN;
        return PARTS_BY_MASK[mask];
    }

    private static void addHousing(
            List<ObjUnbakedGeometry.BoxQuad> into,
            ModelBaker baker,
            Direction facing,
            Material.Baked material) {
        float minX = facing == Direction.WEST ? WIDTH : 0F;
        float minY = facing == Direction.DOWN ? WIDTH : 0F;
        float minZ = facing == Direction.NORTH ? WIDTH : 0F;
        float maxX = 16F - (facing == Direction.EAST ? WIDTH : 0F);
        float maxY = 16F - (facing == Direction.UP ? WIDTH : 0F);
        float maxZ = 16F - (facing == Direction.SOUTH ? WIDTH : 0F);
        box(into, baker, minX, minY, minZ, maxX, maxY, maxZ, material);
    }

    private static void addPad(
            List<ObjUnbakedGeometry.BoxQuad> into, ModelBaker baker, Material.Baked material) {
        box(into, baker, PAD_MIN, PAD_MIN, PAD_MIN, PAD_MAX, PAD_MAX, PAD_MAX, material);
    }

    private static void box(
            List<ObjUnbakedGeometry.BoxQuad> into,
            ModelBaker baker,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            Material.Baked material) {
        Vector3f from = new Vector3f(x0, y0, z0);
        Vector3f to = new Vector3f(x1, y1, z1);
        for (Direction dir : Direction.VALUES) {
            into.add(
                    new ObjUnbakedGeometry.BoxQuad(
                            Boxes.face(baker, from, to, dir, material),
                            ObjUnbakedGeometry.WHITE,
                            null,
                            QuadLighting.cell(0, 0, 0)));
        }
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(prepared, state.getValue(CableDiodeBlock.FACING), partsFor(state));
    }

    public record Root(HFRWavefrontObject obj, Direction facing, String[] parts)
            implements CarrierRoot {

        @Override
        public Identifier carrier() {
            return BASE;
        }

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked housing = BlockModel.slot(baker, carrier, slots, "housing");
            Material.Baked pad = BlockModel.slot(baker, carrier, slots, "pad");
            Material.Baked stub = BlockModel.slot(baker, carrier, slots, "stub");

            List<ObjUnbakedGeometry.BoxQuad> boxes = new ArrayList<>();
            addHousing(boxes, baker, facing, housing);
            addPad(boxes, baker, pad);
            return ObjUnbakedGeometry.bakeGroups(
                    List.of(
                            new ObjUnbakedGeometry.Group(
                                    obj, parts, stub, BlockModelRotation.IDENTITY)),
                    boxes,
                    0F,
                    OFFSET_Y,
                    0F);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {

            return List.of(facing, parts);
        }
    }
}
