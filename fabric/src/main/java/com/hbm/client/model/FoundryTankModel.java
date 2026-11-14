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

import com.hbm.blocks.machine.FoundryTankBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
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

public class FoundryTankModel implements BlockStateModel {

    private static final Direction[] HORIZONTALS = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    public static Factory FACTORY = FoundryTankModel::new;

    private final ModelBaker baker;
    private final ResolvedModel base;
    private final TextureSlots slots;
    private final Material.Baked particle;
    private final BlockStateModelPart fallback;
    private final ModelPartCache cache = ModelPartCache.dense(1024, this::buildPart);

    public FoundryTankModel(ModelBaker baker, ResolvedModel base) {
        this.baker = RuntimeBaker.capture(baker, base);
        this.base = base;
        this.slots = base.getTopTextureSlots();
        this.particle = base.resolveParticleMaterial(slots, baker);
        this.fallback = buildPart(0);
    }

    private static FoundryTankModel create(ModelBaker baker, ResolvedModel base) {
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
        output.add(partFor(FoundryTankBlock.mask(level, pos)));
    }

    public Object geometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return FoundryTankBlock.mask(level, pos);
    }

    public BlockStateModelPart partFor(int mask) {
        return cache.get(mask);
    }

    private BlockStateModelPart buildPart(int mask) {
        return new SimpleModelWrapper(
                build(baker, base, slots, mask), base.getTopAmbientOcclusion(), particle);
    }

    private static boolean tank(int mask, Direction dir) {
        return (mask & (1 << dir.ordinal())) != 0;
    }

    private static boolean outlet(int mask, Direction dir) {
        return (mask & (1 << (4 + dir.ordinal()))) != 0;
    }

    private static QuadCollection build(
            ModelBaker baker, ResolvedModel base, TextureSlots slots, int mask) {
        List<ObjUnbakedGeometry.BoxQuad> b = new ArrayList<>();
        boolean down = tank(mask, Direction.DOWN);
        boolean up = tank(mask, Direction.UP);

        if (!down) {
            Vector3f from = new Vector3f(0F, 0F, 0F);
            Vector3f to = new Vector3f(16F, 2F, 16F);
            Material.Baked bottom = BlockModel.slot(baker, base, slots, "bottom");
            b.add(ownBlock(Boxes.face(baker, from, to, Direction.UP, bottom)));
            b.add(ownBlock(Boxes.face(baker, from, to, Direction.DOWN, bottom)));
        }

        Material.Baked inner = BlockModel.slot(baker, base, slots, up ? "bottom" : "inner");
        Material.Baked top = BlockModel.slot(baker, base, slots, "top");

        for (Direction dir : HORIZONTALS) {
            if (tank(mask, dir)) continue;

            Vector3f from =
                    new Vector3f(
                            dir == Direction.EAST ? 14F : 0F,
                            0F,
                            dir == Direction.SOUTH ? 14F : 0F);
            Vector3f to =
                    new Vector3f(
                            dir == Direction.WEST ? 2F : 16F,
                            16F,
                            dir == Direction.NORTH ? 2F : 16F);

            String outerSlot =
                    down
                            ? (outlet(mask, dir) ? "upper_outlet" : "upper")
                            : (outlet(mask, dir) ? "side_outlet" : "side");
            b.add(
                    ownBlock(
                            Boxes.face(
                                    baker,
                                    from,
                                    to,
                                    dir,
                                    BlockModel.slot(baker, base, slots, outerSlot))));
            b.add(ownBlock(Boxes.face(baker, from, to, dir.getOpposite(), inner)));

            for (Direction flank : HORIZONTALS) {
                if (flank.getAxis() == dir.getAxis() || !tank(mask, flank)) continue;
                b.add(ownBlock(Boxes.face(baker, from, to, flank, inner)));
            }

            b.add(ownBlock(Boxes.face(baker, from, to, Direction.UP, top)));
        }

        return ObjUnbakedGeometry.bakeGroups(List.of(), b, 0F, 0F, 0F);
    }

    private static ObjUnbakedGeometry.BoxQuad ownBlock(BakedQuad quad) {
        return new ObjUnbakedGeometry.BoxQuad(
                quad, ObjUnbakedGeometry.WHITE, null, QuadLighting.OWN_BLOCK);
    }

    public interface Factory {
        FoundryTankModel create(ModelBaker baker, ResolvedModel base);
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
