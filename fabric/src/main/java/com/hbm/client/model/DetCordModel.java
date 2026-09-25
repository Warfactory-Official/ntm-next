// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.bomb.IDetConnectible;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class DetCordModel implements BlockStateModel {

    public static Factory FACTORY = DetCordModel::new;

    private static final Identifier OBJ = Library.id("models/blocks/cable_neo.obj");
    private static final float OFFSET_Y = 0.5F;

    private static final int P_X = 32, N_X = 16, P_Y = 8, N_Y = 4, P_Z = 2, N_Z = 1;

    private static final String[][] PARTS_BY_MASK =
            BlockModel.partsByMask(DetCordModel::selectParts);

    private final ModelBaker baker;
    private final ResolvedModel base;
    private final TextureSlots slots;
    private final Material.Baked particle;
    private final HFRWavefrontObject obj;
    private final BlockStateModelPart[] cache = new BlockStateModelPart[64];
    private final BlockStateModelPart fallback;

    public DetCordModel(ModelBaker baker, ResolvedModel base) {
        this.baker = baker;
        this.base = base;
        this.slots = base.getTopTextureSlots();
        this.particle = base.resolveParticleMaterial(slots, baker);
        this.obj = Meshes.load(OBJ);
        this.fallback = buildPart(0);
    }

    private static String[] selectParts(int mask) {
        if (mask == (P_X | N_X) || mask == P_X || mask == N_X) return new String[] {"CX"};
        if (mask == (P_Y | N_Y) || mask == P_Y || mask == N_Y) return new String[] {"CY"};
        if (mask == (P_Z | N_Z) || mask == P_Z || mask == N_Z) return new String[] {"CZ"};

        String[] parts = new String[1 + Integer.bitCount(mask)];
        int i = 0;
        parts[i++] = "Core";
        if ((mask & P_X) != 0) parts[i++] = "posX";
        if ((mask & N_X) != 0) parts[i++] = "negX";
        if ((mask & P_Y) != 0) parts[i++] = "posY";
        if ((mask & N_Y) != 0) parts[i++] = "negY";

        if ((mask & N_Z) != 0) parts[i++] = "posZ";
        if ((mask & P_Z) != 0) parts[i++] = "negZ";
        return parts;
    }

    private static int maskAt(BlockAndTintGetter level, BlockPos pos) {
        int mask = 0;
        if (connects(level, pos, Direction.EAST)) mask |= P_X;
        if (connects(level, pos, Direction.WEST)) mask |= N_X;
        if (connects(level, pos, Direction.UP)) mask |= P_Y;
        if (connects(level, pos, Direction.DOWN)) mask |= N_Y;
        if (connects(level, pos, Direction.SOUTH)) mask |= P_Z;
        if (connects(level, pos, Direction.NORTH)) mask |= N_Z;
        return mask;
    }

    private static boolean connects(BlockAndTintGetter level, BlockPos pos, Direction dir) {
        return IDetConnectible.isConnectible(level, pos.relative(dir), dir.getOpposite());
    }

    private static DetCordModel create(ModelBaker baker, ResolvedModel base) {
        return FACTORY.create(baker, base);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        output.add(fallback);
    }

    @Override
    public Material.Baked particleMaterial() {
        return particle;
    }

    @Override
    public int materialFlags() {
        return fallback.materialFlags();
    }

    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> output) {
        output.add(partFor(maskAt(level, pos)));
    }

    public Object geometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return maskAt(level, pos);
    }

    private BlockStateModelPart partFor(int mask) {
        synchronized (cache) {
            BlockStateModelPart part = cache[mask];
            if (part == null) {
                part = buildPart(mask);
                cache[mask] = part;
            }
            return part;
        }
    }

    private BlockStateModelPart buildPart(int mask) {
        Material.Baked material =
                BlockModel.slot(baker, base, slots, ObjUnbakedGeometry.SINGLE_SLOT);
        return new SimpleModelWrapper(
                ObjUnbakedGeometry.bakeGroups(
                        obj,
                        PARTS_BY_MASK[mask],
                        material,
                        BlockModelRotation.IDENTITY,
                        0F,
                        OFFSET_Y,
                        0F,
                        false),
                base.getTopAmbientOcclusion(),
                particle);
    }

    public interface Factory {
        DetCordModel create(ModelBaker baker, ResolvedModel base);
    }

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
