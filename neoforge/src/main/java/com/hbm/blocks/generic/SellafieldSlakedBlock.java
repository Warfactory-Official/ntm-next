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

public class SellafieldSlakedBlock extends Block {
    public static int tint(int shade) {
        int brightness = Math.round((1F - shade / 15F) * 255);
        return 0xFF000000 | brightness << 16 | brightness << 8 | brightness;
    }

    public static final MapCodec<SellafieldSlakedBlock> CODEC =
            simpleCodec(SellafieldSlakedBlock::new);

    public static final IntegerProperty SHADE = IntegerProperty.create("shade", 0, 15);

    public SellafieldSlakedBlock(BlockBehaviour.Properties props) {
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
