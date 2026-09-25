// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.energymk2;

import com.hbm.uninos.IBufferedEndpoint;

public interface IEnergyHandlerMK2 extends IBufferedEndpoint {

    long getPower();

    void setPower(long power);

    long getMaxPower();

    default void usePower(long power) {
        this.setPower(this.getPower() - power);
    }

    default long getProviderSpeed() {
        return this.getMaxPower();
    }

    default long transferPower(long power, boolean simulate) {
        if (power + this.getPower() <= this.getMaxPower()) {
            if (!simulate) this.setPower(power + this.getPower());
            return 0;
        }
        long capacity = this.getMaxPower() - this.getPower();
        long overshoot = power - capacity;
        if (!simulate) this.setPower(this.getMaxPower());
        return overshoot;
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
