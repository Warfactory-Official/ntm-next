// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.generic.BlockReeds;
import com.hbm.lib.Library;
import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class ReedsModel implements BlockStateModel {

    private static final String[] SHEETS = {"top", "mid", "bottom"};

    public static Factory FACTORY = ReedsModel::new;

    private final ModelBaker baker;
    private final ResolvedModel[] crosses = new ResolvedModel[SHEETS.length];
    private final Material.Baked particle;
    private final Map<Integer, BlockStateModelPart> byDepth = new ConcurrentHashMap<>();
    private final BlockStateModelPart lone;

    public ReedsModel(ModelBaker baker) {
        this.baker = baker;
        for (int i = 0; i < SHEETS.length; i++) crosses[i] = baker.getModel(cross(SHEETS[i]));
        this.particle = crosses[0].resolveParticleMaterial(crosses[0].getTopTextureSlots(), baker);
        this.lone = buildPart(1);
    }

    private static ReedsModel create(ModelBaker baker) {
        return FACTORY.create(baker);
    }

    private static Identifier cross(String sheet) {
        return Library.id("block/plant_reeds_" + sheet);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        output.add(lone);
    }

    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> output) {
        output.add(byDepth.computeIfAbsent(BlockReeds.depth(level, pos), this::buildPart));
    }

    public Object geometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return new Key(BlockReeds.depth(level, pos));
    }

    @Override
    public Material.Baked particleMaterial() {
        return particle;
    }

    @Override
    public int materialFlags() {
        return lone.materialFlags();
    }

    private BlockStateModelPart buildPart(int depth) {
        List<ObjUnbakedGeometry.BoxQuad> quads = new ArrayList<>();
        for (int level = 0; level < depth; level++) {
            ResolvedModel sheet = crosses[level == 0 ? 0 : level == depth - 1 ? 2 : 1];

            var posed =
                    Boxes.pose(new Transformation(new Vector3f(0F, -level, 0F), null, null, null));
            for (var quad :
                    sheet.bakeTopGeometry(sheet.getTopTextureSlots(), baker, posed).getAll()) {
                quads.add(
                        new ObjUnbakedGeometry.BoxQuad(
                                quad,
                                ObjUnbakedGeometry.WHITE,
                                null,
                                QuadLighting.cell(0, -level, 0)));
            }
        }
        return new SimpleModelWrapper(
                ObjUnbakedGeometry.bakeGroups(List.of(), quads, 0F, 0F, 0F),
                crosses[0].getTopAmbientOcclusion(),
                particle);
    }

    public interface Factory {
        ReedsModel create(ModelBaker baker);
    }

    public record Key(int depth) {}

    public record Family() implements SimpleBlockModel {

        @Override
        public BlockStateModel.UnbakedRoot root(Block block, BlockState state) {
            return new Root();
        }
    }

    public record Root() implements BlockStateModel.UnbakedRoot {

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            for (String sheet : SHEETS) resolver.markDependency(cross(sheet));
        }

        @Override
        public BlockStateModel bake(BlockState state, ModelBaker baker) {
            return baker.compute(new SharedKey(this));
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return this;
        }
    }

    private record SharedKey(Root root) implements ModelBaker.SharedOperationKey<BlockStateModel> {

        @Override
        public BlockStateModel compute(ModelBaker baker) {
            return create(baker);
        }
    }
}
