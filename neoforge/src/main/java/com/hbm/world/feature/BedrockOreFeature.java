// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModItems;
import com.hbm.tileentity.BlockEntityBedrockOre;
import com.hbm.world.NtmWorldgenFields;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public class BedrockOreFeature extends Feature<BedrockOreConfig> {

    public BedrockOreFeature() {
        super(BedrockOreConfig.CODEC);
    }

    public static int getTier(double density) {
        if (density > 1.5) return 4;
        if (density > 1) return 3;
        if (density > 0.75) return 2;
        return 1;
    }

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

    private static BedrockOreConfig.@Nullable Entry pickWeighted(
            List<BedrockOreConfig.Entry> entries, RandomSource rand) {
        int total = 0;
        for (BedrockOreConfig.Entry e : entries) total += e.weight();
        if (total <= 0) return null;
        int r = rand.nextInt(total);
        for (BedrockOreConfig.Entry e : entries) {
            r -= e.weight();
            if (r < 0) return e;
        }
        return entries.getLast();
    }

    @Override
    public boolean place(FeaturePlaceContext<BedrockOreConfig> ctx) {
        WorldGenLevel level = ctx.level();
        BedrockOreConfig cfg = ctx.config();
        RandomSource rand = ctx.random();
        int x = ctx.origin().getX();
        int z = ctx.origin().getZ();

        int floor = level.getMinY();

        ItemStack resource;
        FluidStackNTM acid;
        int color;
        int tier;

        if (cfg.auto()) {
            BedrockOreField field = NtmWorldgenFields.get(level).bedrock();
            tier = getTier(field.averageOreLevel(x, z));
            acid = getBoreFluid(tier);
            color = 0xD78A16;
            resource = new ItemStack(ModItems.BEDROCK_ORE_BASE);
        } else {
            BedrockOreConfig.Entry entry = pickWeighted(cfg.entries(), rand);
            if (entry == null) return false;
            resource = new ItemStack(entry.item(), entry.count());
            color = entry.color();
            tier = entry.tier();
            acid = null;
        }

        BlockState ore = ModBlocks.ORE_BEDROCK.get().defaultBlockState();
        BlockState depth = cfg.depthRock();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int ix = x - 1; ix <= x + 1; ix++) {
            for (int iz = z - 1; iz <= z + 1; iz++) {
                pos.set(ix, floor, iz);
                if (!level.getBlockState(pos).is(Blocks.BEDROCK)) continue;
                if ((ix == x && iz == z) || rand.nextBoolean()) {
                    level.setBlock(pos, ore, 2);
                    if (level.getBlockEntity(pos) instanceof BlockEntityBedrockOre be) {
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
                    if (cur.is(Blocks.STONE)
                            || cur.is(Blocks.DEEPSLATE)
                            || cur.is(Blocks.NETHERRACK)
                            || cur.is(Blocks.BEDROCK)) {
                        level.setBlock(pos, depth, 2);
                    }
                }
            }
        }
        return true;
    }
}
