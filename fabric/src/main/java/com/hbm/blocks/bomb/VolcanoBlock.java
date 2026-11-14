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

package com.hbm.blocks.bomb;

import com.hbm.blockentity.bomb.VolcanoCoreBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.TickingEntityBlock;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

public class VolcanoBlock extends Block implements TickingEntityBlock {
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);

    public VolcanoBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(MODE, Mode.STATIC_ACTIVE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VolcanoCoreBlockEntity(pos, state);
    }

    public ItemStack stackFor(Mode mode) {
        ItemStack stack = new ItemStack(this);
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(MODE, mode));
        return stack;
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return stackFor(state.getValue(MODE));
    }

    public enum Mode implements StringRepresentable {
        STATIC_ACTIVE(false, false),
        STATIC_EXTINGUISHING(false, true),
        GROWING_ACTIVE(true, false),
        GROWING_EXTINGUISHING(true, true),
        SMOLDERING(false, false);

        private final boolean growing;
        private final boolean extinguishing;

        Mode(boolean growing, boolean extinguishing) {
            this.growing = growing;
            this.extinguishing = extinguishing;
        }

        public boolean growing() {
            return growing;
        }

        public boolean extinguishing() {
            return extinguishing;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static class VolcanoBlockItem extends BlockItem {

        public VolcanoBlockItem(VolcanoBlock block, Properties properties) {
            super(block, properties);
        }

        private static Mode mode(ItemStack stack) {
            Mode mode =
                    stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
                            .get(MODE);
            return mode == null ? Mode.STATIC_ACTIVE : mode;
        }

        @Override
        public void appendHoverText(
                ItemStack stack,
                Item.TooltipContext context,
                TooltipDisplay display,
                Consumer<Component> lines,
                TooltipFlag flag) {
            Mode mode = mode(stack);

            if (mode == Mode.SMOLDERING) {
                lines.accept(
                        Component.translatable("desc.block.volcano.shieldVolcano")
                                .withStyle(ChatFormatting.GOLD));
                return;
            }

            lines.accept(
                    mode.growing()
                            ? Component.translatable("desc.block.volcano.doesGrow")
                                    .withStyle(ChatFormatting.RED)
                            : Component.translatable("desc.block.volcano.doesNotGrow")
                                    .withStyle(ChatFormatting.DARK_GRAY));
            lines.accept(
                    mode.extinguishing()
                            ? Component.translatable("desc.block.volcano.doesExtinguish")
                                    .withStyle(ChatFormatting.RED)
                            : Component.translatable("desc.block.volcano.doesNotExtinguish")
                                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static boolean isRadioactive(BlockState state) {
        return state.is(ModBlocks.VOLCANO_RAD_CORE.get());
    }
}
