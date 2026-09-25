// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;

public class BlockSellafieldSlaked extends Block {

    public static final MapCodec<BlockSellafieldSlaked> CODEC =
            simpleCodec(BlockSellafieldSlaked::new);

    public static final IntegerProperty SHADE = IntegerProperty.create("shade", 0, 15);

    public BlockSellafieldSlaked(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(SHADE, 0));
    }

    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(5.0F)
                .sound(SoundType.STONE)
                .noLootTable()
                .requiresCorrectToolForDrops();
    }

    public static BlockBehaviour.Properties sellafieldBedrockProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(-1.0F, 3_600_000.0F)
                .sound(SoundType.STONE)
                .isValidSpawn((state, level, pos, type) -> false)
                .noLootTable();
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHADE);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {

        if (this == ModBlocks.SELLAFIELD_BEDROCK.get()) return List.of();
        return List.of(new ItemStack(this.asItem()));
    }
}
