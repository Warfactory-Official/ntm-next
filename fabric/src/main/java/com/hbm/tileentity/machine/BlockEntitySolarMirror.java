// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntitySolarMirror extends BlockEntityMachineBase implements SyncUnitSchema {

    @SyncField(units = 1L << 0)
    public int tX;

    @SyncField(units = 1L << 0)
    public int tY;

    @SyncField(units = 1L << 0)
    public int tZ;

    @SyncField(units = 1L << 1)
    public boolean isOn;

    public BlockEntitySolarMirror(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLARMIRROR.get(), pos, state, 0);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm.solar_mirror");
    }

    @Override
    public void tickServer() {

        if (tY < worldPosition.getY()) {
            setOff();
            return;
        }

        int sun = level.getBrightness(LightLayer.SKY, worldPosition) - level.getSkyDarken() - 11;

        if (sun <= 0 || !level.canSeeSky(worldPosition.above())) {
            setOff();
            return;
        }

        if (!isOn) {
            isOn = true;
            networkPackNT(200);
        }

        BlockEntity te = level.getBlockEntity(new BlockPos(tX, tY - 1, tZ));
        if (te instanceof BlockEntitySolarBoiler boiler) {
            boiler.heat += sun;
        }
    }

    @Override
    public void tickClient() {

        if (isOn
                && level.getBlockEntity(new BlockPos(tX, tY - 1, tZ))
                        instanceof BlockEntitySolarBoiler boiler) {
            boiler.primary.add(worldPosition);
        }
    }

    private void setOff() {
        if (isOn) {
            isOn = false;
            networkPackNT(200);
        }
    }

    public void setTarget(int x, int y, int z) {
        tX = x;
        tY = y;
        tZ = z;
        setChanged();
        networkPackNT(200);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tX = input.getIntOr("targetX", 0);
        tY = input.getIntOr("targetY", 0);
        tZ = input.getIntOr("targetZ", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("targetX", tX);
        output.putInt("targetY", tY);
        output.putInt("targetZ", tZ);
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> {
                output.writeInt(this.tX);
                output.writeInt(this.tY);
                output.writeInt(this.tZ);
            }
            case 1 -> output.writeBoolean(this.isOn);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> {
                this.tX = input.readInt();
                this.tY = input.readInt();
                this.tZ = input.readInt();
            }
            case 1 -> this.isOn = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
