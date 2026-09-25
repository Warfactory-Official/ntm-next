// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.storage;

import com.hbm.blocks.IPersistentInfoProvider;
import com.hbm.blocks.machine.BlockMachineHorizontal;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemLock;
import com.hbm.tileentity.machine.storage.BlockEntityMassStorage;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class BlockMassStorage extends BlockMachineHorizontal implements IPersistentInfoProvider {

    public final int capacity;

    public BlockMassStorage(Properties props, int capacity) {
        super(props);
        this.capacity = capacity;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMassStorage(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (stack.getItem() instanceof ItemLock || stack.is(ModItems.KEY_KIT.get())) {
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof BlockEntityMassStorage storage
                && storage.canAccess(player)) {
            storage.openMenu(player, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {}

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public void appendPersistentInfo(ItemStack stack, Consumer<Component> adder) {
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container == null) return;
        NonNullList<ItemStack> contents =
                NonNullList.withSize(BlockEntityMassStorage.SLOT_COUNT, ItemStack.EMPTY);
        container.copyInto(contents);
        ItemStack type = contents.get(BlockEntityMassStorage.SLOT_TYPE);
        if (type.isEmpty()) return;
        adder.accept(type.getHoverName().copy().withStyle(ChatFormatting.GOLD));
        adder.accept(
                Component.literal(
                        String.format(
                                Locale.US,
                                "%,d / %,d",
                                stack.getOrDefault(ModDataComponents.MASS_STOCKPILE.get(), 0),
                                capacity)));
    }
}
