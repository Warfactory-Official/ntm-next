// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.toclient.RbmkJetPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityZirnoxDestroyed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class ZirnoxDestroyed extends BlockMultiblockCore implements ITickingBlock {

    private static final int[] DIMENSIONS = {1, 0, 2, 2, 2, 2};

    private static final int FLAME_CHANCE = 4;
    private static final int FLAME_MAX_AGE = 90;
    private static final int FLAME_RANGE = 75;

    public ZirnoxDestroyed(Properties props) {
        super(props);
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!(level instanceof ServerLevel server) || state.is(oldState.getBlock())) return;

        RandomSource rand = server.getRandom();
        if (rand.nextInt(FLAME_CHANCE) != 0) return;

        flame(server, pos);
        server.playSound(
                null,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                SoundEvents.FIRE_AMBIENT,
                SoundSource.BLOCKS,
                1.0F + rand.nextFloat(),
                rand.nextFloat() * 0.7F + 0.3F);
    }

    public static void flame(ServerLevel level, BlockPos pos) {
        RandomSource rand = level.getRandom();
        double px = pos.getX() + 0.25D + rand.nextDouble() * 0.5D;
        double py = pos.getY() + 1.75D;
        double pz = pos.getZ() + 0.25D + rand.nextDouble() * 0.5D;
        Services.NETWORK.sendToAllAround(
                RbmkJetPayload.flame(px, py, pz, FLAME_MAX_AGE),
                new TargetPoint(level, pos.getX() + 0.5D, py, pos.getZ() + 0.5D, FLAME_RANGE));
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityZirnoxDestroyed(pos, state);
    }
}
