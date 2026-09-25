// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.tileentity.BlockEntityLogicBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
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
