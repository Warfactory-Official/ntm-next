/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client.model;

import com.hbm.blockentity.machine.FoundryDischarge;
import com.hbm.blocks.machine.FoundryOutletBlock;
import com.hbm.client.render.FoundryFaces;
import com.hbm.util.Facing;
import com.mojang.math.Quadrant;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public class FoundryOutletModel implements BlockStateModel {
    public static Factory FACTORY = FoundryOutletModel::new;
    private static final CuboidFace.UVs PLATE_UV = new CuboidFace.UVs(6F, 8F, 10F, 15F);
    private final ModelBaker baker;
    private final ResolvedModel carrier;
    private final TextureSlots slots;
    private final Material.Baked particle;
    private final BlockStateModelPart fallback;
    private final ModelPartCache cache = ModelPartCache.dense(16, this::buildPart);

    public FoundryOutletModel(ModelBaker baker, ResolvedModel carrier) {
        this.baker = RuntimeBaker.capture(baker, carrier);
        this.carrier = carrier;
        slots = carrier.getTopTextureSlots();
        particle = carrier.resolveParticleMaterial(slots, baker);
        fallback = partFor(Direction.NORTH.get2DDataValue());
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        output.add(fallback);
    }

    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> output) {
        output.add(partFor(key(level, pos, state)));
    }

    public Object geometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return key(level, pos, state);
    }

    public static void blockChanged(ClientLevel level, BlockPos pos) {
        for (Direction direction : Direction.VALUES) {
            int coordinate =
                    switch (direction.getAxis()) {
                        case X -> pos.getX();
                        case Y -> pos.getY();
                        case Z -> pos.getZ();
                    };
            int boundary =
                    direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 14 : 1;
            if ((coordinate & 15) != boundary) continue;
            BlockPos outletPos = pos.relative(direction, 2);
            BlockState outlet = level.getBlockState(outletPos);
            if (!(outlet.getBlock() instanceof FoundryOutletBlock)) continue;
            BlockPos conductorPos = pos.relative(direction);
            BlockState conductor = level.getBlockState(conductorPos);
            if (!conductor.isRedstoneConductor(level, conductorPos)) continue;
            level.sendBlockUpdated(outletPos, outlet, outlet, 0);
        }
    }

    private static int key(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        int key = state.getValue(FoundryOutletBlock.FACING).get2DDataValue();
        if (level.getBlockEntity(pos) instanceof FoundryDischarge outlet) {
            if (outlet.filter != null) key |= 4;
            boolean hasNeighborSignal;
            hasNeighborSignal:
            {
                for (Direction direction : Direction.VALUES) {
                    BlockPos neighborPos = pos.relative(direction);
                    BlockState neighbor = ((BlockGetter) level).getBlockState(neighborPos);
                    int signal = neighbor.getSignal((BlockGetter) level, neighborPos, direction);
                    if (neighbor.isRedstoneConductor((BlockGetter) level, neighborPos)) {
                        int direct = 0;
                        for (Direction directDirection : Direction.VALUES) {
                            BlockPos directPos = neighborPos.relative(directDirection);
                            BlockState directState = ((BlockGetter) level).getBlockState(directPos);
                            direct =
                                    Math.max(
                                            direct,
                                            directState.getDirectSignal(
                                                    (BlockGetter) level,
                                                    directPos,
                                                    directDirection));
                            if (direct >= 15) break;
                        }
                        signal = Math.max(signal, direct);
                    }
                    if (signal > 0) {
                        hasNeighborSignal = true;
                        break hasNeighborSignal;
                    }
                }
                hasNeighborSignal = false;
            }
            boolean powered = hasNeighborSignal;
            if (outlet.invertRedstone ^ powered) key |= 8;
        }
        return key;
    }

    public BlockStateModelPart partFor(int key) {
        return cache.get(key);
    }

    @Override
    public Material.Baked particleMaterial() {
        return particle;
    }

    @Override
    public int materialFlags() {
        return fallback.materialFlags();
    }

    private BlockStateModelPart buildPart(int key) {
        Direction facing = Direction.from2DDataValue(key & 3);
        ModelState rotation = BlockModelRotation.get(Facing.yawRotation(facing, 0));
        QuadCollection stub = carrier.bakeTopGeometry(slots, baker, rotation);
        List<ObjUnbakedGeometry.BoxQuad> quads = new ArrayList<>(stub.getAll().size() + 4);
        append(quads, stub, null);
        for (Direction side : Direction.VALUES) append(quads, stub, side);
        int color = 0xFF000000 | FoundryFaces.shaded(0xFFFFFF, facing);
        if ((key & 4) != 0) plate(quads, rotation, "filter", 15.5F, color);
        if ((key & 8) != 0) plate(quads, rotation, "lock", 15F, color);
        QuadCollection geometry = ObjUnbakedGeometry.bakeGroups(List.of(), quads, 0F, 0F, 0F);
        return new SimpleModelWrapper(geometry, carrier.getTopAmbientOcclusion(), particle);
    }

    private static void append(
            List<ObjUnbakedGeometry.BoxQuad> output,
            QuadCollection geometry,
            @Nullable Direction cull) {
        for (BakedQuad quad : geometry.getQuads(cull)) {
            output.add(new ObjUnbakedGeometry.BoxQuad(quad, ObjUnbakedGeometry.WHITE, cull));
        }
    }

    private void plate(
            List<ObjUnbakedGeometry.BoxQuad> output,
            ModelState rotation,
            String slot,
            float z,
            int color) {
        Material.Baked material = BlockModel.slot(baker, carrier, slots, slot);
        Vector3f from = new Vector3f(6F, 1F, z);
        Vector3f to = new Vector3f(10F, 8F, z);
        plateFace(output, rotation, material, from, to, Direction.SOUTH, color);
        plateFace(output, rotation, material, from, to, Direction.NORTH, color);
    }

    private void plateFace(
            List<ObjUnbakedGeometry.BoxQuad> output,
            ModelState rotation,
            Material.Baked material,
            Vector3f from,
            Vector3f to,
            Direction side,
            int color) {
        BakedQuad quad =
                Boxes.face(
                        baker,
                        from,
                        to,
                        side,
                        material,
                        PLATE_UV,
                        Quadrant.R0,
                        CuboidFace.NO_TINT,
                        rotation);
        final BakedQuad.MaterialInfo original = quad.materialInfo();
        var unshaded =
                new BakedQuad.MaterialInfo(
                        original.sprite(),
                        original.layer(),
                        original.itemRenderType(),
                        original.itemGlintRenderType(),
                        original.itemGlintSpecialRenderType(),
                        original.tintIndex(),
                        false ? null : Direction.UP,
                        original.lightEmission(),
                        original.ambientOcclusion());
        BakedQuad face =
                new BakedQuad(
                        quad.position0(),
                        quad.position1(),
                        quad.position2(),
                        quad.position3(),
                        quad.packedUV0(),
                        quad.packedUV1(),
                        quad.packedUV2(),
                        quad.packedUV3(),
                        quad.direction(),
                        unshaded);
        output.add(new ObjUnbakedGeometry.BoxQuad(face, color, null, QuadLighting.OWN_BLOCK));
    }

    public interface Factory {
        FoundryOutletModel create(ModelBaker baker, ResolvedModel carrier);
    }

    public record Family() implements SimpleBlockModel {
        @Override
        public BlockStateModel.UnbakedRoot root(Block block, BlockState state) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            return new Root(id.withPath("block/" + id.getPath()));
        }
    }

    public record Root(Identifier carrier) implements BlockStateModel.UnbakedRoot {
        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(carrier);
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
            return FACTORY.create(baker, baker.getModel(root.carrier()));
        }
    }
}
