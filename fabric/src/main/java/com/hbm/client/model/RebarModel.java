// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.config.RenderConfig;
import com.hbm.platform.Services;
import com.hbm.tileentity.BlockEntityRebar;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class RebarModel implements BlockStateModel {

    private static final float LO = 7F;
    private static final float OFFSET = 4F;
    private static final float MIN = -0.016F;
    private static final float MAX = 16.016F;

    private final ModelBaker baker;
    private final ResolvedModel base;
    private final Material.Baked particle;
    private final Material.Baked concrete;
    private final BlockStateModelPart rods;
    private final BlockStateModelPart simple;
    private final @Nullable BlockStateModelPart[] fills =
            new BlockStateModelPart[BlockEntityRebar.FULL + 1];

    private RebarModel(ModelBaker baker, ResolvedModel base) {
        this.baker = baker;
        this.base = base;
        TextureSlots slots = base.getTopTextureSlots();
        this.particle = base.resolveParticleMaterial(slots, baker);
        Material.Baked rebar = BlockModel.slot(baker, base, slots, "rebar");
        this.concrete = BlockModel.slot(baker, base, slots, "fill");

        List<ObjUnbakedGeometry.BoxQuad> full = new ArrayList<>();
        box(
                full,
                rebar,
                (LO - OFFSET),
                MIN,
                (LO - OFFSET),
                (LO - OFFSET) + 2F,
                MAX,
                (LO - OFFSET) + 2F,
                RebarUv.W0);
        box(
                full,
                rebar,
                MIN,
                (LO - OFFSET),
                (LO - OFFSET),
                MAX,
                (LO - OFFSET) + 2F,
                (LO - OFFSET) + 2F,
                RebarUv.W1);
        box(
                full,
                rebar,
                (LO - OFFSET),
                (LO - OFFSET),
                MIN,
                (LO - OFFSET) + 2F,
                (LO - OFFSET) + 2F,
                MAX,
                RebarUv.W2);

        box(
                full,
                rebar,
                (LO - OFFSET),
                MIN,
                (LO + OFFSET),
                (LO - OFFSET) + 2F,
                MAX,
                (LO + OFFSET) + 2F,
                RebarUv.W3);
        box(
                full,
                rebar,
                MIN,
                (LO - OFFSET),
                (LO + OFFSET),
                MAX,
                (LO - OFFSET) + 2F,
                (LO + OFFSET) + 2F,
                RebarUv.W4);
        box(
                full,
                rebar,
                (LO - OFFSET),
                (LO + OFFSET),
                MIN,
                (LO - OFFSET) + 2F,
                (LO + OFFSET) + 2F,
                MAX,
                RebarUv.W5);

        box(
                full,
                rebar,
                (LO + OFFSET),
                MIN,
                (LO - OFFSET),
                (LO + OFFSET) + 2F,
                MAX,
                (LO - OFFSET) + 2F,
                RebarUv.W6);
        box(
                full,
                rebar,
                MIN,
                (LO + OFFSET),
                (LO - OFFSET),
                MAX,
                (LO + OFFSET) + 2F,
                (LO - OFFSET) + 2F,
                RebarUv.W7);
        box(
                full,
                rebar,
                (LO + OFFSET),
                (LO - OFFSET),
                MIN,
                (LO + OFFSET) + 2F,
                (LO - OFFSET) + 2F,
                MAX,
                RebarUv.W8);

        box(
                full,
                rebar,
                (LO + OFFSET),
                MIN,
                (LO + OFFSET),
                (LO + OFFSET) + 2F,
                MAX,
                (LO + OFFSET) + 2F,
                RebarUv.W9);
        box(
                full,
                rebar,
                MIN,
                (LO + OFFSET),
                (LO + OFFSET),
                MAX,
                (LO + OFFSET) + 2F,
                (LO + OFFSET) + 2F,
                RebarUv.W10);
        box(
                full,
                rebar,
                (LO + OFFSET),
                (LO + OFFSET),
                MIN,
                (LO + OFFSET) + 2F,
                (LO + OFFSET) + 2F,
                MAX,
                RebarUv.W11);

        this.rods = part(full);

        List<ObjUnbakedGeometry.BoxQuad> three = new ArrayList<>();
        box(three, rebar, LO, MIN, LO, LO + 2F, MAX, LO + 2F, RebarUv.W12);
        box(three, rebar, MIN, LO, LO, MAX, LO + 2F, LO + 2F, RebarUv.W13);
        box(three, rebar, LO, LO, MIN, LO + 2F, LO + 2F, MAX, RebarUv.W14);
        this.simple = part(three);
    }

    private BlockStateModelPart part(List<ObjUnbakedGeometry.BoxQuad> quads) {
        return new SimpleModelWrapper(
                ObjUnbakedGeometry.bakeGroups(List.of(), quads, 0F, 0F, 0F),
                base.getTopAmbientOcclusion(),
                particle);
    }

    private void box(
            List<ObjUnbakedGeometry.BoxQuad> b,
            Material.Baked material,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            RebarUv.Uv[] windows) {
        Vector3f from = new Vector3f(x0, y0, z0);
        Vector3f to = new Vector3f(x1, y1, z1);
        for (Direction dir : Direction.VALUES) {
            RebarUv.Uv uv = windows[dir.ordinal()];
            b.add(
                    new ObjUnbakedGeometry.BoxQuad(
                            Boxes.face(
                                    baker,
                                    from,
                                    to,
                                    dir,
                                    material,
                                    uv.uvs(),
                                    uv.quadrant(),
                                    CuboidFace.NO_TINT,
                                    Boxes.IDENTITY),
                            ObjUnbakedGeometry.WHITE,
                            null,
                            QuadLighting.cell(0, 0, 0)));
        }
    }

    private BlockStateModelPart rods() {
        return RenderConfig.simpleRebar ? simple : rods;
    }

    private BlockStateModelPart fill(int progress) {
        BlockStateModelPart part = fills[progress];
        if (part == null) {
            List<ObjUnbakedGeometry.BoxQuad> b = new ArrayList<>(6);
            box(
                    b,
                    concrete,
                    0F,
                    0F,
                    0F,
                    16F,
                    progress * 16F / BlockEntityRebar.FULL,
                    16F,
                    RebarUv.windows(0F, 0F, 0F, 16F, progress * 16F / BlockEntityRebar.FULL, 16F));
            part = part(b);
            fills[progress] = part;
        }
        return part;
    }

    private static int progress(BlockAndTintGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BlockEntityRebar rebar
                ? Math.clamp(rebar.progress(), 0, BlockEntityRebar.FULL)
                : 0;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        output.add(rods());
    }

    @Override
    public Material.Baked particleMaterial() {
        return particle;
    }

    @Override
    public int materialFlags() {
        return rods.materialFlags();
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<@Nullable Direction> cullTest) {
        List<BlockStateModelPart> parts = new ArrayList<>(2);
        collectParts(level, pos, state, random, parts);
        for (int i = 0; i < parts.size(); i++) parts.get(i).emitQuads(emitter, cullTest);
    }

    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> output) {
        output.add(rods());
        int progress = progress(level, pos);
        if (progress > 0) output.add(fill(progress));
    }

    @Override
    public Object createGeometryKey(
            BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return new GeometryKey(RenderConfig.simpleRebar, progress(level, pos));
    }

    private record GeometryKey(boolean simple, int progress) {}

    public record Family() implements SimpleBlockModel {

        @Override
        public BlockStateModel.UnbakedRoot root(Block block, BlockState state) {
            return new Root(BlockModel.base(block));
        }
    }

    public record Root(Identifier baseModel) implements BlockStateModel.UnbakedRoot {

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(baseModel);
        }

        @Override
        public BlockStateModel bake(BlockState blockState, ModelBaker baker) {
            return baker.compute(new SharedKey(this));
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return this;
        }
    }

    private record SharedKey(Root root) implements ModelBaker.SharedOperationKey<BlockStateModel> {

        @Override
        public BlockStateModel compute(ModelBaker baker) {
            return new RebarModel(baker, baker.getModel(root.baseModel()));
        }
    }
}
