// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.items.block.ItemBlockBlastInfo;
import com.mojang.serialization.MapCodec;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public final class BlockPlatemetal extends Block {
    public static final MapCodec<BlockPlatemetal> CODEC = simpleCodec(BlockPlatemetal::new);
    public static final EnumProperty<Variant> VARIANT =
            EnumProperty.create("variant", Variant.class);

    public BlockPlatemetal(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, Variant.BASE));
    }

    public ItemStack stackFor(Variant variant) {
        ItemStack stack = new ItemStack(this);
        stack.set(DataComponents.BLOCK_STATE, stateFor(variant));
        return stack;
    }

    public static BlockItemStateProperties stateFor(Variant variant) {
        return BlockItemStateProperties.EMPTY.with(VARIANT, variant);
    }

    public static Variant variant(ItemStack stack) {
        Variant variant =
                stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
                        .get(VARIANT);
        return variant == null ? Variant.BASE : variant;
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return stackFor(state.getValue(VARIANT));
    }

    @Override
    protected MapCodec<BlockPlatemetal> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    public enum Variant implements StringRepresentable {
        BASE,
        BLACK,
        WHITE,
        RED,
        GREEN,
        LIGHT_GRAY,
        BLUE,
        PURPLE,
        CYAN,
        PINK,
        LIME,
        YELLOW,
        LIGHT_BLUE,
        MAGENTA,
        ORANGE;

        public static final Variant[] VALUES = values();

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final class PlatemetalItem extends ItemBlockBlastInfo {
        public PlatemetalItem(BlockPlatemetal block, Properties properties) {
            super(block, properties);
        }

        @Override
        public Component getName(ItemStack stack) {
            Variant variant = variant(stack);
            return Component.translatable(
                    variant == Variant.BASE
                            ? getDescriptionId()
                            : getDescriptionId() + "." + variant.getSerializedName());
        }
    }
}
