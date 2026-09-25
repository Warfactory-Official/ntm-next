// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.entity.missile.EntityMissileBaseNT;
import com.hbm.inventory.container.MenuLaunchPad;
import com.hbm.packet.SyncField;
import com.hbm.particle.HbmParticles;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class BlockEntityLaunchPad extends BlockEntityLaunchPadBase {

    private static final int LAUNCH_OFFSET = 1;
    private static final int RELOAD_TICKS = 100;

    @SyncField(units = 1L << 5)
    private int reloadDelay;

    public BlockEntityLaunchPad(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCH1.get(), pos, state);
    }

    @Override
    public void tickServer() {
        if (reloadDelay > 0) reloadDelay--;

        if (!isMissileValid(getItem(SLOT_MISSILE)) || !hasFuel()) reloadDelay = RELOAD_TICKS;

        if (!hasFuel() || !isMissileValid(getItem(SLOT_MISSILE))) {
            state = STATE_MISSING;
        } else {
            state = reloadDelay > 0 ? STATE_LOADING : STATE_READY;
        }

        tickShared();
    }

    @Override
    public void tickClient() {
        if (level.getEntitiesOfClass(
                        EntityMissileBaseNT.class,
                        new AABB(
                                worldPosition.getX() - .5D,
                                worldPosition.getY(),
                                worldPosition.getZ() - .5D,
                                worldPosition.getX() + 1.5D,
                                worldPosition.getY() + 10D,
                                worldPosition.getZ() + 1.5D))
                .isEmpty()) return;
        Direction facing = BlockMultiblockCore.coreFacing(getBlockState());
        for (int i = 0; i < 15; i++) {
            Direction direction = facing;
            if (level.getRandom().nextBoolean()) direction = direction.getOpposite();
            if (level.getRandom().nextBoolean()) direction = direction.getClockWise();
            double motionX = level.getRandom().nextGaussian() * .15D + .75D;
            double motionZ = level.getRandom().nextGaussian() * .15D + .75D;
            level.addParticle(
                    HbmParticles.LAUNCH_SMOKE.get(),
                    true,
                    false,
                    worldPosition.getX() + .5D,
                    worldPosition.getY() + .25D,
                    worldPosition.getZ() + .5D,
                    motionX * direction.getStepX(),
                    0D,
                    motionZ * direction.getStepZ());
        }
    }

    public void refreshRedstone() {
        BlockPos core = getBlockPos();
        boolean powered = false;
        for (int dx = -1; dx <= 1 && !powered; dx++) {
            for (int dz = -1; dz <= 1 && !powered; dz++) {
                if (getLevel().hasNeighborSignal(core.offset(dx, 0, dz))) powered = true;
            }
        }
        redstone = powered;
    }

    @Override
    public boolean isReadyForLaunch() {
        return reloadDelay <= 0;
    }

    @Override
    protected double getLaunchOffset() {
        return LAUNCH_OFFSET;
    }

    @Override
    protected void afterLaunch() {
        reloadDelay = RELOAD_TICKS;
    }

    public int reloadDelay() {
        return reloadDelay;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 5;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit == 5) output.writeInt(reloadDelay);
        else super.writeSyncUnit(unit, output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit == 5) reloadDelay = input.readInt();
        else super.readSyncUnit(unit, input);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        reloadDelay = input.getIntOr("reloadDelay", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("reloadDelay", reloadDelay);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuLaunchPad(containerId, inventory, this);
    }
}
