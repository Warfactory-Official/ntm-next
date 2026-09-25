// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.network.ConveyorBlockBase;
import com.hbm.blocks.network.ConveyorChuteBlock;
import com.hbm.blocks.network.ConveyorNeighbors;
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
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class ConveyorChuteModel implements BlockStateModel {

    private static final float INNER_MIN = 4F;
    private static final float INNER_MAX = 12F;
    private static final float GLASS_MIN = 2F;
    private static final float GLASS_MAX = 14F;
    private static final float LIP = 4F;

    private static final String GRATE = "grate";

    private static final Direction[] X_FACES = {Direction.WEST, Direction.EAST};
    private static final Direction[] Z_FACES = {Direction.NORTH, Direction.SOUTH};

    public static Factory FACTORY = ConveyorChuteModel::new;

    private final ModelBaker baker;
    private final ResolvedModel base;
    private final TextureSlots slots;
    private final Material.Baked particle;
    private final Int2ObjectMap<BlockStateModelPart> cache = new Int2ObjectOpenHashMap<>();
    private final BlockStateModelPart fallback;

    public ConveyorChuteModel(ModelBaker baker, ResolvedModel base) {
        this.baker = baker;
        this.base = base;
        this.slots = base.getTopTextureSlots();
        this.particle = base.resolveParticleMaterial(slots, baker);
        this.fallback = buildPart(Direction.NORTH, true, 0);
    }

    private static ConveyorChuteModel create(ModelBaker baker, ResolvedModel base) {
        return FACTORY.create(baker, base);
    }

    private static int key(Direction facing, boolean lowest, int belts) {
        return facing.ordinal() | (lowest ? 1 << 3 : 0) | (belts << 4);
    }

    private static int beltMask(BlockAndTintGetter level, BlockPos pos) {
        int mask = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (ConveyorNeighbors.beltAt(level, pos.relative(dir))) {
                mask |= 1 << dir.ordinal();
            }
        }
        return mask;
    }

    private static boolean beltAt(int mask, Direction dir) {
        return (mask & (1 << dir.ordinal())) != 0;
    }

    private static String beltSlot(Direction facing, Direction face) {
        boolean alongZ = facing.getAxis() == Direction.Axis.Z;
        boolean sideFace =
                alongZ
                        ? face == Direction.WEST || face == Direction.EAST
                        : face == Direction.NORTH || face == Direction.SOUTH;
        return sideFace ? ConveyorModel.SIDE : ConveyorModel.BELT;
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
        if (!(state.getBlock() instanceof ConveyorChuteBlock)) {
            output.add(fallback);
            return;
        }
        output.add(
                partFor(
                        state.getValue(ConveyorBlockBase.FACING),
                        !state.getValue(ConveyorChuteBlock.FEEDS_DOWN),
                        beltMask(level, pos)));
    }

    public Object geometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof ConveyorChuteBlock)) return 0;
        return key(
                state.getValue(ConveyorBlockBase.FACING),
                !state.getValue(ConveyorChuteBlock.FEEDS_DOWN),
                beltMask(level, pos));
    }

    private BlockStateModelPart partFor(Direction facing, boolean lowest, int belts) {
        int k = key(facing, lowest, belts);
        synchronized (cache) {
            BlockStateModelPart part = cache.get(k);
            if (part == null) {
                part = buildPart(facing, lowest, belts);
                cache.put(k, part);
            }
            return part;
        }
    }

    private BlockStateModelPart buildPart(Direction facing, boolean lowest, int belts) {
        List<ObjUnbakedGeometry.BoxQuad> b = new ArrayList<>();

        if (lowest) {
            ConveyorUv.Uv[][] belt =
                    switch (facing) {
                        case SOUTH -> ConveyorChuteUv.BELT_SOUTH;
                        case EAST -> ConveyorChuteUv.BELT_EAST;
                        case WEST -> ConveyorChuteUv.BELT_WEST;
                        default -> ConveyorChuteUv.BELT_NORTH;
                    };
            box(b, face -> beltSlot(facing, face), belt[0], INNER_MIN, 0F, 0F, INNER_MAX, LIP, 16F);
            box(
                    b,
                    face -> beltSlot(facing, face),
                    belt[1],
                    0F,
                    0F,
                    INNER_MIN,
                    INNER_MIN,
                    LIP,
                    INNER_MAX);
            box(
                    b,
                    face -> beltSlot(facing, face),
                    belt[2],
                    INNER_MAX,
                    0F,
                    INNER_MIN,
                    16F,
                    LIP,
                    INNER_MAX);
        } else {

            stub(
                    b,
                    facing,
                    belts,
                    Direction.WEST,
                    ConveyorChuteUv.STUB_WEST,
                    0F,
                    INNER_MIN,
                    GLASS_MIN,
                    INNER_MAX);
            stub(
                    b,
                    facing,
                    belts,
                    Direction.EAST,
                    ConveyorChuteUv.STUB_EAST,
                    GLASS_MAX,
                    INNER_MIN,
                    16F,
                    INNER_MAX);
            stub(
                    b,
                    facing,
                    belts,
                    Direction.NORTH,
                    ConveyorChuteUv.STUB_NORTH,
                    INNER_MIN,
                    0F,
                    INNER_MAX,
                    GLASS_MIN);
            stub(
                    b,
                    facing,
                    belts,
                    Direction.SOUTH,
                    ConveyorChuteUv.STUB_SOUTH,
                    INNER_MIN,
                    GLASS_MAX,
                    INNER_MAX,
                    16F);
        }

        box(
                b,
                face -> ConveyorModel.CONCRETE,
                ConveyorChuteUv.POST_MIN_MIN,
                0F,
                0F,
                0F,
                INNER_MIN,
                16F,
                INNER_MIN);
        box(
                b,
                face -> ConveyorModel.CONCRETE,
                ConveyorChuteUv.POST_MAX_MIN,
                INNER_MAX,
                0F,
                0F,
                16F,
                16F,
                INNER_MIN);
        box(
                b,
                face -> ConveyorModel.CONCRETE,
                ConveyorChuteUv.POST_MIN_MAX,
                0F,
                0F,
                INNER_MAX,
                INNER_MIN,
                16F,
                16F);
        box(
                b,
                face -> ConveyorModel.CONCRETE,
                ConveyorChuteUv.POST_MAX_MAX,
                INNER_MAX,
                0F,
                INNER_MAX,
                16F,
                16F,
                16F);

        float floor = lowest ? LIP : 0F;
        pane(
                b,
                facing,
                belts,
                lowest,
                Direction.WEST,
                lowest ? ConveyorChuteUv.PANE_WEST_LIP : ConveyorChuteUv.PANE_WEST_BASE,
                GLASS_MIN,
                floor,
                INNER_MIN,
                GLASS_MIN,
                16F,
                INNER_MAX);
        pane(
                b,
                facing,
                belts,
                lowest,
                Direction.EAST,
                lowest ? ConveyorChuteUv.PANE_EAST_LIP : ConveyorChuteUv.PANE_EAST_BASE,
                GLASS_MAX,
                floor,
                INNER_MIN,
                GLASS_MAX,
                16F,
                INNER_MAX);
        pane(
                b,
                facing,
                belts,
                lowest,
                Direction.NORTH,
                lowest ? ConveyorChuteUv.PANE_NORTH_LIP : ConveyorChuteUv.PANE_NORTH_BASE,
                INNER_MIN,
                floor,
                GLASS_MIN,
                INNER_MAX,
                16F,
                GLASS_MIN);
        pane(
                b,
                facing,
                belts,
                lowest,
                Direction.SOUTH,
                lowest ? ConveyorChuteUv.PANE_SOUTH_LIP : ConveyorChuteUv.PANE_SOUTH_BASE,
                INNER_MIN,
                floor,
                GLASS_MAX,
                INNER_MAX,
                16F,
                GLASS_MAX);

        return new SimpleModelWrapper(
                ObjUnbakedGeometry.bakeGroups(List.of(), b, 0F, 0F, 0F),
                base.getTopAmbientOcclusion(),
                particle);
    }

    private void stub(
            List<ObjUnbakedGeometry.BoxQuad> b,
            Direction facing,
            int belts,
            Direction side,
            ConveyorUv.Uv[] windows,
            float x0,
            float z0,
            float x1,
            float z1) {
        if (!beltAt(belts, side)) return;
        box(b, face -> beltSlot(facing, face), windows, x0, 0F, z0, x1, LIP, z1);
    }

    private void pane(
            List<ObjUnbakedGeometry.BoxQuad> b,
            Direction facing,
            int belts,
            boolean lowest,
            Direction side,
            ConveyorUv.Uv[] windows,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1) {
        if (beltAt(belts, side)) return;
        if (lowest && side == facing) return;

        Vector3f from = new Vector3f(x0, y0, z0);
        Vector3f to = new Vector3f(x1, y1, z1);

        Material.Baked material = BlockModel.slot(baker, base, slots, GRATE);

        for (Direction face : side.getAxis() == Direction.Axis.X ? X_FACES : Z_FACES) {
            ConveyorUv.Uv uv = windows[face.ordinal()];
            b.add(
                    new ObjUnbakedGeometry.BoxQuad(
                            Boxes.face(
                                    baker,
                                    from,
                                    to,
                                    face,
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

    private void box(
            List<ObjUnbakedGeometry.BoxQuad> b,
            ConveyorModel.SlotBySide slotBySide,
            ConveyorUv.Uv[] windows,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1) {
        ConveyorModel.box(
                b, baker, base, slots, Boxes.IDENTITY, slotBySide, windows, x0, y0, z0, x1, y1, z1);
    }

    public interface Factory {
        ConveyorChuteModel create(ModelBaker baker, ResolvedModel base);
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
            return create(baker, baker.getModel(baseModel));
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return this;
        }
    }
}
