// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public record PipeModel(Kind kind) implements BlockModel<PipeModel.Mesh> {

    public static final Identifier FRAME_BASE = Library.id("block/deco_pipe_frame_base");
    public static final String TOP_SLOT = "top";
    public static final String SIDE_SLOT = "side";
    public static final String FRAME_SLOT = "frame";
    public static final String MESH_SLOT = "mesh";
    private static final Identifier FRAME_OBJ = Library.id("models/blocks/pipe_frame.obj");

    private static final float OFFSET_Y = 0.5F;
    private static final String[] TOP = {"Top"};
    private static final String[] SIDE = {"Side"};
    private static final String[] FRAME = {"Frame"};
    private static final String[] MESH = {"Mesh"};
    private static final ModelState IDENTITY = Boxes.IDENTITY;
    private static final ModelState X90 = axisState(90F, 0F);
    private static final ModelState X90_Y90 = axisState(90F, 90F);

    private static ModelState axisState(float xDeg, float yDeg) {
        Matrix4f m =
                new Matrix4f()
                        .rotateY((float) Math.toRadians(yDeg))
                        .rotateX((float) Math.toRadians(xDeg));
        return Boxes.pose(new Transformation(m));
    }

    private static ModelState rotationFor(Direction.Axis axis) {
        return switch (axis) {
            case Y -> IDENTITY;
            case Z -> X90;
            case X -> X90_Y90;
        };
    }

    private static QuadCollection build(
            TextureSlots slots,
            ModelBaker baker,
            ResolvedModel base,
            Kind kind,
            HFRWavefrontObject obj,
            @Nullable HFRWavefrontObject frameObj,
            ModelState rotation) {
        Material.Baked top = BlockModel.slot(baker, base, slots, TOP_SLOT);
        Material.Baked side = BlockModel.slot(baker, base, slots, SIDE_SLOT);

        List<ObjUnbakedGeometry.Group> groups = new ArrayList<>();
        groups.add(new ObjUnbakedGeometry.Group(obj, TOP, top, rotation));
        groups.add(new ObjUnbakedGeometry.Group(obj, SIDE, side, rotation));
        if (kind.framed && frameObj != null) {
            ResolvedModel frameBase = baker.getModel(FRAME_BASE);
            TextureSlots frameSlots = frameBase.getTopTextureSlots();
            Material.Baked frame = BlockModel.slot(baker, frameBase, frameSlots, FRAME_SLOT);
            Material.Baked mesh = BlockModel.slot(baker, frameBase, frameSlots, MESH_SLOT);
            groups.add(new ObjUnbakedGeometry.Group(frameObj, FRAME, frame, rotation));
            groups.add(new ObjUnbakedGeometry.Group(frameObj, MESH, mesh, rotation));
        }
        return ObjUnbakedGeometry.bakeGroups(groups, 0F, OFFSET_Y, 0F);
    }

    @Override
    public Mesh prepare() {
        return new Mesh(Meshes.load(kind.obj), kind.framed ? Meshes.load(FRAME_OBJ) : null);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(Mesh prepared, Block block, BlockState state) {
        return new Root(
                BlockModel.base(block),
                kind,
                prepared.obj(),
                prepared.frame(),
                state.getValue(RotatedPillarBlock.AXIS));
    }

    public enum Kind {
        PLAIN("models/blocks/pipe.obj", false),
        RIM("models/blocks/pipe_rim.obj", false),
        QUAD("models/blocks/pipe_quad.obj", false),
        FRAMED("models/blocks/pipe_rim.obj", true);

        public final Identifier obj;
        public final boolean framed;

        Kind(String path, boolean framed) {
            this.obj = Library.id(path);
            this.framed = framed;
        }
    }

    public record Mesh(HFRWavefrontObject obj, @Nullable HFRWavefrontObject frame) {}

    public record Root(
            Identifier carrier,
            Kind kind,
            HFRWavefrontObject obj,
            @Nullable HFRWavefrontObject frameObj,
            Direction.Axis axis)
            implements CarrierRoot {

        @Override
        public void resolveExtraDependencies(ResolvableModel.Resolver resolver) {
            if (kind.framed) resolver.markDependency(FRAME_BASE);
        }

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            return build(slots, baker, carrier, kind, obj, frameObj, rotationFor(axis));
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return axis;
        }
    }
}
