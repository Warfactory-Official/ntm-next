// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.math.OctahedralGroup;
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
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class FluoroModel implements BlockStateModel {

    public static Factory FACTORY = FluoroModel::new;

    private static final Identifier OBJ = Library.id("models/lights/fluorescent_lamp.obj");

    private static final String[] PARTS = {"FluoroSingle", "FluoroCap", "FluoroMid"};

    private final ModelBaker baker;
    private final ResolvedModel base;
    private final TextureSlots slots;
    private final Material.Baked particle;
    private final HFRWavefrontObject obj;
    private final Direction facing;
    private final BlockStateModelPart[] cache =
            new BlockStateModelPart[PARTS.length * Direction.values().length];
    private final BlockStateModelPart fallback;

    public FluoroModel(ModelBaker baker, ResolvedModel base, Direction facing) {
        this.baker = baker;
        this.base = base;
        this.slots = base.getTopTextureSlots();
        this.particle = base.resolveParticleMaterial(slots, baker);
        this.obj = Meshes.faceNormals(OBJ);
        this.facing = facing;
        this.fallback = buildPart(0);
    }

    private static int keyAt(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(DirectionalBlock.FACING);
        for (Direction conn : Direction.values()) {
            if (conn.getAxis() == facing.getAxis()
                    || !level.getBlockState(pos.relative(conn)).is(state.getBlock())) {
                continue;
            }
            int count =
                    level.getBlockState(pos.relative(conn.getOpposite())).is(state.getBlock())
                            ? 2
                            : 1;
            return count * Direction.values().length + conn.get3DDataValue();
        }
        return 0;
    }

    private static int rollQuarters(Direction conn, Direction axis) {
        int flipX =
                axis == Direction.DOWN || axis == Direction.NORTH || axis == Direction.WEST
                        ? -1
                        : 1;
        int addX = axis == Direction.NORTH || axis == Direction.SOUTH ? -1 : 0;
        boolean flipNS = axis == Direction.WEST;
        return switch (conn) {
            case NORTH -> flipNS ? 2 : 0;
            case SOUTH -> !flipNS ? 2 : 0;
            case EAST -> flipX + addX;
            case WEST -> -flipX + addX;
            case UP -> -1;
            case DOWN -> 1;
        };
    }

    private static OctahedralGroup roll(int quarters) {
        return switch (Math.floorMod(quarters, 4)) {
            case 1 -> OctahedralGroup.BLOCK_ROT_X_90;
            case 2 -> OctahedralGroup.BLOCK_ROT_X_180;
            case 3 -> OctahedralGroup.BLOCK_ROT_X_270;
            default -> OctahedralGroup.IDENTITY;
        };
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
        output.add(partFor(keyAt(level, pos, state)));
    }

    public Object geometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return keyAt(level, pos, state);
    }

    private BlockStateModelPart partFor(int key) {
        synchronized (cache) {
            BlockStateModelPart part = cache[key];
            if (part == null) {
                part = buildPart(key);
                cache[key] = part;
            }
            return part;
        }
    }

    private BlockStateModelPart buildPart(int key) {
        int count = key / Direction.values().length;
        OctahedralGroup rotation = SpotlightModel.rotationFor(facing);
        if (count > 0) {
            rotation =
                    rotation.compose(
                            roll(
                                    rollQuarters(
                                            Direction.from3DDataValue(
                                                    key % Direction.values().length),
                                            facing)));
        }
        Material.Baked material =
                BlockModel.slot(baker, base, slots, ObjUnbakedGeometry.SINGLE_SLOT);
        return new SimpleModelWrapper(
                ObjUnbakedGeometry.bakeGroups(
                        obj,
                        new String[] {PARTS[count]},
                        material,
                        BlockModelRotation.get(rotation),
                        SpotlightModel.OFFSET_X,
                        SpotlightModel.OFFSET_Y,
                        SpotlightModel.OFFSET_Z,
                        false,
                        false),
                base.getTopAmbientOcclusion(),
                particle);
    }

    public interface Factory {
        FluoroModel create(ModelBaker baker, ResolvedModel base, Direction facing);
    }

    public record Root(Identifier carrier, Direction facing)
            implements BlockStateModel.UnbakedRoot {

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(carrier);
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
            return FACTORY.create(baker, baker.getModel(root.carrier()), root.facing());
        }
    }
}
