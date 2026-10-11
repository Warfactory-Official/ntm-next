// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.energymk2;

import com.hbm.uninos.IBufferedEndpoint;
import com.hbm.util.ChunkUtil;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IEnergyHandlerMK2 extends IBufferedEndpoint {

    long getPower();

    void setPower(long power);

    long getMaxPower();

    default void usePower(long power) {
        this.setPower(this.getPower() - power);
        if (power != 0) energyChanged();
    }

    default long getProviderSpeed() {
        return this.getMaxPower();
    }

    default long transferPower(long power, boolean simulate) {
        if (power + this.getPower() <= this.getMaxPower()) {
            if (!simulate) {
                this.setPower(power + this.getPower());
                if (power != 0) energyChanged();
            }
            return 0;
        }
        long capacity = this.getMaxPower() - this.getPower();
        long overshoot = power - capacity;
        if (!simulate) {
            this.setPower(this.getMaxPower());
            if (capacity != 0) energyChanged();
        }
        return overshoot;
    }

    default void energyChanged() {
        if (this instanceof BlockEntity be) ChunkUtil.markUnsaved(be);
    }

    default long getReceiverSpeed() {
        return this.getMaxPower();
    }

    default ConnectionPriority getPriority() {
        return ConnectionPriority.NORMAL;
    }

    default boolean allowDirectProvision() {
        return true;
    }

    enum ConnectionPriority {
        LOWEST,
        LOW,
        NORMAL,
        HIGH,
        HIGHEST;

        public static final ConnectionPriority[] VALUES = {LOWEST, LOW, NORMAL, HIGH, HIGHEST};

        public static ConnectionPriority storage(int ordinal) {
            if (ordinal < LOW.ordinal()) return LOW;
            return ordinal > HIGH.ordinal() ? HIGH : VALUES[ordinal];
        }

        public static ConnectionPriority nextStorage(ConnectionPriority current) {
            int next = current.ordinal() + 1;
            return next > HIGH.ordinal() ? LOW : storage(next);
        }
    }
}
