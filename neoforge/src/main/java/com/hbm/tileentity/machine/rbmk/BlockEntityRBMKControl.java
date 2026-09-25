// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlocks;
import com.hbm.entity.projectile.EntityRBMKDebris;
import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKType;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class BlockEntityRBMKControl extends BlockEntityRBMKBase
        implements IControlReceiver, IEnergyHandlerMK2, SyncUnitSchema, IRORValueProvider {
    public static final double speed = 0.00277D;
    public static final long consumption = 5_000;
    public static final long maxPower = consumption * 10;
    public double lastLevel;

    @SyncField(units = 1L << 4)
    public double level;

    @SyncField(units = 1L << 5)
    public double targetLevel;

    @SyncField(units = 1L << 7)
    public boolean hasPower = false;

    @SyncField(units = 1L << 6)
    public long power;

    protected BlockEntityRBMKControl(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0);
    }

    public boolean isPowered() {
        BlockState state = getBlockState();
        return state.is(ModBlocks.RBMK_CONTROL_REASIM.get())
                || state.is(ModBlocks.RBMK_CONTROL_REASIM_AUTO.get());
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return isPowered() ? maxPower : 0;
    }

    @Override
    public ConnectionPriority getPriority() {
        return ConnectionPriority.LOW;
    }

    @Override
    public boolean isLidRemovable() {
        return false;
    }

    @Override
    public void tickClient() {
        this.lastLevel = this.level;
    }

    @Override
    public void tickServer() {

        this.hasPower = !this.isPowered() || this.power >= consumption;

        this.lastLevel = this.level;

        if (this.hasPower) {
            if (level < targetLevel) {
                level += speed * RBMKConfig.getControlSpeed(getLevel());
                if (level > targetLevel) level = targetLevel;
            }
            if (level > targetLevel) {
                level -= speed * RBMKConfig.getControlSpeed(getLevel());
                if (level < targetLevel) level = targetLevel;
            }

            if (this.isPowered() && level != lastLevel) {
                this.power -= consumption;
            }
        }

        super.tickServer();
    }

    public void setTarget(double target) {
        this.targetLevel = Math.clamp(target, 0D, 1D);
    }

    public double getMult() {
        return this.level;
    }

    @Override
    public int trackingRange() {
        return 100;
    }

    @Override
    public void onMelt(int reduce) {
        if (this.isModerated()) {
            int graphite = 2 + getLevel().getRandom().nextInt(2);
            for (int i = 0; i < graphite; i++) spawnDebris(EntityRBMKDebris.DebrisType.GRAPHITE);
        }
        int rods = 2 + getLevel().getRandom().nextInt(2);
        for (int i = 0; i < rods; i++) spawnDebris(EntityRBMKDebris.DebrisType.ROD);
        standardMelt(reduce);
    }

    @Override
    public RBMKType getRBMKType() {
        return RBMKType.CONTROL_ROD;
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())
                < 20 * 20;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        data.getDouble("level").ifPresent(this::setTarget);
    }

    @Override
    public RBMKColumn getConsoleData(RBMKColumn reuse) {
        RBMKColumn.ControlColumn data = (RBMKColumn.ControlColumn) super.getConsoleData(reuse);
        data.level = this.level;
        return data;
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {PREFIX_VALUE + "extraction"};
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "extraction").equals(name)) return "" + (int) (this.level * 100);
        return null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        level = input.getDoubleOr("level", level);
        targetLevel = input.getDoubleOr("targetLevel", targetLevel);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("level", level);
        output.putDouble("targetLevel", targetLevel);
    }

    @Override
    public void writeDiagnostics(CompoundTag tag) {
        super.writeDiagnostics(tag);
        tag.putDouble("level", level);
        tag.putDouble("targetLevel", targetLevel);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0xf0L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 4 -> output.writeDouble(this.level);
            case 5 -> output.writeDouble(this.targetLevel);
            case 6 -> output.writeLong(this.power);
            case 7 -> output.writeBoolean(this.hasPower);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 4 -> this.level = input.readDouble();
            case 5 -> this.targetLevel = input.readDouble();
            case 6 -> this.power = input.readLong();
            case 7 -> this.hasPower = input.readBoolean();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
