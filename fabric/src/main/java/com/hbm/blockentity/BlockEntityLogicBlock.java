// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.world.gen.util.LogicBlockActions;
import com.hbm.world.gen.util.LogicBlockConditions;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityLogicBlock extends BlockEntity implements Synced, SyncUnitSchema {

    public int phase;
    public int timer;
    public String actionID = "FODDER_WAVE";
    public String conditionID = "PLAYER_CUBE_5";

    @SyncField(units = 1L)
    public @Nullable String interactionID;

    public @Nullable Direction direction;
    private boolean powered;

    public BlockEntityLogicBlock(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NTM_LOGIC_BLOCK.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntityLogicBlock be) {
        be.serverTick((ServerLevel) level, pos);
    }

    public void refreshRedstone() {
        powered = level.hasNeighborSignal(worldPosition);
    }

    public boolean isPowered() {
        return powered;
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
        Consumer<BlockEntityLogicBlock> action = LogicBlockActions.ACTIONS.get(actionID);
        Predicate<BlockEntityLogicBlock> condition =
                LogicBlockConditions.CONDITIONS.get(conditionID);

        if (action == null || condition == null) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            return;
        }

        action.accept(this);
        if (condition.test(this)) {
            phase++;
            timer = 0;
        } else {
            timer++;
        }
        if (!isRemoved()) networkPackNTTracking();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("phase", phase);
        output.putInt("timer", timer);
        output.putString("actionID", actionID);
        output.putString("conditionID", conditionID);
        if (interactionID != null) output.putString("interactionID", interactionID);
        output.putInt("direction", direction == null ? -1 : direction.get3DDataValue());
        output.putBoolean("powered", powered);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        phase = input.getIntOr("phase", 0);
        timer = input.getIntOr("timer", 0);
        actionID = input.getStringOr("actionID", actionID);
        conditionID = input.getStringOr("conditionID", conditionID);
        interactionID = input.getString("interactionID").orElse(null);
        int dir = input.getIntOr("direction", -1);
        direction = dir < 0 ? null : Direction.from3DDataValue(dir);
        powered = input.getBooleanOr("powered", false);
    }

    @Override
    public long syncUnitMask() {
        return 1L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit != 0) throw new IllegalArgumentException();
        output.writeBoolean(interactionID != null);
        if (interactionID != null) ByteBufCodecs.STRING_UTF8.encode(output, interactionID);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit != 0) throw new IllegalArgumentException();
        interactionID = input.readBoolean() ? ByteBufCodecs.STRING_UTF8.decode(input) : null;
    }
}
