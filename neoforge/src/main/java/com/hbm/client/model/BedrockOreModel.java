// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.tileentity.BlockEntityBedrockOre;
import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BedrockOreModel implements BlockStateModel {

    public static final int SHAPES = 10;
    public static final int OVERLAY_TINT = 0;
    public static Factory FACTORY = BedrockOreModel::new;

    private final ModelBaker baker;
    private final ResolvedModel base;
    private final TextureSlots slots;
    private final Material.Baked particle;
    private final BlockStateModelPart[] cache = new BlockStateModelPart[SHAPES];
    private final BlockStateModelPart bare;

    public BedrockOreModel(ModelBaker baker, ResolvedModel base) {
        this.baker = baker;
        this.base = base;
        this.slots = base.getTopTextureSlots();
        this.particle = base.resolveParticleMaterial(slots, baker);
        this.bare = buildPart(-1);
    }

    private static BedrockOreModel create(ModelBaker baker, ResolvedModel base) {
        return FACTORY.create(baker, base);
    }

    private static int shapeAt(BlockAndTintGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BlockEntityBedrockOre ore
                ? Mth.positiveModulo(ore.shape, SHAPES)
                : -1;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        output.add(bare);
    }

    @Override
    public Material.Baked particleMaterial() {
        return particle;
    }

    @Override
    public int materialFlags() {
        return bare.materialFlags();
    }

    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> output) {
        int shape = shapeAt(level, pos);
        output.add(shape < 0 ? bare : partFor(shape));
    }

    public Object geometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return new Key(shapeAt(level, pos));
    }

    private BlockStateModelPart partFor(int shape) {
        synchronized (cache) {
            BlockStateModelPart part = cache[shape];
            if (part == null) {
                part = buildPart(shape);
                cache[shape] = part;
            }
            return part;
        }
    }

    private BlockStateModelPart buildPart(int shape) {
        QuadCollection.Builder quads = new QuadCollection.Builder();
        Boxes.cube(quads, baker, BlockModel.slot(baker, base, slots, "all"), CuboidFace.NO_TINT);
        if (shape >= 0) {
            Boxes.cube(
                    quads,
                    baker,
                    BlockModel.slot(baker, base, slots, "overlay" + shape),
                    OVERLAY_TINT);
        }
        return new SimpleModelWrapper(quads.build(), base.getTopAmbientOcclusion(), particle);
    }

    public interface Factory {
        BedrockOreModel create(ModelBaker baker, ResolvedModel base);
    }

    public record Key(int shape) {}

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
            return create(baker, baker.getModel(root.baseModel()));
        }
    }
}
