/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.neoforge.client.ctm;

import io.github.chiselteam.ctm.api.model.CTMVariant;
import io.github.chiselteam.ctm.api.strategy.CTMKind;
import io.github.chiselteam.ctm.api.strategy.CTMLogic;
import io.github.chiselteam.ctm.client.baked.StandardCTMBlockStateModel;
import io.github.chiselteam.ctm.impl.texture.CTMFaceOrientation;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.joml.Vector3fc;

final class CtmLibBakedModel extends StandardCTMBlockStateModel {

    private final Block[] connections;
    private final BlockState[][] connectionStates;
    private final int connectedMask;
    private final int flags;

    private CtmLibBakedModel(
            Block owner,
            Map<Direction, BakedQuad[]> plain,
            Map<Direction, BakedQuad[][]> connected,
            TextureAtlasSprite particle,
            Block[] connections,
            BlockState[][] connectionStates,
            int flags) {
        super(
                connected.keySet(),
                Set.of(),
                false,
                plain,
                connected,
                particle,
                CTMVariant.of(owner, CTMKind.STANDARD));
        this.connections = connections;
        this.connectionStates = connectionStates;
        int mask = 0;
        for (Direction face : connected.keySet()) mask |= 1 << face.ordinal();
        this.connectedMask = mask;
        this.flags = flags;
    }

    static BlockStateModel.UnbakedRoot wrap(
            BlockStateModel.UnbakedRoot root,
            List<Block> connections,
            Map<Identifier, Identifier> textures,
            Map<Identifier, List<BlockState>> connectionStates) {
        return new Root(root, connections, textures, connectionStates);
    }

    @Override
    protected boolean shouldConnectSide(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            Direction face,
            Direction side) {
        return connects(level, pos, state, face, pos.relative(side));
    }

    @Override
    protected boolean isCornerBlockPresent(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            Direction face,
            Direction first,
            Direction second) {
        return connects(level, pos, state, face, pos.relative(first).relative(second));
    }

    private boolean connects(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            Direction face,
            BlockPos neighborPos) {
        if ((connectedMask & 1 << face.ordinal()) == 0) return false;
        BlockState neighbor = level.getBlockState(neighborPos);
        BlockState appearance = neighbor.getAppearance(level, neighborPos, face, state, pos);
        BlockState[] allowed = connectionStates[face.ordinal()];
        if (allowed != null) {
            for (BlockState expected : allowed) if (appearance == expected) return true;
            return false;
        }
        for (Block block : connections) if (appearance.is(block)) return true;
        return false;
    }

    @Override
    public int materialFlags() {
        return flags;
    }

    private record Root(
            BlockStateModel.UnbakedRoot base,
            List<Block> connections,
            Map<Identifier, Identifier> textures,
            Map<Identifier, List<BlockState>> connectionStates)
            implements BlockStateModel.UnbakedRoot {

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            base.resolveDependencies(resolver);
            textures.values().forEach(resolver::markDependency);
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return base.visualEqualityGroup(state);
        }

        @Override
        public BlockStateModel bake(BlockState state, ModelBaker baker) {
            BlockStateModel original = base.bake(state, baker);
            List<BlockStateModelPart> parts = new ArrayList<>();
            original.collectParts(RandomSource.create(0), parts);
            boolean applies = false;
            for (BlockStateModelPart part : parts) {
                for (Direction face : Direction.VALUES) {
                    for (BakedQuad quad : part.getQuads(face)) {
                        if (textures.containsKey(quad.materialInfo().sprite().contents().name()))
                            applies = true;
                    }
                }
            }
            if (!applies) return original;
            return baker.compute(
                    new BakeKey(
                            state.getBlock(),
                            List.copyOf(parts),
                            original.particleMaterial(),
                            original.materialFlags(),
                            connections,
                            textures,
                            connectionStates));
        }
    }

    private record BakeKey(
            Block owner,
            List<BlockStateModelPart> parts,
            Material.Baked particle,
            int originalFlags,
            List<Block> connections,
            Map<Identifier, Identifier> textures,
            Map<Identifier, List<BlockState>> connectionStates)
            implements ModelBaker.SharedOperationKey<BlockStateModel> {

        @Override
        public BlockStateModel compute(ModelBaker baker) {
            Map<Direction, BakedQuad[]> plain = new EnumMap<>(Direction.class);
            Map<Direction, BakedQuad[][]> connected = new EnumMap<>(Direction.class);
            int flags = originalFlags;
            BlockState[][] statesByFace = new BlockState[Direction.VALUES.length][];
            for (BlockStateModelPart part : parts) {
                if (!part.getQuads(null).isEmpty()) {
                    throw new IllegalStateException(owner + ": CTM Lib requires culled cube faces");
                }
            }
            for (Direction face : Direction.VALUES) {
                ArrayList<BakedQuad> faceQuads = new ArrayList<>();
                parts.forEach(part -> faceQuads.addAll(part.getQuads(face)));
                if (faceQuads.size() != 1) {
                    throw new IllegalStateException(
                            owner + ": CTM Lib requires one cube quad on " + face);
                }
                BakedQuad quad = faceQuads.getFirst();
                Identifier texture = textures.get(quad.materialInfo().sprite().contents().name());
                if (texture == null) {
                    plain.put(face, new BakedQuad[] {quad});
                    continue;
                }
                List<BlockState> allowed =
                        connectionStates.get(quad.materialInfo().sprite().contents().name());
                if (allowed != null)
                    statesByFace[face.ordinal()] = allowed.toArray(BlockState[]::new);
                ResolvedModel carrier = baker.getModel(texture);
                Material.Baked atlas =
                        baker.materials()
                                .get(
                                        carrier.getTopTextureSlots().getMaterial("connected"),
                                        carrier);
                BakedQuad[][] corners = new BakedQuad[4][CTMLogic.values().length];
                boolean swapped = swapsTextureAxes(quad);
                for (int corner = 0; corner < 4; corner++) {
                    int vertex = cornerVertex(quad, corner);
                    for (CTMLogic logic : CTMLogic.values()) {
                        corners[corner][logic.ordinal()] =
                                split(quad, vertex, logic, swapped, atlas.sprite());
                        flags |= corners[corner][logic.ordinal()].materialInfo().flags();
                    }
                }
                connected.put(face, corners);
            }
            if (connected.isEmpty())
                throw new IllegalStateException(owner + ": CTM Lib matched no face texture");
            return new CtmLibBakedModel(
                    owner,
                    plain,
                    connected,
                    particle.sprite(),
                    connections.toArray(Block[]::new),
                    statesByFace,
                    flags);
        }
    }

    private static int cornerVertex(BakedQuad quad, int corner) {
        Direction face = quad.direction();
        Direction first = CTMFaceOrientation.IDENTITY.cornerDirection(face, corner);
        Direction second = CTMFaceOrientation.IDENTITY.cornerDirection(face, (corner + 1) % 4);
        Vector3f target =
                new Vector3f(face.getStepX(), face.getStepY(), face.getStepZ())
                        .add(first.getStepX(), first.getStepY(), first.getStepZ())
                        .add(second.getStepX(), second.getStepY(), second.getStepZ())
                        .add(1, 1, 1)
                        .mul(0.5F);
        for (int vertex = 0; vertex < 4; vertex++) {
            if (quad.position(vertex).distanceSquared(target) < 1.0E-10F) return vertex;
        }
        throw new IllegalStateException(
                quad.materialInfo().sprite().contents().name()
                        + ": CTM Lib requires a unit cube on "
                        + face);
    }

    private static boolean swapsTextureAxes(BakedQuad quad) {
        Direction.Axis identityU =
                CTMFaceOrientation.IDENTITY.textureDirection(quad.direction(), 1).getAxis();
        for (int adjacent : new int[] {1, 3}) {
            Vector3f delta = new Vector3f(quad.position(adjacent)).sub(quad.position0());
            if (Math.abs(identityU.choose(delta.x, delta.y, delta.z)) < 0.5F) continue;
            float du =
                    Math.abs(
                            UVPair.unpackU(quad.packedUV(adjacent))
                                    - UVPair.unpackU(quad.packedUV0()));
            float dv =
                    Math.abs(
                            UVPair.unpackV(quad.packedUV(adjacent))
                                    - UVPair.unpackV(quad.packedUV0()));
            return dv > du;
        }
        throw new IllegalStateException("No CTM texture U edge on " + quad.direction());
    }

    private static BakedQuad split(
            BakedQuad quad, int corner, CTMLogic logic, boolean swapped, TextureAtlasSprite sheet) {
        float s0 = corner >= 2 ? 0.5F : 0;
        float t0 = corner == 1 || corner == 2 ? 0.5F : 0;
        boolean alongU = logic == (swapped ? CTMLogic.HORIZONTAL : CTMLogic.VERTICAL);
        boolean alongV = logic == (swapped ? CTMLogic.VERTICAL : CTMLogic.HORIZONTAL);
        float tileU = logic == CTMLogic.CORNER || alongV ? 0.5F : 0;
        float tileV = logic == CTMLogic.CORNER || alongU ? 0.5F : 0;
        float scale = logic == CTMLogic.NONE ? 1 : 0.5F;
        Vector3fc[] positions = new Vector3fc[4];
        long[] uvs = new long[4];
        TextureAtlasSprite base = quad.materialInfo().sprite();
        TextureAtlasSprite sprite = logic == CTMLogic.NONE ? base : sheet;
        for (int vertex = 0; vertex < 4; vertex++) {
            float s = s0 + (vertex >= 2 ? 0.5F : 0);
            float t = t0 + (vertex == 1 || vertex == 2 ? 0.5F : 0);
            positions[vertex] =
                    new Vector3f(quad.position0())
                            .lerp(quad.position3(), s)
                            .lerp(new Vector3f(quad.position1()).lerp(quad.position2(), s), t);
            float u = interpolate(quad, s, t, true);
            float v = interpolate(quad, s, t, false);
            u = (u - base.getU0()) / (base.getU1() - base.getU0());
            v = (v - base.getV0()) / (base.getV1() - base.getV0());
            uvs[vertex] =
                    UVPair.pack(sprite.getU(tileU + u * scale), sprite.getV(tileV + v * scale));
        }
        BakedQuad.MaterialInfo info = quad.materialInfo();
        final RenderType itemType = info.itemRenderType();
        final int tint = info.tintIndex();
        final int emission = info.lightEmission();
        BakedQuad.MaterialInfo material =
                new BakedQuad.MaterialInfo(
                        sprite,
                        info.layer(),
                        itemType,
                        info.itemGlintRenderType(),
                        info.itemGlintSpecialRenderType(),
                        tint,
                        info.shadeDirectionOverride(),
                        emission,
                        info.ambientOcclusion() && true);
        return new BakedQuad(
                positions[0],
                positions[1],
                positions[2],
                positions[3],
                uvs[0],
                uvs[1],
                uvs[2],
                uvs[3],
                quad.direction(),
                material,
                quad.bakedNormals(),
                quad.bakedColors());
    }

    private static float interpolate(BakedQuad quad, float s, float t, boolean u) {
        float a = u ? UVPair.unpackU(quad.packedUV0()) : UVPair.unpackV(quad.packedUV0());
        float b = u ? UVPair.unpackU(quad.packedUV1()) : UVPair.unpackV(quad.packedUV1());
        float c = u ? UVPair.unpackU(quad.packedUV2()) : UVPair.unpackV(quad.packedUV2());
        float d = u ? UVPair.unpackU(quad.packedUV3()) : UVPair.unpackV(quad.packedUV3());
        return (a + (d - a) * s) * (1 - t) + (b + (c - b) * s) * t;
    }
}
