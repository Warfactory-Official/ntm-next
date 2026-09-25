// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlocks;
import com.hbm.tileentity.bomb.BlockEntityVolcanoCore;
import com.mojang.serialization.MapCodec;
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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

public class BlockVolcano extends Block implements ITickingBlock {

    public static final MapCodec<BlockVolcano> CODEC = simpleCodec(BlockVolcano::new);

    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);

    public BlockVolcano(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(MODE, Mode.STATIC_ACTIVE));
    }

    @Override
    protected MapCodec<BlockVolcano> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityVolcanoCore(pos, state);
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

        public VolcanoBlockItem(BlockVolcano block, Properties properties) {
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
