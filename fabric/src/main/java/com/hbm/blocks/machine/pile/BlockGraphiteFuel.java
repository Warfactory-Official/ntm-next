// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.pile;

import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.IBlowable;
import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.pile.BlockEntityPileFuel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockGraphiteFuel extends BlockGraphiteDrilledTE implements IBlowable {

    public BlockGraphiteFuel(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PU239);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        BlockEntityPileFuel pile = new BlockEntityPileFuel(pos, state);
        if (state.getValue(PU239)) pile.progress = BlockEntityPileFuel.maxProgress() - 1000;
        return pile;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(
            BlockState state, Level level, BlockPos pos, Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityPileFuel pile)) return 0;
        return Mth.clamp((pile.progress * 15) / (BlockEntityPileFuel.maxProgress() - 1000), 0, 15);
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
                Item inserted = getInsertedItem(state);
                level.setBlock(
                        pos,
                        copyShape(
                                state, ModBlocks.BLOCK_GRAPHITE_DRILLED.get().defaultBlockState()),
                        3);
                ejectItem(level, pos, side, new ItemStack(inserted));
            }

            if (tool == ToolType.HAND_DRILL
                    && level.getBlockEntity(pos) instanceof BlockEntityPileFuel pile) {
                player.sendSystemMessage(
                        Component.translatable(
                                        "desc.shared.cp1FuelAssembly",
                                        pos.getX(),
                                        pos.getY(),
                                        pos.getZ())
                                .withStyle(ChatFormatting.GOLD));
                player.sendSystemMessage(
                        Component.translatable(
                                        "desc.block.graphiteFuel.heat",
                                        pile.heat + "/" + BlockEntityPileFuel.maxHeat)
                                .withStyle(ChatFormatting.YELLOW));
                player.sendSystemMessage(
                        Component.translatable(
                                        "desc.block.graphiteFuel.depletion",
                                        pile.progress + "/" + BlockEntityPileFuel.maxProgress())
                                .withStyle(ChatFormatting.YELLOW));
                player.sendSystemMessage(
                        Component.translatable("desc.shared.flux", pile.lastNeutrons)
                                .withStyle(ChatFormatting.YELLOW));
                if (state.getValue(PU239)) {
                    player.sendSystemMessage(
                            Component.translatable("desc.block.graphiteFuel.pu239Rich")
                                    .withStyle(ChatFormatting.DARK_GREEN));
                }
            }
        }

        return true;
    }

    @Override
    protected Item getInsertedItem(BlockState state) {
        return state.getValue(PU239)
                ? ModItems.PILE_ROD_PU239.get()
                : ModItems.PILE_ROD_URANIUM.get();
    }

    @Override
    public void applyFan(Level level, BlockPos pos, Direction dir, int dist) {
        if (level.getBlockEntity(pos) instanceof BlockEntityPileFuel pile)
            pile.heat -= pile.heat * 0.025;
    }
}
