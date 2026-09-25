// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.pile;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.pile.BlockEntityPileNeutronDetector;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
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

public class BlockGraphiteNeutronDetector extends BlockGraphiteDrilledTE {

    public BlockGraphiteNeutronDetector(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WITHDRAWN);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPileNeutronDetector(pos, state);
    }

    public void triggerRods(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        boolean withdrawn = state.getValue(WITHDRAWN);

        level.setBlock(pos, state.setValue(WITHDRAWN, !withdrawn), 3);

        level.playSound(
                null,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                ModSounds.TECH_BLEEP.get(),
                SoundSource.BLOCKS,
                0.02F,
                1.0F);

        Direction dir =
                Direction.fromAxisAndDirection(
                        state.getValue(AXIS), Direction.AxisDirection.NEGATIVE);
        BlockGraphiteRod.toggleRun(level, pos, state, dir, !withdrawn);
        BlockGraphiteRod.toggleRun(level, pos, state, dir.getOpposite(), !withdrawn);
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

            if (tool == ToolType.SCREWDRIVER) {

                if (!player.isShiftKeyDown()) {
                    if (isChannelFace(state, side)) {
                        level.setBlock(
                                pos,
                                copyShape(
                                        state,
                                        ModBlocks.BLOCK_GRAPHITE_DRILLED.get().defaultBlockState()),
                                3);
                        ejectItem(level, pos, side, new ItemStack(getInsertedItem()));
                    }
                } else if (level.getBlockEntity(pos)
                        instanceof BlockEntityPileNeutronDetector pile) {
                    player.sendSystemMessage(
                            Component.translatable(
                                            "desc.shared.cp1FuelAssembly",
                                            pos.getX(),
                                            pos.getY(),
                                            pos.getZ())
                                    .withStyle(ChatFormatting.GOLD));
                    player.sendSystemMessage(
                            Component.translatable(
                                            "desc.shared.flux",
                                            pile.lastNeutrons + "/" + pile.maxNeutrons)
                                    .withStyle(ChatFormatting.YELLOW));
                }
            }

            if (tool == ToolType.DEFUSER
                    && level.getBlockEntity(pos) instanceof BlockEntityPileNeutronDetector pile) {
                if (player.isShiftKeyDown()) {
                    if (pile.maxNeutrons > 1) pile.maxNeutrons--;
                } else {
                    pile.maxNeutrons++;
                }
                pile.setChanged();
            }
        }

        return true;
    }

    @Override
    public Item getInsertedItem() {
        return ModItems.PILE_ROD_DETECTOR.get();
    }
}
