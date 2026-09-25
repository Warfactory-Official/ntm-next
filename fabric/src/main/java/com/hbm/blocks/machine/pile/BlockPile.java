// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.pile;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IToolable;
import com.hbm.tileentity.machine.pile.BlockEntityPileCore;
import com.hbm.util.ChunkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockPile extends Block
        implements ITickingBlock, IToolable, ILookOverlay, AssembledMembers.Member {

    public static final EnumProperty<Role> ROLE = EnumProperty.create("role", Role.class);

    public BlockPile(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ROLE, Role.DUMMY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROLE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(ROLE) == Role.CORE ? new BlockEntityPileCore(pos, state) : null;
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        boolean core = state.getValue(ROLE) == Role.CORE;
        BlockPos owner = core ? null : AssembledMembers.owner(level, pos);
        if (!core) AssembledMembers.release(level, pos);
        if (BlockEntityPileCore.meltingDown) return;
        if (!core && owner == null) return;

        if (level.getBlockState(pos).is(ModBlocks.PILE_BRICK.get())) return;
        level.setBlockAndUpdate(pos, ModBlocks.PILE_BRICK.get().defaultBlockState());
        BlockEntityPileCore pile =
                owner == null
                        ? null
                        : ChunkUtil.blockEntityIfLoaded(BlockEntityPileCore.class, level, owner);
        if (pile != null && !pile.isRemoved()) pile.destroy();
    }

    @Override
    public void verify(ServerLevel level, BlockPos pos, BlockState state, BlockPos owner) {
        if (BlockEntityPileCore.meltingDown) return;
        if (level.getBlockEntity(owner) instanceof BlockEntityPileCore) return;
        level.setBlockAndUpdate(pos, ModBlocks.PILE_BRICK.get().defaultBlockState());
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {

        if (tool == ToolType.HAND_DRILL) {

            if (level.getBlockState(pos).getValue(ROLE) == Role.CORE) {
                PileError.send(
                        pos, Component.translatable("marker.hbm.pile.intersect_core"), player);
                return false;
            }

            if (level.isClientSide()) return true;

            BlockPos owner =
                    level instanceof ServerLevel server
                            ? AssembledMembers.owner(server, pos)
                            : null;
            BlockEntityPileCore core =
                    owner == null
                            ? null
                            : ChunkUtil.blockEntityIfLoaded(
                                    BlockEntityPileCore.class, level, owner);
            if (core != null && side != null) {
                return core.drillChannel(pos, side.getOpposite(), player);
            }

            PileError.send(pos, Component.translatable("marker.hbm.pile.no_core"), player);
        }

        return false;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, ILookOverlay.LookInfo info) {
        Role role = level.getBlockState(pos).getValue(ROLE);
        String key =
                switch (role) {
                    case FUEL_IN -> "overlay.hbm.pile.fuel_in";
                    case FUEL_OUT -> "overlay.hbm.pile.fuel_out";
                    case AIR_IN -> "overlay.hbm.pile.air_in";
                    case AIR_OUT -> "overlay.hbm.pile.air_out";
                    case CONTROL -> "overlay.hbm.pile.control";
                    default -> null;
                };
        if (key == null && role != Role.CORE) return;
        info.title(getName().getString(), 0xffff00, 0x404000);
        if (key != null) info.line(Component.translatable(key).getString());
        if (role == Role.CORE && level.getBlockEntity(pos) instanceof BlockEntityPileCore core)
            info.line(
                    Component.translatable(
                                    "overlay.hbm.pile.core.temp",
                                    Math.round(core.highestHeat),
                                    BlockEntityPileCore.MAX_HEAT)
                            .getString());
    }

    public enum Role implements StringRepresentable {
        DUMMY("dummy"),
        CORE("core"),
        CHANNEL("channel"),
        FUEL_IN("fuel_in"),
        FUEL_OUT("fuel_out"),
        AIR_IN("air_in"),
        AIR_OUT("air_out"),
        CONTROL("control"),
        EDGE("edge");

        private final String name;

        Role(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
