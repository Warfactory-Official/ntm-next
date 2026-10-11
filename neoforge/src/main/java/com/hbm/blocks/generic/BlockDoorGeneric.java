// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blockentity.BlockEntityDoorGeneric;
import com.hbm.blockentity.DoorDecl;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.config.NeoForgeHbmConfig;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.IToolable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BlockDoorGeneric extends BlockMultiblockCore
        implements ITickingBlock, IBomb, IToolable {
    public static final IntegerProperty SKIN = IntegerProperty.create("skin", 0, 6);

    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    public final DoorDecl type;

    private final boolean sealsRadiation;

    public BlockDoorGeneric(Properties props, DoorDecl type) {
        this(props, type, false);
    }

    public BlockDoorGeneric(Properties props, DoorDecl type, boolean sealsRadiation) {
        super(props);
        this.type = type;
        this.sealsRadiation = sealsRadiation;
        registerDefaultState(defaultBlockState().setValue(SKIN, 0).setValue(OPEN, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SKIN, OPEN);
    }

    @Override
    public boolean cellsSealRadiation() {
        return sealsRadiation;
    }

    @Override
    public boolean cellsOpen() {
        return true;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityDoorGeneric(pos, state);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return doorShape(state, level, pos, false);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return doorShape(state, level, pos, true);
    }

    private VoxelShape doorShape(
            BlockState state, BlockGetter level, BlockPos pos, boolean forCollision) {
        boolean open =
                level.getBlockEntity(pos) instanceof BlockEntityDoorGeneric door
                        && door.state != BlockEntityDoorGeneric.STATE_CLOSED;
        return coreShape(state.getValue(FACING), open, forCollision);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (NeoForgeHbmConfig.runtime().ultraLarpMode() && type != DoorDecl.WATER_DOOR)
            return InteractionResult.SUCCESS;
        return level.getBlockEntity(core) instanceof BlockEntityDoorGeneric door
                        && door.tryToggle(player)
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER || !player.isShiftKeyDown()) return false;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityDoorGeneric door) || !type.hasSkins())
            return false;
        if (level.isClientSide()) return true;
        door.cycleSkinIndex();
        return true;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (!type.remoteControllable()) return BombReturnCode.ERROR_INCOMPATIBLE;
        return level.getBlockEntity(pos) instanceof BlockEntityDoorGeneric door
                        && door.tryToggle(null)
                ? BombReturnCode.TRIGGERED
                : BombReturnCode.ERROR_INCOMPATIBLE;
    }

    @Override
    public boolean wantsNeighborUpdates() {
        return true;
    }

    @Override
    public void cellNeighborChanged(ServerLevel level, BlockPos core, BlockPos cell) {
        if (level.getBlockEntity(core) instanceof BlockEntityDoorGeneric door)
            door.updateRedstonePower(cell);
    }
}
