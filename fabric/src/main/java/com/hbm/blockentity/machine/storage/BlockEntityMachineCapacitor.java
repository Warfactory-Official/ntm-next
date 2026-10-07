// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.machine.storage;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.FoldedCoreResident;
import com.hbm.blockentity.GraphResident;
import com.hbm.blockentity.PersistentDrop;
import com.hbm.blockentity.Synced;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.storage.MachineCapacitor;
import com.hbm.items.ModDataComponents;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineCapacitor extends BlockEntity
        implements Synced,
                SyncUnitSchema,
                GraphResident,
                FoldedCoreResident,
                IEnergyHandlerMK2,
                PersistentDrop,
                IRORValueProvider {

    private static final String[] PERSISTENT_KEYS = {"power", "maxPower"};
    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "fill", PREFIX_VALUE + "fillpercent",
            };

    @SyncField(units = 1L)
    private long power;

    @SyncField(units = 1L)
    private long maxPower;

    @SyncField(units = 1L)
    private long powerReceived;

    @SyncField(units = 1L)
    private long powerSent;

    private long lastPowerReceived;
    private long lastPowerSent;
    private long counted = Long.MIN_VALUE;

    public BlockEntityMachineCapacitor(BlockPos pos, BlockState state) {
        this(pos, state, 0L);
    }

    public BlockEntityMachineCapacitor(BlockPos pos, BlockState state, long maxPower) {
        super(ModBlockEntities.CAPACITOR.get(), pos, state);
        this.maxPower = maxPower;
    }

    public void tickServer() {
        getMaxPower();
        networkPackNT(15);
        rollover();
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, getMaxPower()));
    }

    private void rollover() {
        long now = level.getGameTime();
        if (now == counted) return;
        boolean previous = now == counted + 1;
        lastPowerSent = previous ? powerSent : 0;
        lastPowerReceived = previous ? powerReceived : 0;
        powerSent = 0;
        powerReceived = 0;
        counted = now;
    }

    public long lastPowerReceived() {
        rollover();
        return lastPowerReceived;
    }

    public long lastPowerSent() {
        rollover();
        return lastPowerSent;
    }

    public long powerReceivedThisTick() {
        return powerReceived;
    }

    public long powerSentThisTick() {
        return powerSent;
    }

    @Override
    public long syncUnitMask() {
        return 1L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit != 0) throw new IllegalArgumentException();
        output.writeLong(power);
        output.writeLong(maxPower);
        output.writeLong(powerReceived);
        output.writeLong(powerSent);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit != 0) throw new IllegalArgumentException();
        power = input.readLong();
        maxPower = input.readLong();
        powerReceived = input.readLong();
        powerSent = input.readLong();
    }

    @Override
    public long transferPower(long power, boolean simulate) {
        long overshoot = IEnergyHandlerMK2.super.transferPower(power, simulate);
        if (!simulate) {
            rollover();
            powerReceived += power - overshoot;
        }
        return overshoot;
    }

    @Override
    public void usePower(long power) {
        rollover();
        powerSent += Math.min(getPower(), power);
        setPower(getPower() - power);
        if (power != 0) markChanged();
    }

    @Override
    public long getMaxPower() {
        if (maxPower == 0L && getBlockState().getBlock() instanceof MachineCapacitor capacitor) {
            maxPower = capacitor.maxPower;
        }
        return maxPower;
    }

    @Override
    public long getProviderSpeed() {
        return getMaxPower() / 300L;
    }

    @Override
    public long getReceiverSpeed() {
        return getMaxPower() / 100L;
    }

    public boolean acceptsFace(Direction dir) {
        return dir == getBlockState().getValue(DirectionalBlock.FACING);
    }

    @Override
    public ConnectionPriority getPriority() {
        return ConnectionPriority.LOW;
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + power;
        if ((PREFIX_VALUE + "fillpercent").equals(name)) {
            long ceiling = getMaxPower();
            if (ceiling == 0) throw new ArithmeticException();
            long hundreds = ceiling / 100;
            long remainder = ceiling % 100;
            int low = 0;
            int high = 100;
            while (low < high) {
                int middle = (low + high + 1) >>> 1;
                long threshold = hundreds * middle + (remainder * middle + 99) / 100;
                if (power >= threshold) low = middle;
                else high = middle - 1;
            }
            return Integer.toString(low);
        }
        return null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        maxPower = input.getLongOr("maxPower", maxPower);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        components.set(
                ModDataComponents.CAPACITOR_STATE.get(), new CapacitorCharge(power, maxPower));
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        CapacitorCharge state = components.get(ModDataComponents.CAPACITOR_STATE.get());
        if (state == null) return;
        power = state.power();
        maxPower = state.maxPower();
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }
}
