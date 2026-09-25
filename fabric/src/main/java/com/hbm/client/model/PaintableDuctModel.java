// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.network.CablePaintableBlock;
import com.hbm.blocks.network.FluidDuctPaintableBlock;
import com.hbm.blocks.network.FluidDuctPaintableExhaustBlock;
import com.hbm.blocks.network.PaintableCamoBlock;
import com.hbm.blocks.network.pneumatic.PneumoTubePaintableBlock;
import com.hbm.tileentity.network.BlockEntityPipePaintable;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoTubePaintable;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public record PaintableDuctModel() implements SimpleBlockModel {

    @Override
    public BlockStateModel.UnbakedRoot root(Block block, BlockState state) {
        PaintableCamoBlock paintable = (PaintableCamoBlock) block;
        return new Root(
                BlockModel.base(block),
                Kind.of(block),
                state.getValue(paintable.paintedProperty()),
                state.getValue(paintable.overlayProperty()));
    }

    public enum Kind {
        DUCT("overlay", 0, "frame"),
        CABLE("overlay", CuboidFace.NO_TINT, "overlay"),
        EXHAUST(null, CuboidFace.NO_TINT, "frame"),
        TUBE(null, CuboidFace.NO_TINT, "frame");

        final @Nullable String overlaySlot;
        final int overlayTint;
        final String frameSlot;

        Kind(@Nullable String overlaySlot, int overlayTint, String frameSlot) {
            this.overlaySlot = overlaySlot;
            this.overlayTint = overlayTint;
            this.frameSlot = frameSlot;
        }

        static Kind of(Block block) {
            if (block instanceof FluidDuctPaintableExhaustBlock) return EXHAUST;
            if (block instanceof FluidDuctPaintableBlock) return DUCT;
            if (block instanceof CablePaintableBlock) return CABLE;
            if (block instanceof PneumoTubePaintableBlock) return TUBE;
            throw new IllegalArgumentException(block.toString());
        }
    }

    public record Root(Identifier carrier, Kind kind, boolean painted, boolean overlay)
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
            return Baked.FACTORY.create(baker, baker.getModel(root.carrier()), root);
        }
    }

    public static class Baked implements BlockStateModel {

        public static Factory FACTORY = Baked::new;
        private static final int NO_PORT = 6;

        private final ModelBaker baker;
        private final ResolvedModel carrier;
        private final TextureSlots slots;
        private final Material.Baked particle;
        private final Root root;
        private final SimpleModelWrapper plain;
        private final @Nullable SimpleModelWrapper frame;
        private final SimpleModelWrapper[] tubeFrames =
                new SimpleModelWrapper[(NO_PORT + 1) * (NO_PORT + 1)];

        public Baked(ModelBaker baker, ResolvedModel carrier, Root root) {
            this.baker = baker;
            this.carrier = carrier;
            this.slots = carrier.getTopTextureSlots();
            this.particle = carrier.resolveParticleMaterial(slots, baker);
            this.root = root;
            QuadCollection.Builder quads = new QuadCollection.Builder();
            cube(quads, "base", CuboidFace.NO_TINT);
            if (root.kind().overlaySlot != null)
                cube(quads, root.kind().overlaySlot, root.kind().overlayTint);
            this.plain = wrap(quads);
            this.frame =
                    root.kind() != Kind.TUBE && root.painted() && root.overlay()
                            ? wrap(
                                    cube(
                                            new QuadCollection.Builder(),
                                            root.kind().frameSlot,
                                            CuboidFace.NO_TINT))
                            : null;
        }

        private QuadCollection.Builder cube(QuadCollection.Builder quads, String slot, int tint) {
            Boxes.cube(quads, baker, BlockModel.slot(baker, carrier, slots, slot), tint);
            return quads;
        }

        private SimpleModelWrapper wrap(QuadCollection.Builder quads) {
            return new SimpleModelWrapper(
                    quads.build(), carrier.getTopAmbientOcclusion(), particle);
        }

        @Override
        public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
            output.add(plain);
        }

        @Override
        public Material.Baked particleMaterial() {
            return particle;
        }

        @Override
        public int materialFlags() {
            return plain.materialFlags() | (frame == null ? 0 : frame.materialFlags());
        }

        public void collectParts(
                BlockAndTintGetter level,
                BlockPos pos,
                BlockState state,
                RandomSource random,
                List<BlockStateModelPart> output) {
            visit(
                    level,
                    pos,
                    random,
                    new PartVisitor() {
                        @Override
                        public void own(BlockStateModelPart part) {
                            output.add(part);
                        }

                        @Override
                        public void camo(BlockStateModelPart part, BlockState camo) {
                            output.add(tinted(part, camo, level, pos));
                        }
                    });
        }

        protected void visit(
                BlockAndTintGetter level, BlockPos pos, RandomSource random, PartVisitor visitor) {
            BlockEntity be = level.getBlockEntity(pos);
            BlockState camo = root.painted() ? camo(be) : null;
            if (camo != null) {
                List<BlockStateModelPart> parts = new ArrayList<>();
                Minecraft.getInstance()
                        .getModelManager()
                        .getBlockStateModelSet()
                        .get(camo)
                        .collectParts(random, parts);
                for (BlockStateModelPart part : parts) visitor.camo(part, camo);
            } else {
                visitor.own(plain);
            }
            if (!root.overlay()) return;
            if (root.kind() == Kind.TUBE) {
                visitor.own(tubeFrame(be));
            } else if (camo != null && frame != null) {
                visitor.own(frame);
            }
        }

        public @Nullable Object geometryKey(
                BlockAndTintGetter level, BlockPos pos, BlockState state) {
            BlockEntity be = level.getBlockEntity(pos);
            if (root.painted() && camo(be) != null) return null;
            return new Key(this, root.kind() == Kind.TUBE && root.overlay() ? portIndex(be) : -1);
        }

        private static @Nullable BlockState camo(@Nullable BlockEntity be) {
            if (be instanceof BlockEntityPipePaintable pipe) return pipe.getCamo();
            if (be instanceof BlockEntityPneumoTubePaintable tube) return tube.getCamo();
            return null;
        }

        private static int portIndex(@Nullable BlockEntity be) {
            if (!(be instanceof BlockEntityPneumoTubePaintable tube))
                return NO_PORT * (NO_PORT + 1) + NO_PORT;
            return index(tube.ejectionDir) * (NO_PORT + 1) + index(tube.insertionDir);
        }

        private static int index(@Nullable Direction dir) {
            return dir == null ? NO_PORT : dir.ordinal();
        }

        private SimpleModelWrapper tubeFrame(@Nullable BlockEntity be) {
            int index = portIndex(be);
            synchronized (tubeFrames) {
                SimpleModelWrapper part = tubeFrames[index];
                if (part == null) {
                    int ejection = index / (NO_PORT + 1);
                    int insertion = index % (NO_PORT + 1);
                    QuadCollection.Builder quads = new QuadCollection.Builder();
                    for (Direction dir : Direction.VALUES) {

                        String slot =
                                dir.ordinal() == ejection
                                        ? "frame_in"
                                        : dir.ordinal() == insertion ? "frame_out" : "frame";
                        quads.addCulledFace(
                                dir,
                                Boxes.face(
                                        baker,
                                        Boxes.UNIT_FROM,
                                        Boxes.UNIT_TO,
                                        dir,
                                        BlockModel.slot(baker, carrier, slots, slot)));
                    }
                    part = wrap(quads);
                    tubeFrames[index] = part;
                }
                return part;
            }
        }

        public static int camoTint(
                BlockState camo, BlockAndTintGetter level, BlockPos pos, int tintIndex) {
            BlockTintSource source =
                    Minecraft.getInstance().getBlockColors().getTintSource(camo, tintIndex);
            return source == null ? -1 : source.colorInWorld(camo, level, pos);
        }

        protected BlockStateModelPart tinted(
                BlockStateModelPart part, BlockState camo, BlockAndTintGetter level, BlockPos pos) {
            return part;
        }

        public interface Factory {
            Baked create(ModelBaker baker, ResolvedModel carrier, Root root);
        }

        protected interface PartVisitor {
            void own(BlockStateModelPart part);

            void camo(BlockStateModelPart part, BlockState camo);
        }

        public record Key(Baked model, int ports) {}
    }
}
