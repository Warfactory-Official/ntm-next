// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.pile;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.pile.BlockEntityPileBreedingFuel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockGraphiteBreedingFuel extends BlockGraphiteDrilledTE {

    public BlockGraphiteBreedingFuel(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPileBreedingFuel(pos, state);
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {

        if (!level.isClientSide()) {

            BlockState state = level.getBlockState(pos);

            if (tool == ToolType.SCREWDRIVER && isChannelFace(state, side)) {

                level.setBlock(
                        pos,
                        copyShape(
                                state, ModBlocks.BLOCK_GRAPHITE_DRILLED.get().defaultBlockState()),
                        3);
                ejectItem(level, pos, side, new ItemStack(ModItems.PILE_ROD_LITHIUM));
            }

            if (tool == ToolType.HAND_DRILL
                    && level.getBlockEntity(pos) instanceof BlockEntityPileBreedingFuel pile) {
                player.sendSystemMessage(
                        Component.translatable(
                                        "desc.shared.cp1FuelAssembly",
                                        pos.getX(),
                                        pos.getY(),
                                        pos.getZ())
                                .withStyle(ChatFormatting.GOLD));
                player.sendSystemMessage(
                        Component.literal(
                                        "DEPLETION: "
                                                + pile.progress
                                                + "/"
                                                + BlockEntityPileBreedingFuel.maxProgress())
                                .withStyle(ChatFormatting.YELLOW));
                player.sendSystemMessage(
                        Component.translatable("desc.shared.flux", pile.lastNeutrons)
                                .withStyle(ChatFormatting.YELLOW));
            }
        }

        return true;
    }

    @Override
    public Item getInsertedItem() {
        return ModItems.PILE_ROD_LITHIUM.get();
    }
}
