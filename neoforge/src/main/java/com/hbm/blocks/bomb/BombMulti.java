// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.blocks.machine.BlockMachineHorizontal;
import com.hbm.entity.effect.EntityMist;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.interfaces.IBomb;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.tileentity.bomb.BlockEntityBombMulti.Payload;
import com.hbm.tileentity.bomb.BlockEntityBombMulti;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BombMulti extends BlockMachineHorizontal implements IBomb {

    private static final float BASE_STRENGTH = 8.0F;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    public BombMulti(Properties props) {
        super(props);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getCounterClockWise());
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityBombMulti(pos, state);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (level.isClientSide()) return;
        if (level.hasNeighborSignal(pos)) explode(level, pos, null);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (!(level instanceof ServerLevel server)) return BombReturnCode.UNDEFINED;
        if (!(server.getBlockEntity(pos) instanceof BlockEntityBombMulti bomb)
                || !bomb.isLoaded()) {
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }

        float strength = BASE_STRENGTH;
        int cluster = 0;
        int fire = 0;
        int poison = 0;
        int gas = 0;
        for (int slot : BlockEntityBombMulti.PAYLOAD_SLOTS) {
            switch (bomb.payload(slot)) {
                case GUNPOWDER -> strength += 1.0F;
                case TNT -> strength += 4.0F;
                case CLUSTER -> cluster += 50;
                case FIRE -> fire += 10;
                case POISON -> poison += 15;
                case GAS -> gas += 50;
                case NONE -> {}
            }
        }

        bomb.clearSlots();
        server.removeBlock(pos, false);

        ExplosionLarge.explode(
                server, pos.getX(), pos.getY(), pos.getZ(), strength, true, true, true);

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;

        if (cluster > 0) {
            ExplosionChaos.cluster(
                    server,
                    x,
                    y,
                    z,
                    cluster,
                    0F,
                    (float) Math.PI * 0.5F,
                    (float) Math.PI * 2F,
                    (float) Math.PI * 0.125F,
                    0.375F);
        }
        if (fire > 0) {
            ExplosionChaos.igniteAllBlocks(server, pos.getX(), pos.getY(), pos.getZ(), fire);
        }
        if (poison > 0) {
            ExplosionNukeGeneric.wasteNoSchrab(server, pos, poison, server.getRandom());
        }
        if (gas > 0) {
            EntityMist mist = new EntityMist(server);
            mist.setType(NTMFluids.CHLORINE);
            mist.setPos(x, y, z);
            mist.setArea(gas * 15F / 50F, gas * 7.5F / 50F);
            server.addFreshEntity(mist);
        }
        return BombReturnCode.DETONATED;
    }

    public static float yawFor(Direction facing) {
        return switch (facing) {
            case SOUTH -> (float) Math.PI;
            case WEST -> (float) (Math.PI * 1.5);
            case EAST -> (float) (Math.PI * 0.5);
            default -> 0F;
        };
    }
}
