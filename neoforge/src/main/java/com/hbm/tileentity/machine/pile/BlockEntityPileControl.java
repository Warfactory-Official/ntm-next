// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.pile;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.machine.pile.BlockEntityPileCore.PileChannel;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityPileControl extends BlockEntityPileDeviceBase
        implements SyncUnitSchema, IRORInteractive {
    public static final double SPEED = 1D / 60D;
    private static final String[] ROR = {
        PREFIX_FUNCTION + "setrods" + NAME_SEPARATOR + "percent",
        PREFIX_FUNCTION + "extendrods" + NAME_SEPARATOR + "percent"
    };

    @SyncField(units = 1L)
    public double extension;

    public double lastExtension;
    private double syncExtension;
    private int turnProgress;
    public double targetExtension;
    private boolean wasRedstone;

    public BlockEntityPileControl(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_CONTROL.get(), pos, state, 0);
    }

    @Override
    public void tickServer() {
        PileChannel channel = attachedChannel(BlockPile.Role.CONTROL);
        if (channel != null) {
            channel.control = extension;
            if (extension != targetExtension) {
                if (Math.abs(extension - targetExtension) <= SPEED) extension = targetExtension;
                else extension += Math.copySign(SPEED, targetExtension - extension);
                setChanged();
            }
        }

        Direction facing = orientation();
        boolean redstone =
                level.getSignal(worldPosition.relative(facing), facing.getOpposite()) > 0;
        if (redstone != wasRedstone) setTarget(redstone ? 1D : 0D);
        wasRedstone = redstone;
        networkPackNT(100);
    }

    @Override
    public void tickClient() {
        lastExtension = extension;
        if (turnProgress > 0) {
            extension += (syncExtension - extension) / turnProgress;
            turnProgress--;
        } else extension = syncExtension;
    }

    public void setTarget(double target) {
        targetExtension = Math.clamp(target, 0D, 1D);
        setChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setrods").equals(name) && params.length > 0) {
            setTarget(IRORInteractive.parseInt(params[0], 0, 100) / 100D);
        } else if ((PREFIX_FUNCTION + "extendrods").equals(name) && params.length > 0) {
            setTarget(targetExtension + IRORInteractive.parseInt(params[0], -100, 100) / 100D);
        }
        return null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        extension = input.getDoubleOr("level", 0D);
        targetExtension = input.getDoubleOr("targetLevel", 0D);
        wasRedstone = input.getBooleanOr("redstone", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("level", extension);
        output.putDouble("targetLevel", targetExtension);
        output.putBoolean("redstone", wasRedstone);
    }

    @Override
    public long syncUnitMask() {
        return 5L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeDouble(extension);
            case 2 -> output.writeInt(channelNumber);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> {
                double next = input.readDouble();
                if (syncExtension != next) turnProgress = 2;
                syncExtension = next;
            }
            case 2 -> channelNumber = input.readInt();
            default -> throw new IllegalArgumentException();
        }
    }
}
