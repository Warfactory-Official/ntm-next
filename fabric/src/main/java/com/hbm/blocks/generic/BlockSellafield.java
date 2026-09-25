// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.potion.HbmPotion;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jspecify.annotations.Nullable;

public class BlockSellafield extends Block {

    public static final MapCodec<BlockSellafield> CODEC = simpleCodec(BlockSellafield::new);

    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 5);

    public BlockSellafield(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    public static final int LEVELS = 6;

    public ItemStack stackFor(int level) {
        ItemStack stack = new ItemStack(this);
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(LEVEL, level));
        return stack;
    }

    public static int level(ItemStack stack) {
        Integer level =
                stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
                        .get(LEVEL);
        return level == null ? 0 : level;
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return stackFor(state.getValue(LEVEL));
    }

    public static class SellafieldBlockItem extends BlockItem {

        public SellafieldBlockItem(BlockSellafield block, Properties properties) {
            super(block, properties);
        }

        @Override
        public Component getName(ItemStack stack) {
            int level = level(stack);
            return Component.translatable(
                    level == 0 ? getDescriptionId() : getDescriptionId() + "." + level);
        }
    }

    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(5.0F)
                .sound(SoundType.STONE)
                .randomTicks()
                .noLootTable()
                .requiresCorrectToolForDrops();
    }

    @Override
    protected MapCodec<BlockSellafield> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int lvl = state.getValue(LEVEL);
        if (random.nextInt(lvl == 0 ? 25 : 15) == 0) {
            if (lvl > 0) {
                level.setBlock(pos, state.setValue(LEVEL, lvl - 1), 2);
            } else {
                level.setBlock(pos, ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState(), 3);
            }
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            int lvl = state.getValue(LEVEL);
            living.addEffect(
                    new MobEffectInstance(HbmPotion.radiation(), 30 * 20, lvl < 5 ? lvl : lvl * 2));
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(stackFor(state.getValue(LEVEL)));
    }
}
