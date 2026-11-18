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

package com.hbm.world.feature;

import com.hbm.blockentity.BedrockOreBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModItems;
import com.hbm.items.special.BedrockOreSet;
import com.hbm.registration.OverridePoint;
import com.hbm.util.RegistryUtil;
import com.hbm.world.NtmWorldgenFields;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public record BedrockOreFeature(
        boolean auto, List<Entry> entries, BlockState depthRock, RuleTest host) implements Feature {
    private static final RuleTest NATURAL_STONE = new TagMatchTest(BlockTags.BASE_STONE_OVERWORLD);

    public static final MapCodec<BedrockOreFeature> CODEC =
            RecordCodecBuilder.mapCodec(
                    inst ->
                            inst.group(
                                            Codec.BOOL
                                                    .fieldOf("auto")
                                                    .forGetter(BedrockOreFeature::auto),
                                            Entry.CODEC
                                                    .listOf()
                                                    .fieldOf("entries")
                                                    .forGetter(BedrockOreFeature::entries),
                                            BlockState.CODEC
                                                    .fieldOf("depth_rock")
                                                    .forGetter(BedrockOreFeature::depthRock),
                                            RuleTest.CODEC
                                                    .optionalFieldOf("host", NATURAL_STONE)
                                                    .forGetter(BedrockOreFeature::host))
                                    .apply(inst, BedrockOreFeature::new));

    public BedrockOreFeature(boolean auto, List<Entry> entries, BlockState depthRock) {
        this(auto, entries, depthRock, NATURAL_STONE);
    }

    public record Entry(Item item, int count, int color, int tier, int weight) {
        public static final Codec<Entry> CODEC =
                RecordCodecBuilder.create(
                        inst ->
                                inst.group(
                                                BuiltInRegistries.ITEM
                                                        .byNameCodec()
                                                        .fieldOf("item")
                                                        .forGetter(Entry::item),
                                                Codec.INT.fieldOf("count").forGetter(Entry::count),
                                                Codec.INT.fieldOf("color").forGetter(Entry::color),
                                                Codec.INT.fieldOf("tier").forGetter(Entry::tier),
                                                Codec.INT
                                                        .fieldOf("weight")
                                                        .forGetter(Entry::weight))
                                        .apply(inst, Entry::new));
    }

    @Override
    public MapCodec<BedrockOreFeature> codec() {
        return CODEC;
    }

    public static int getTier(double density) {
        if (density > 1.5) return 4;
        if (density > 1) return 3;
        if (density > 0.75) return 2;
        return 1;
    }

    public static final OverridePoint<Function<Level, @Nullable FluidStackNTM>> BORE_FLUID =
            OverridePoint.of(RegistryUtil.id("bedrock_bore_fluid"), level -> null);

    public static @Nullable FluidStackNTM getBoreFluid(int tier) {
        return switch (tier) {
            case 2 -> BoreFluids.WATER;
            case 3 -> BoreFluids.SULFURIC_ACID;
            case 4 -> BoreFluids.SOLVENT;
            default -> null;
        };
    }

    private static final class BoreFluids {
        private static final FluidStackNTM WATER = new FluidStackNTM(Fluids.WATER, 1_000);
        private static final FluidStackNTM SULFURIC_ACID =
                new FluidStackNTM(NTMFluids.SULFURIC_ACID, 1_000);
        private static final FluidStackNTM SOLVENT = new FluidStackNTM(NTMFluids.SOLVENT, 2_000);
    }

    private static BedrockOreFeature.@Nullable Entry pickWeighted(
            List<BedrockOreFeature.Entry> entries, RandomSource rand) {
        int total = 0;
        for (BedrockOreFeature.Entry e : entries) total += e.weight();
        if (total <= 0) return null;
        int r = rand.nextInt(total);
        for (BedrockOreFeature.Entry e : entries) {
            r -= e.weight();
            if (r < 0) return e;
        }
        return entries.getLast();
    }

    @Override
    public boolean place(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BlockPos featureOrigin) {
        BedrockOreFeature cfg = this;
        RandomSource rand = random;
        int x = featureOrigin.getX();
        int z = featureOrigin.getZ();

        ItemStack resource;
        FluidStackNTM acid;
        int color;
        int tier;

        if (cfg.auto()) {
            BedrockOreField field = NtmWorldgenFields.get(level).bedrock();
            tier =
                    getTier(
                            field.averageOreLevel(
                                    BedrockOreSet.OF.get().apply(level.getLevel()), x, z));
            acid = BORE_FLUID.get().apply(level.getLevel());
            if (acid == null) acid = getBoreFluid(tier);
            color = 0xD78A16;
            resource = new ItemStack(ModItems.BEDROCK_ORE_BASE);
        } else {
            BedrockOreFeature.Entry entry = pickWeighted(cfg.entries(), rand);
            if (entry == null) return false;
            resource = new ItemStack(entry.item(), entry.count());
            color = entry.color();
            tier = entry.tier();
            acid = null;
        }

        generate(level, x, z, resource, acid, color, tier, cfg.depthRock(), cfg.host(), rand);
        return true;
    }

    public static void generate(
            WorldGenLevel level,
            int x,
            int z,
            ItemStack resource,
            @Nullable FluidStackNTM acid,
            int color,
            int tier,
            BlockState depth,
            RuleTest host,
            RandomSource rand) {
        int floor = level.getMinY();
        BlockState ore = ModBlocks.ORE_BEDROCK.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int ix = x - 1; ix <= x + 1; ix++) {
            for (int iz = z - 1; iz <= z + 1; iz++) {
                pos.set(ix, floor, iz);
                if (!level.getBlockState(pos).is(Blocks.BEDROCK)) continue;
                if ((ix == x && iz == z) || rand.nextBoolean()) {
                    level.setBlock(pos, ore, 2);
                    if (level.getBlockEntity(pos) instanceof BedrockOreBlockEntity be) {
                        be.setData(resource.copy(), acid, color, tier, rand.nextInt(10));
                    }
                }
            }
        }

        for (int ix = x - 3; ix <= x + 3; ix++) {
            for (int iz = z - 3; iz <= z + 3; iz++) {
                for (int dy = 1; dy < 7; dy++) {
                    pos.set(ix, floor + dy, iz);
                    BlockState cur = level.getBlockState(pos);
                    if (dy >= 3 && !cur.is(Blocks.BEDROCK)) continue;
                    if (host.test(cur, pos, rand) || cur.is(Blocks.BEDROCK))
                        level.setBlock(pos, depth, 2);
                }
            }
        }
    }
}
