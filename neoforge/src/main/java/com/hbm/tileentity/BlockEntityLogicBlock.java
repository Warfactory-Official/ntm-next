// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.world.gen.util.LogicBlockActions;
import com.hbm.world.gen.util.LogicBlockConditions;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityLogicBlock extends BlockEntity {

    public int phase;
    public int timer;
    public String actionID = "FODDER_WAVE";
    public String conditionID = "PLAYER_CUBE_5";
    public @Nullable Direction direction;
    private boolean powered;

    public BlockEntityLogicBlock(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NTM_LOGIC_BLOCK.get(), pos, state);
    }

    public void refreshRedstone() {
        powered = level.hasNeighborSignal(worldPosition);
    }

    public boolean isPowered() {
        return powered;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntityLogicBlock be) {
        be.serverTick((ServerLevel) level, pos);
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
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("phase", phase);
        output.putInt("timer", timer);
        output.putString("actionID", actionID);
        output.putString("conditionID", conditionID);
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
        int dir = input.getIntOr("direction", -1);
        direction = dir < 0 ? null : Direction.from3DDataValue(dir);
        powered = input.getBooleanOr("powered", false);
    }
}
