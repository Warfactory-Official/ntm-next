// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blockentity.BlockEntityLogicBlock;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.world.gen.util.LogicBlockInteractions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

@RefreshesNeighborState(be = BlockEntityLogicBlock.class, calling = "refreshRedstone")
public class LogicBlock extends Block implements ITickingBlock {

    public static final MapCodec<LogicBlock> CODEC = simpleCodec(LogicBlock::new);
    public static final EnumProperty<Disguise> DISGUISE =
            EnumProperty.create("disguise", Disguise.class);

    public LogicBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(DISGUISE, Disguise.NONE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DISGUISE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityLogicBlock(pos, state);
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
        return stack.isEmpty()
                ? InteractionResult.TRY_WITH_EMPTY_HAND
                : interact(level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(level, pos, player);
    }

    private static InteractionResult interact(Level level, BlockPos pos, Player player) {
        BlockEntityLogicBlock logic = (BlockEntityLogicBlock) level.getBlockEntity(pos);
        if (logic.interactionID == null || logic.interactionID.isEmpty())
            return InteractionResult.PASS;
        if (level instanceof ServerLevel server)
            LogicBlockInteractions.interact(server, logic, player);
        return InteractionResult.SUCCESS;
    }

    public enum Disguise implements StringRepresentable {
        NONE("none"),
        CRATE("crate"),
        CRATE_WEAPON("crate_weapon"),
        CRATE_IRON("crate_iron"),
        DECO_RUSTY_STEEL("deco_rusty_steel"),
        DECO_STEEL("deco_steel"),
        DECO_TUNGSTEN("deco_tungsten"),
        MACHINE_CONTROLLER("machine_controller");

        private final String name;

        Disguise(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
