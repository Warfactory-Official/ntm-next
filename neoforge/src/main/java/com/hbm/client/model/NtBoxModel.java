// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.generic.BlockWoodBarrier;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
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

public final class NtBoxModel implements BlockStateModel {

    private final Shared shared;
    private final Shape shape;
    private final BlockStateModelPart fallback;

    private NtBoxModel(Shared shared, Shape shape, BlockState state) {
        this.shared = shared;
        this.shape = shape;
        this.fallback = shared.part(shape, shape.key(state));
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        output.add(fallback);
    }

    @Override
    public Material.Baked particleMaterial() {
        return shared.particle;
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
        output.add(shared.part(shape, shape.key(level, pos, state)));
    }

    @Override
    public Object createGeometryKey(
            BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return new GeometryKey(shape, shape.key(level, pos, state));
    }

    private record GeometryKey(Shape shape, int key) {}

    @FunctionalInterface
    public interface BoxVisitor {
        void box(float x0, float y0, float z0, float x1, float y1, float z1);
    }

    public interface Shape {
        int key(BlockState state);

        int key(BlockAndTintGetter level, BlockPos pos, BlockState state);

        void boxes(int key, BoxVisitor out);
    }

    private static final int W = 1, E = 2, N = 4, S = 8, UP = 16;

    private static boolean same(
            BlockAndTintGetter level, BlockPos pos, BlockState state, Direction dir) {
        return level.getBlockState(pos.relative(dir)).is(state.getBlock());
    }

    public enum Shapes implements Shape {
        WOOD_BARRIER {
            @Override
            public int key(BlockState state) {
                return (BlockWoodBarrier.panel(state, Direction.WEST) ? W : 0)
                        | (BlockWoodBarrier.panel(state, Direction.EAST) ? E : 0)
                        | (BlockWoodBarrier.panel(state, Direction.NORTH) ? N : 0)
                        | (BlockWoodBarrier.panel(state, Direction.SOUTH) ? S : 0);
            }

            @Override
            public int key(BlockAndTintGetter level, BlockPos pos, BlockState state) {

                return key(state) | (level.getBlockState(pos.above()).isSolidRender() ? UP : 0);
            }

            @Override
            public void boxes(int key, BoxVisitor out) {
                boolean w = (key & W) != 0,
                        e = (key & E) != 0,
                        n = (key & N) != 0,
                        s = (key & S) != 0;
                float z0 = n ? 2 : 0, z1 = s ? 14 : 16, x0 = w ? 2 : 0, x1 = e ? 14 : 16;
                if (w) {
                    out.box(0, 0, 7, 2, 16, 9);
                    out.box(0, 1, z0, 1, 7, z1);
                    out.box(0, 9, z0, 1, 15, z1);
                }
                if (n) {
                    out.box(7, 0, 0, 9, 16, 2);
                    out.box(x0, 1, 0, x1, 7, 1);
                    out.box(x0, 9, 0, x1, 15, 1);
                }
                if (e) {
                    out.box(14, 0, 7, 16, 16, 9);
                    out.box(15, 1, z0, 16, 7, z1);
                    out.box(15, 9, z0, 16, 15, z1);
                }
                if (s) {
                    out.box(7, 0, 14, 9, 16, 16);
                    out.box(x0, 1, 15, x1, 7, 16);
                    out.box(x0, 9, 15, x1, 15, 16);
                }
                if ((key & UP) != 0) ceiling(out);
            }
        },

        WOOD_ROOF {
            @Override
            public int key(BlockState state) {
                return 0;
            }

            @Override
            public int key(BlockAndTintGetter level, BlockPos pos, BlockState state) {
                return (same(level, pos, state, Direction.WEST) ? W : 0)
                        | (same(level, pos, state, Direction.EAST) ? E : 0);
            }

            @Override
            public void boxes(int key, BoxVisitor out) {
                float x0 = (key & W) != 0 ? 0 : 1, x1 = (key & E) != 0 ? 16 : 15;
                out.box(0, 0, 0, 2, 2, 16);
                out.box(14, 0, 0, 16, 2, 16);
                out.box(x0, 2, 1, x1, 3, 7);
                out.box(x0, 2, 9, x1, 3, 15);
            }
        },

        WOOD_SCAFFOLD {
            @Override
            public int key(BlockState state) {
                return 0;
            }

            @Override
            public int key(BlockAndTintGetter level, BlockPos pos, BlockState state) {
                return (same(level, pos, state, Direction.WEST) ? W : 0)
                        | (same(level, pos, state, Direction.EAST) ? E : 0)
                        | (same(level, pos, state, Direction.NORTH) ? N : 0)
                        | (same(level, pos, state, Direction.SOUTH) ? S : 0)
                        | (same(level, pos, state, Direction.UP) ? UP : 0);
            }

            @Override
            public void boxes(int key, BoxVisitor out) {
                float top = (key & UP) != 0 ? 16 : 14;
                out.box(1, 0, 1, 3, top, 3);
                out.box(13, 0, 1, 15, top, 3);
                out.box(1, 0, 13, 3, top, 15);
                out.box(13, 0, 13, 15, top, 15);
                if ((key & W) == 0) out.box(0, 2, 0, 1, 6, 16);
                if ((key & E) == 0) out.box(15, 2, 0, 16, 6, 16);
                if ((key & N) == 0) out.box(0, 8, 0, 16, 12, 1);
                if ((key & S) == 0) out.box(0, 8, 15, 16, 12, 16);
                if ((key & UP) == 0) out.box(0, 14, 0, 16, 16, 16);
            }
        },

        WOOD_CEILING {
            @Override
            public int key(BlockState state) {
                return 0;
            }

            @Override
            public int key(BlockAndTintGetter level, BlockPos pos, BlockState state) {
                return 0;
            }

            @Override
            public void boxes(int key, BoxVisitor out) {
                ceiling(out);
            }
        };

        private static void ceiling(BoxVisitor out) {
            out.box(0, 14, 0, 2, 15, 16);
            out.box(14, 14, 0, 16, 15, 16);
            out.box(0, 15, 1, 16, 16, 7);
            out.box(0, 15, 9, 16, 16, 15);
        }
    }

    private static final class Shared {

        private final ModelBaker baker;
        private final Material.Baked sheet;
        private final Material.Baked particle;
        private final boolean ambientOcclusion;
        private final Int2ObjectMap<BlockStateModelPart> parts = new Int2ObjectOpenHashMap<>();

        private Shared(ModelBaker baker, ResolvedModel carrier) {
            this.baker = baker;
            TextureSlots slots = carrier.getTopTextureSlots();
            this.sheet = BlockModel.slot(baker, carrier, slots, "texture");
            this.particle = carrier.resolveParticleMaterial(slots, baker);
            this.ambientOcclusion = carrier.getTopAmbientOcclusion();
        }

        private BlockStateModelPart part(Shape shape, int key) {
            synchronized (parts) {
                BlockStateModelPart part = parts.get(key);
                if (part == null) {
                    QuadCollection.Builder b = new QuadCollection.Builder();
                    shape.boxes(
                            key,
                            (x0, y0, z0, x1, y1, z1) -> {
                                Vector3f from = new Vector3f(x0, y0, z0);
                                Vector3f to = new Vector3f(x1, y1, z1);
                                for (Direction dir : Direction.VALUES) {
                                    NtBoxUv.Uv uv =
                                            NtBoxUv.window(
                                                    dir, from.x(), from.y(), from.z(), to.x(),
                                                    to.y(), to.z());
                                    b.addUnculledFace(
                                            Boxes.face(
                                                    baker,
                                                    from,
                                                    to,
                                                    dir,
                                                    sheet,
                                                    uv.uvs(),
                                                    uv.quadrant(),
                                                    CuboidFace.NO_TINT,
                                                    Boxes.IDENTITY));
                                }
                            });
                    part = new SimpleModelWrapper(b.build(), ambientOcclusion, particle);
                    parts.put(key, part);
                }
                return part;
            }
        }
    }

    public record Family(Shapes shape) implements SimpleBlockModel {

        @Override
        public BlockStateModel.UnbakedRoot root(Block block, BlockState state) {
            return new Root(BlockModel.base(block), shape);
        }
    }

    public record Root(Identifier carrier, Shapes shape) implements BlockStateModel.UnbakedRoot {

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(carrier);
        }

        @Override
        public BlockStateModel bake(BlockState blockState, ModelBaker baker) {
            return new NtBoxModel(baker.compute(new SharedKey(this)), shape, blockState);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return new GeometryKey(shape, shape.key(blockState));
        }
    }

    private record SharedKey(Root root) implements ModelBaker.SharedOperationKey<Shared> {

        @Override
        public Shared compute(ModelBaker baker) {
            return new Shared(baker, baker.getModel(root.carrier()));
        }
    }
}
