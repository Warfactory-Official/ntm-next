// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.ModBlocks;
import com.hbm.lib.Library;
import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class HangingVineModel implements BlockStateModel {

    private static final String[] MODELS = {
        "vine_phosphor",
        "vine_phosphor_ground",
        "vine_phosphor_hang",
        "vine_phosphor_glow",
        "vine_phosphor_hang_glow"
    };
    public static Factory FACTORY = HangingVineModel::new;

    private final BlockStateModelPart[] parts = new BlockStateModelPart[MODELS.length];
    private final Material.Baked particle;

    public HangingVineModel(ModelBaker baker) {
        for (int i = 0; i < MODELS.length; i++) {
            Identifier id = model(MODELS[i]);
            ResolvedModel resolved = baker.getModel(id);
            Material.Baked material =
                    resolved.resolveParticleMaterial(resolved.getTopTextureSlots(), baker);
            parts[i] =
                    new SimpleModelWrapper(
                            resolved.bakeTopGeometry(
                                    resolved.getTopTextureSlots(), baker, Boxes.IDENTITY),
                            resolved.getTopAmbientOcclusion(),
                            material);
        }
        particle = parts[0].particleMaterial();
    }

    private static HangingVineModel create(ModelBaker baker) {
        return FACTORY.create(baker);
    }

    private static Identifier model(String name) {
        return Library.id("block/" + name);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        output.add(parts[0]);
        output.add(parts[3]);
    }

    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> output) {
        int key = geometryKey(level, pos, state);
        output.add(parts[key & 3]);
        output.add(parts[3 + (key >>> 2)]);
    }

    public int geometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        BlockPos below = pos.below();
        BlockState lower = level.getBlockState(below);
        int body;
        if (lower.isFaceSturdy(level, below, Direction.UP)) body = 1;
        else if (lower.is(ModBlocks.VINE_PHOSPHOR.get())) body = 0;
        else body = 2;
        int glow = lower.is(Blocks.AIR) ? 1 : 0;
        return body | glow << 2;
    }

    @Override
    public Material.Baked particleMaterial() {
        return particle;
    }

    @Override
    public int materialFlags() {
        int flags = 0;
        for (BlockStateModelPart part : parts) flags |= part.materialFlags();
        return flags;
    }

    public interface Factory {
        HangingVineModel create(ModelBaker baker);
    }

    public record Family() implements SimpleBlockModel {
        @Override
        public BlockStateModel.UnbakedRoot root(Block block, BlockState state) {
            return new Root();
        }
    }

    public record Root() implements BlockStateModel.UnbakedRoot {
        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            for (String name : MODELS) resolver.markDependency(model(name));
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
