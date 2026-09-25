// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.network.pneumatic.PneumoTubeBlock;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoTube;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public class PneumoTubeModel implements BlockStateModel {

    private static final Direction[] DIRS = Direction.VALUES;

    private static final float LOWER = 5F;
    private static final float UPPER = 11F;
    private static final float C_LOWER = 4F;
    private static final float C_UPPER = 12F;

    private static final int NOZZLE_NONE = 0;
    private static final int NOZZLE_IN = 1;
    private static final int NOZZLE_OUT = 2;
    private static final int NOZZLE_AIR = 3;
    public static Factory FACTORY = PneumoTubeModel::new;
    private final ModelBaker baker;
    private final ResolvedModel base;
    private final TextureSlots slots;
    private final Material.Baked particle;
    private final BlockStateModelPart fallback;
    private final Int2ObjectMap<BlockStateModelPart> cache = new Int2ObjectOpenHashMap<>();

    public PneumoTubeModel(ModelBaker baker, ResolvedModel base) {
        this.baker = baker;
        this.base = base;
        this.slots = base.getTopTextureSlots();
        this.particle = base.resolveParticleMaterial(slots, baker);
        this.fallback = buildPart(0, 0, null, null);
    }

    private static int key(int arms, int air, @Nullable Direction in, @Nullable Direction out) {
        return arms
                | (air << 6)
                | (BlockEntityPneumoTube.dirIndex(in) << 12)
                | (BlockEntityPneumoTube.dirIndex(out) << 15);
    }

    private static boolean has(int mask, Direction dir) {
        return (mask & (1 << dir.ordinal())) != 0;
    }

    private static int hidden(Direction a, Direction b) {
        return (1 << a.ordinal()) | (1 << b.ordinal());
    }

    private static PneumoTubeModel create(ModelBaker baker, ResolvedModel base) {
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
        if (!(state.getBlock() instanceof PneumoTubeBlock)) {
            output.add(fallback);
            return;
        }
        output.add(partFor((int) geometryKey(level, pos, state)));
    }

    public Object geometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof PneumoTubeBlock)) return 0;
        int faces = PneumoTubeBlock.faceMask(state);
        int arms = 0;
        for (Direction dir : DIRS) {
            if (has(faces, dir) && PneumoTubeBlock.canConnectTo(level, pos, dir))
                arms |= 1 << dir.ordinal();
        }
        Direction in = null;
        Direction out = null;
        int air = 0;
        if (level.getBlockEntity(pos) instanceof BlockEntityPneumoTube tube) {
            in = tube.insertionDir;
            out = tube.ejectionDir;
            air = tube.airMask & faces;
        }
        return key(arms, air, in, out);
    }

    private BlockStateModelPart partFor(int k) {
        synchronized (cache) {
            BlockStateModelPart part = cache.get(k);
            if (part == null) {
                part =
                        buildPart(
                                k & 63,
                                (k >> 6) & 63,
                                BlockEntityPneumoTube.dirFromIndex((k >> 12) & 7),
                                BlockEntityPneumoTube.dirFromIndex((k >> 15) & 7));
                cache.put(k, part);
            }
            return part;
        }
    }

    private BlockStateModelPart buildPart(
            int arms, int air, @Nullable Direction in, @Nullable Direction out) {
        QuadCollection.Builder b = new QuadCollection.Builder();

        Material.Baked baseTex = BlockModel.slot(baker, base, slots, "base");
        Material.Baked straight = BlockModel.slot(baker, base, slots, "straight");

        boolean pX = has(arms, Direction.EAST), nX = has(arms, Direction.WEST);
        boolean pY = has(arms, Direction.UP), nY = has(arms, Direction.DOWN);
        boolean pZ = has(arms, Direction.SOUTH), nZ = has(arms, Direction.NORTH);

        int legacyMask =
                (pX ? 32 : 0)
                        | (nX ? 16 : 0)
                        | (pY ? 8 : 0)
                        | (nY ? 4 : 0)
                        | (pZ ? 2 : 0)
                        | (nZ ? 1 : 0);
        boolean hasConnections = in != null || out != null;

        if (legacyMask == 0b110000 && !hasConnections) {
            box(
                    b,
                    straight,
                    PneumoTubeUv.STRAIGHT_X,
                    hidden(Direction.WEST, Direction.EAST),
                    0,
                    LOWER,
                    LOWER,
                    16,
                    UPPER,
                    UPPER);

        } else if (legacyMask == 0b000011 && !hasConnections) {
            box(
                    b,
                    straight,
                    PneumoTubeUv.STRAIGHT_Z,
                    hidden(Direction.NORTH, Direction.SOUTH),
                    LOWER,
                    LOWER,
                    0,
                    UPPER,
                    UPPER,
                    16);

        } else if (legacyMask == 0b001100 && !hasConnections) {
            box(
                    b,
                    straight,
                    PneumoTubeUv.STRAIGHT_Y,
                    hidden(Direction.DOWN, Direction.UP),
                    LOWER,
                    0,
                    LOWER,
                    UPPER,
                    16,
                    UPPER);

        } else {
            int core = 0;
            for (Direction dir : DIRS) if (has(arms, dir)) core |= 1 << dir.ordinal();
            box(b, baseTex, PneumoTubeUv.CORE, core, LOWER, LOWER, LOWER, UPPER, UPPER, UPPER);
            for (Direction dir : DIRS) if (has(arms, dir)) arm(b, baseTex, dir);
        }

        if (in != null) nozzle(b, in, NOZZLE_IN);
        if (out != null) nozzle(b, out, NOZZLE_OUT);
        for (Direction dir : DIRS) if (has(air, dir)) nozzle(b, dir, NOZZLE_AIR);

        return new SimpleModelWrapper(b.build(), base.getTopAmbientOcclusion(), particle);
    }

    private void nozzle(QuadCollection.Builder b, Direction dir, int type) {
        Material.Baked baseTex = BlockModel.slot(baker, base, slots, "base");
        Material.Baked head =
                BlockModel.slot(
                        baker,
                        base,
                        slots,
                        type == NOZZLE_IN ? "in" : type == NOZZLE_OUT ? "out" : "connector");
        int caps = hidden(dir, dir.getOpposite());
        switch (dir) {
            case EAST -> {
                box(
                        b,
                        baseTex,
                        PneumoTubeUv.NOZZLE_EAST_BODY,
                        caps,
                        UPPER,
                        LOWER,
                        LOWER,
                        C_UPPER,
                        UPPER,
                        UPPER);
                box(
                        b,
                        head,
                        PneumoTubeUv.NOZZLE_EAST_HEAD,
                        0,
                        C_UPPER,
                        C_LOWER,
                        C_LOWER,
                        16,
                        C_UPPER,
                        C_UPPER);
            }
            case WEST -> {
                box(
                        b,
                        baseTex,
                        PneumoTubeUv.NOZZLE_WEST_BODY,
                        caps,
                        C_LOWER,
                        LOWER,
                        LOWER,
                        LOWER,
                        UPPER,
                        UPPER);
                box(
                        b,
                        head,
                        PneumoTubeUv.NOZZLE_WEST_HEAD,
                        0,
                        0,
                        C_LOWER,
                        C_LOWER,
                        C_LOWER,
                        C_UPPER,
                        C_UPPER);
            }
            case UP -> {
                box(
                        b,
                        baseTex,
                        PneumoTubeUv.NOZZLE_UP_BODY,
                        caps,
                        LOWER,
                        UPPER,
                        LOWER,
                        UPPER,
                        C_UPPER,
                        UPPER);
                box(
                        b,
                        head,
                        PneumoTubeUv.NOZZLE_UP_HEAD,
                        0,
                        C_LOWER,
                        C_UPPER,
                        C_LOWER,
                        C_UPPER,
                        16,
                        C_UPPER);
            }
            case DOWN -> {
                box(
                        b,
                        baseTex,
                        PneumoTubeUv.NOZZLE_DOWN_BODY,
                        caps,
                        LOWER,
                        C_LOWER,
                        LOWER,
                        UPPER,
                        LOWER,
                        UPPER);
                box(
                        b,
                        head,
                        PneumoTubeUv.NOZZLE_DOWN_HEAD,
                        0,
                        C_LOWER,
                        0,
                        C_LOWER,
                        C_UPPER,
                        C_LOWER,
                        C_UPPER);
            }
            case SOUTH -> {
                box(
                        b,
                        baseTex,
                        PneumoTubeUv.NOZZLE_SOUTH_BODY,
                        caps,
                        LOWER,
                        LOWER,
                        UPPER,
                        UPPER,
                        UPPER,
                        C_UPPER);
                box(
                        b,
                        head,
                        PneumoTubeUv.NOZZLE_SOUTH_HEAD,
                        0,
                        C_LOWER,
                        C_LOWER,
                        C_UPPER,
                        C_UPPER,
                        C_UPPER,
                        16);
            }
            case NORTH -> {
                box(
                        b,
                        baseTex,
                        PneumoTubeUv.NOZZLE_NORTH_BODY,
                        caps,
                        LOWER,
                        LOWER,
                        C_LOWER,
                        UPPER,
                        UPPER,
                        LOWER);
                box(
                        b,
                        head,
                        PneumoTubeUv.NOZZLE_NORTH_HEAD,
                        0,
                        C_LOWER,
                        C_LOWER,
                        0,
                        C_UPPER,
                        C_UPPER,
                        C_LOWER);
            }
        }
    }

    private void arm(QuadCollection.Builder b, Material.Baked tex, Direction dir) {
        int caps = hidden(dir, dir.getOpposite());
        switch (dir) {
            case EAST ->
                    box(b, tex, PneumoTubeUv.ARM_EAST, caps, UPPER, LOWER, LOWER, 16, UPPER, UPPER);
            case WEST ->
                    box(b, tex, PneumoTubeUv.ARM_WEST, caps, 0, LOWER, LOWER, LOWER, UPPER, UPPER);
            case UP ->
                    box(b, tex, PneumoTubeUv.ARM_UP, caps, LOWER, UPPER, LOWER, UPPER, 16, UPPER);
            case DOWN ->
                    box(b, tex, PneumoTubeUv.ARM_DOWN, caps, LOWER, 0, LOWER, UPPER, LOWER, UPPER);
            case SOUTH ->
                    box(
                            b,
                            tex,
                            PneumoTubeUv.ARM_SOUTH,
                            caps,
                            LOWER,
                            LOWER,
                            UPPER,
                            UPPER,
                            UPPER,
                            16);
            case NORTH ->
                    box(b, tex, PneumoTubeUv.ARM_NORTH, caps, LOWER, LOWER, 0, UPPER, UPPER, LOWER);
        }
    }

    private void box(
            QuadCollection.Builder b,
            Material.Baked material,
            PneumoTubeUv.Uv[] windows,
            int hiddenMask,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1) {
        Vector3f from = new Vector3f(x0, y0, z0);
        Vector3f to = new Vector3f(x1, y1, z1);
        for (Direction dir : DIRS) {
            if ((hiddenMask & (1 << dir.ordinal())) != 0) continue;
            PneumoTubeUv.Uv uv = windows[dir.ordinal()];
            b.addUnculledFace(
                    Boxes.face(
                            baker,
                            from,
                            to,
                            dir,
                            material,
                            uv.uvs(),
                            uv.quadrant(),
                            CuboidFace.NO_TINT,
                            Boxes.IDENTITY));
        }
    }

    public interface Factory {
        PneumoTubeModel create(ModelBaker baker, ResolvedModel base);
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
