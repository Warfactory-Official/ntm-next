// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.network.ConveyorBlockBase;
import com.hbm.blocks.network.ConveyorChuteBlock;
import com.hbm.blocks.network.ConveyorNeighbors;
import com.mojang.math.Quadrant;
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
        this.baker = RuntimeBaker.capture(baker, base);
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
            int[] beltRot = ConveyorModel.beltRot(facing);
            final Vector3f from = new Vector3f(INNER_MIN, 0F, 0F);
            final Vector3f to = new Vector3f(INNER_MAX, LIP, 16F);
            ConveyorModel.faces(
                    b,
                    this.baker,
                    this.base,
                    this.slots,
                    Boxes.IDENTITY,
                    face -> beltSlot(facing, face),
                    (Direction) null,
                    from,
                    to,
                    new UvRotation[] {
                        switch (beltRot[0]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(4.0F, 16.0F, 12.0F, 0.0F),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 12.0F, 16.0F, 4.0F),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 4.0F, 0.0F, 12.0F),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(12.0F, 0.0F, 4.0F, 16.0F),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + beltRot[0] + " outside 0..3");
                        },
                        switch (beltRot[1]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(4.0F, 0.0F, 12.0F, 16.0F),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 4.0F, 16.0F, 12.0F),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 12.0F, 0.0F, 4.0F),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(12.0F, 16.0F, 4.0F, 0.0F),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + beltRot[1] + " outside 0..3");
                        },
                        switch (beltRot[2]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 4.0F, 12.0F, 12.0F),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 12.0F, 4.0F, 4.0F),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(12.0F, 4.0F, 4.0F, 0.0F),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + beltRot[2] + " outside 0..3");
                        },
                        switch (beltRot[3]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 4.0F, 4.0F, 12.0F),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 12.0F, 12.0F, 4.0F),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(12.0F, 4.0F, 4.0F, 0.0F),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + beltRot[3] + " outside 0..3");
                        },
                        switch (beltRot[4]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 12.0F, 16.0F, 16.0F),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 0.0F, 4.0F, 16.0F),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 16.0F, 12.0F, 0.0F),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 4.0F, 0.0F, 0.0F),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + beltRot[4] + " outside 0..3");
                        },
                        switch (beltRot[5]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 12.0F, 16.0F, 16.0F),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 0.0F, 12.0F, 16.0F),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 16.0F, 4.0F, 0.0F),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 4.0F, 0.0F, 0.0F),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + beltRot[5] + " outside 0..3");
                        }
                    });
            final Vector3f from2 = new Vector3f(0F, 0F, INNER_MIN);
            final Vector3f to2 = new Vector3f(INNER_MIN, LIP, INNER_MAX);
            ConveyorModel.faces(
                    b,
                    this.baker,
                    this.base,
                    this.slots,
                    Boxes.IDENTITY,
                    face -> beltSlot(facing, face),
                    (Direction) null,
                    from2,
                    to2,
                    new UvRotation[] {
                        switch (beltRot[0]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 12.0F, 4.0F, 4.0F),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(4.0F, 4.0F, 12.0F, 0.0F),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(12.0F, 12.0F, 4.0F, 16.0F),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 4.0F, 12.0F, 12.0F),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + beltRot[0] + " outside 0..3");
                        },
                        switch (beltRot[1]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 4.0F, 4.0F, 12.0F),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(12.0F, 4.0F, 4.0F, 0.0F),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 12.0F, 12.0F, 4.0F),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + beltRot[1] + " outside 0..3");
                        },
                        switch (beltRot[2]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 12.0F, 4.0F, 16.0F),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 0.0F, 12.0F, 4.0F),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 16.0F, 4.0F, 12.0F),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 4.0F, 12.0F, 0.0F),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + beltRot[2] + " outside 0..3");
                        },
                        switch (beltRot[3]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 12.0F, 4.0F, 16.0F),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 12.0F, 4.0F, 16.0F),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 4.0F, 12.0F, 0.0F),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 4.0F, 12.0F, 0.0F),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + beltRot[3] + " outside 0..3");
                        },
                        switch (beltRot[4]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 4.0F, 4.0F, 12.0F),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 12.0F, 12.0F, 4.0F),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(12.0F, 4.0F, 4.0F, 0.0F),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + beltRot[4] + " outside 0..3");
                        },
                        switch (beltRot[5]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 4.0F, 12.0F, 12.0F),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 12.0F, 4.0F, 4.0F),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(12.0F, 4.0F, 4.0F, 0.0F),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + beltRot[5] + " outside 0..3");
                        }
                    });
            final Vector3f from3 = new Vector3f(INNER_MAX, 0F, INNER_MIN);
            final Vector3f to3 = new Vector3f(16F, LIP, INNER_MAX);
            ConveyorModel.faces(
                    b,
                    this.baker,
                    this.base,
                    this.slots,
                    Boxes.IDENTITY,
                    face -> beltSlot(facing, face),
                    (Direction) null,
                    from3,
                    to3,
                    new UvRotation[] {
                        switch (beltRot[0]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(12.0F, 12.0F, 16.0F, 4.0F),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(4.0F, 16.0F, 12.0F, 12.0F),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(12.0F, 0.0F, 4.0F, 4.0F),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(4.0F, 4.0F, 0.0F, 12.0F),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + beltRot[0] + " outside 0..3");
                        },
                        switch (beltRot[1]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(12.0F, 4.0F, 16.0F, 12.0F),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(4.0F, 0.0F, 12.0F, 4.0F),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(12.0F, 16.0F, 4.0F, 12.0F),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(4.0F, 12.0F, 0.0F, 4.0F),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + beltRot[1] + " outside 0..3");
                        },
                        switch (beltRot[2]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(12.0F, 12.0F, 16.0F, 16.0F),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 12.0F, 12.0F, 16.0F),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 4.0F, 4.0F, 0.0F),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(4.0F, 4.0F, 0.0F, 0.0F),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + beltRot[2] + " outside 0..3");
                        },
                        switch (beltRot[3]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(12.0F, 12.0F, 16.0F, 16.0F),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 0.0F, 4.0F, 4.0F),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 16.0F, 12.0F, 12.0F),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(4.0F, 4.0F, 0.0F, 0.0F),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + beltRot[3] + " outside 0..3");
                        },
                        switch (beltRot[4]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 4.0F, 4.0F, 12.0F),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 12.0F, 12.0F, 4.0F),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(12.0F, 4.0F, 4.0F, 0.0F),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + beltRot[4] + " outside 0..3");
                        },
                        switch (beltRot[5]) {
                            case 0 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F),
                                            Quadrant.R0));
                            case 1 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(16.0F, 4.0F, 12.0F, 12.0F),
                                            Quadrant.R90));
                            case 2 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(0.0F, 12.0F, 4.0F, 4.0F),
                                            Quadrant.R90));
                            case 3 ->
                                    (new UvRotation(
                                            new CuboidFace.UVs(12.0F, 4.0F, 4.0F, 0.0F),
                                            Quadrant.R0));
                            default ->
                                    throw new IllegalArgumentException(
                                            "uvRotate value " + beltRot[5] + " outside 0..3");
                        }
                    });
        } else {
            if (beltAt(belts, Direction.WEST)) {
                final Vector3f from4 = new Vector3f(0F, 0F, INNER_MIN);
                final Vector3f to4 = new Vector3f(GLASS_MIN, LIP, INNER_MAX);
                ConveyorModel.faces(
                        b,
                        this.baker,
                        this.base,
                        this.slots,
                        Boxes.IDENTITY,
                        face2 -> beltSlot(facing, face2),
                        (Direction) null,
                        from4,
                        to4,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(0.0F, 12.0F, 2.0F, 4.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 14.0F, 12.0F, 16.0F), Quadrant.R90),
                            new UvRotation(
                                    new CuboidFace.UVs(0.0F, 12.0F, 2.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(0.0F, 12.0F, 2.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F), Quadrant.R0)
                        });
            }
            if (beltAt(belts, Direction.EAST)) {
                final Vector3f from5 = new Vector3f(GLASS_MAX, 0F, INNER_MIN);
                final Vector3f to5 = new Vector3f(16F, LIP, INNER_MAX);
                ConveyorModel.faces(
                        b,
                        this.baker,
                        this.base,
                        this.slots,
                        Boxes.IDENTITY,
                        face3 -> beltSlot(facing, face3),
                        (Direction) null,
                        from5,
                        to5,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(14.0F, 12.0F, 16.0F, 4.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(12.0F, 16.0F, 4.0F, 14.0F), Quadrant.R90),
                            new UvRotation(
                                    new CuboidFace.UVs(14.0F, 12.0F, 16.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(14.0F, 12.0F, 16.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F), Quadrant.R0)
                        });
            }
            if (beltAt(belts, Direction.NORTH)) {
                final Vector3f from6 = new Vector3f(INNER_MIN, 0F, 0F);
                final Vector3f to6 = new Vector3f(INNER_MAX, LIP, GLASS_MIN);
                ConveyorModel.faces(
                        b,
                        this.baker,
                        this.base,
                        this.slots,
                        Boxes.IDENTITY,
                        face4 -> beltSlot(facing, face4),
                        (Direction) null,
                        from6,
                        to6,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 2.0F, 12.0F, 0.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(12.0F, 16.0F, 4.0F, 14.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(0.0F, 12.0F, 2.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(0.0F, 12.0F, 2.0F, 16.0F), Quadrant.R0)
                        });
            }
            if (beltAt(belts, Direction.SOUTH)) {
                final Vector3f from7 = new Vector3f(INNER_MIN, 0F, GLASS_MAX);
                final Vector3f to7 = new Vector3f(INNER_MAX, LIP, 16F);
                ConveyorModel.faces(
                        b,
                        this.baker,
                        this.base,
                        this.slots,
                        Boxes.IDENTITY,
                        face5 -> beltSlot(facing, face5),
                        (Direction) null,
                        from7,
                        to7,
                        new UvRotation[] {
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 16.0F, 12.0F, 14.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 14.0F, 12.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(4.0F, 12.0F, 12.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(14.0F, 12.0F, 16.0F, 16.0F), Quadrant.R0),
                            new UvRotation(
                                    new CuboidFace.UVs(14.0F, 12.0F, 16.0F, 16.0F), Quadrant.R0)
                        });
            }
        }

        final int[] plain = new int[] {0, 0, 0, 0, 0, 0};
        final Vector3f from8 = new Vector3f(0F, 0F, 0F);
        final Vector3f to8 = new Vector3f(INNER_MIN, 16F, INNER_MIN);
        ConveyorModel.faces(
                b,
                this.baker,
                this.base,
                this.slots,
                Boxes.IDENTITY,
                face -> ConveyorModel.CONCRETE,
                (Direction) null,
                from8,
                to8,
                new UvRotation[] {
                    new UvRotation(new CuboidFace.UVs(0.0F, 4.0F, 4.0F, 0.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(0.0F, 0.0F, 4.0F, 4.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(0.0F, 0.0F, 4.0F, 16.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(0.0F, 0.0F, 4.0F, 16.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(0.0F, 0.0F, 4.0F, 16.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(0.0F, 0.0F, 4.0F, 16.0F), Quadrant.R0)
                });
        final Vector3f from9 = new Vector3f(INNER_MAX, 0F, 0F);
        final Vector3f to9 = new Vector3f(16F, 16F, INNER_MIN);
        ConveyorModel.faces(
                b,
                this.baker,
                this.base,
                this.slots,
                Boxes.IDENTITY,
                face -> ConveyorModel.CONCRETE,
                (Direction) null,
                from9,
                to9,
                new UvRotation[] {
                    new UvRotation(new CuboidFace.UVs(12.0F, 4.0F, 16.0F, 0.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(12.0F, 0.0F, 16.0F, 4.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(12.0F, 0.0F, 16.0F, 16.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(12.0F, 0.0F, 16.0F, 16.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(0.0F, 0.0F, 4.0F, 16.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(0.0F, 0.0F, 4.0F, 16.0F), Quadrant.R0)
                });
        final Vector3f from10 = new Vector3f(0F, 0F, INNER_MAX);
        final Vector3f to10 = new Vector3f(INNER_MIN, 16F, 16F);
        ConveyorModel.faces(
                b,
                this.baker,
                this.base,
                this.slots,
                Boxes.IDENTITY,
                face -> ConveyorModel.CONCRETE,
                (Direction) null,
                from10,
                to10,
                new UvRotation[] {
                    new UvRotation(new CuboidFace.UVs(0.0F, 16.0F, 4.0F, 12.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(0.0F, 12.0F, 4.0F, 16.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(0.0F, 0.0F, 4.0F, 16.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(0.0F, 0.0F, 4.0F, 16.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(12.0F, 0.0F, 16.0F, 16.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(12.0F, 0.0F, 16.0F, 16.0F), Quadrant.R0)
                });
        final Vector3f from11 = new Vector3f(INNER_MAX, 0F, INNER_MAX);
        final Vector3f to11 = new Vector3f(16F, 16F, 16F);
        ConveyorModel.faces(
                b,
                this.baker,
                this.base,
                this.slots,
                Boxes.IDENTITY,
                face -> ConveyorModel.CONCRETE,
                (Direction) null,
                from11,
                to11,
                new UvRotation[] {
                    new UvRotation(new CuboidFace.UVs(12.0F, 16.0F, 16.0F, 12.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(12.0F, 12.0F, 16.0F, 16.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(12.0F, 0.0F, 16.0F, 16.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(12.0F, 0.0F, 16.0F, 16.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(12.0F, 0.0F, 16.0F, 16.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(12.0F, 0.0F, 16.0F, 16.0F), Quadrant.R0)
                });

        float floor = lowest ? LIP : 0F;
        if (!beltAt(belts, Direction.WEST) && !(lowest && Direction.WEST == facing)) {
            final Vector3f from12 = new Vector3f(GLASS_MIN, floor, INNER_MIN);
            final Vector3f to12 = new Vector3f(GLASS_MIN, 16F, INNER_MAX);
            this.grate(
                    b,
                    Direction.WEST,
                    from12,
                    to12,
                    new UvRotation[] {
                        new UvRotation(new CuboidFace.UVs(2.0F, 12.0F, 2.0F, 4.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(2.0F, 4.0F, 2.0F, 12.0F), Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(2.0F, 0.0F, 2.0F, 16F - from12.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(2.0F, 0.0F, 2.0F, 16F - from12.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(4.0F, 0.0F, 12.0F, 16F - from12.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(4.0F, 0.0F, 12.0F, 16F - from12.y()),
                                Quadrant.R0)
                    });
        }
        if (!beltAt(belts, Direction.EAST) && !(lowest && Direction.EAST == facing)) {
            final Vector3f from13 = new Vector3f(GLASS_MAX, floor, INNER_MIN);
            final Vector3f to13 = new Vector3f(GLASS_MAX, 16F, INNER_MAX);
            this.grate(
                    b,
                    Direction.EAST,
                    from13,
                    to13,
                    new UvRotation[] {
                        new UvRotation(new CuboidFace.UVs(14.0F, 12.0F, 14.0F, 4.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(14.0F, 4.0F, 14.0F, 12.0F), Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(14.0F, 0.0F, 14.0F, 16F - from13.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(14.0F, 0.0F, 14.0F, 16F - from13.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(4.0F, 0.0F, 12.0F, 16F - from13.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(4.0F, 0.0F, 12.0F, 16F - from13.y()),
                                Quadrant.R0)
                    });
        }
        if (!beltAt(belts, Direction.NORTH) && !(lowest && Direction.NORTH == facing)) {
            final Vector3f from14 = new Vector3f(INNER_MIN, floor, GLASS_MIN);
            final Vector3f to14 = new Vector3f(INNER_MAX, 16F, GLASS_MIN);
            this.grate(
                    b,
                    Direction.NORTH,
                    from14,
                    to14,
                    new UvRotation[] {
                        new UvRotation(new CuboidFace.UVs(4.0F, 2.0F, 12.0F, 2.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(4.0F, 2.0F, 12.0F, 2.0F), Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(4.0F, 0.0F, 12.0F, 16F - from14.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(4.0F, 0.0F, 12.0F, 16F - from14.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(2.0F, 0.0F, 2.0F, 16F - from14.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(2.0F, 0.0F, 2.0F, 16F - from14.y()), Quadrant.R0)
                    });
        }
        if (!beltAt(belts, Direction.SOUTH) && !(lowest && Direction.SOUTH == facing)) {
            final Vector3f from15 = new Vector3f(INNER_MIN, floor, GLASS_MAX);
            final Vector3f to15 = new Vector3f(INNER_MAX, 16F, GLASS_MAX);
            this.grate(
                    b,
                    Direction.SOUTH,
                    from15,
                    to15,
                    new UvRotation[] {
                        new UvRotation(new CuboidFace.UVs(4.0F, 14.0F, 12.0F, 14.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(4.0F, 14.0F, 12.0F, 14.0F), Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(4.0F, 0.0F, 12.0F, 16F - from15.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(4.0F, 0.0F, 12.0F, 16F - from15.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(14.0F, 0.0F, 14.0F, 16F - from15.y()),
                                Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(14.0F, 0.0F, 14.0F, 16F - from15.y()),
                                Quadrant.R0)
                    });
        }

        return new SimpleModelWrapper(
                ObjUnbakedGeometry.bakeGroups(List.of(), b, 0F, 0F, 0F),
                base.getTopAmbientOcclusion(),
                particle);
    }

    private void grate(
            List<ObjUnbakedGeometry.BoxQuad> b,
            Direction side,
            Vector3f from,
            Vector3f to,
            UvRotation[] uvs) {
        Material.Baked material = BlockModel.slot(baker, base, slots, GRATE);

        for (Direction face : side.getAxis() == Direction.Axis.X ? X_FACES : Z_FACES) {
            UvRotation uv = uvs[face.ordinal()];
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
