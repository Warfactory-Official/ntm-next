// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.items.ModItems;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;

public class BlockWasteLog extends RotatedPillarBlock {

    public static final MapCodec<BlockWasteLog> CODEC =
            simpleCodec(props -> new BlockWasteLog(props, () -> ModItems.BURNT_BARK.get()));

    private final Supplier<Item> barkDrop;

    public BlockWasteLog(BlockBehaviour.Properties props, Supplier<Item> barkDrop) {
        super(props);
        this.barkDrop = barkDrop;
    }

    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(5.0F, 1.5F)
                .sound(SoundType.WOOD)
                .noLootTable()
                .ignitedByLava();
    }

    @Override
    public MapCodec<BlockWasteLog> codec() {
        return CODEC;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        RandomSource random = params.getLevel().getRandom();
        if (random.nextInt(1000) == 0) {
            return List.of(new ItemStack(barkDrop.get()));
        }
        return List.of(new ItemStack(Items.COAL, 2 + random.nextInt(3)));
    }
}
