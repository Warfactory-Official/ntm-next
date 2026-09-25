// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability.port;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.capability.NtmCapabilities;
import org.jspecify.annotations.Nullable;

public final class EnergyPort implements PortView {

    private static final IEnergyHandlerMK2[] NO_BUFFERS = new IEnergyHandlerMK2[0];
    private static final int[] NO_SLOTS = new int[0];
    private static final EnergyPort NONE = new EnergyPort(NO_BUFFERS, NO_SLOTS, NO_SLOTS, null);

    private final IEnergyHandlerMK2[] buffers;
    private final int[] receive;
    private final int[] provide;
    private final @Nullable Runnable onChanged;
    private final @Nullable Receiver receiverView;
    private final @Nullable Provider providerView;

    private EnergyPort(
            IEnergyHandlerMK2[] buffers,
            int[] receive,
            int[] provide,
            @Nullable Runnable onChanged) {
        this.buffers = buffers;
        this.receive = receive;
        this.provide = provide;
        this.onChanged = onChanged;
        this.receiverView = receive.length > 0 ? new Receiver() : null;
        this.providerView = provide.length > 0 ? new Provider() : null;
    }

    public static EnergyPort none() {
        return NONE;
    }

    public static EnergyPort of(IEnergyHandlerMK2[] buffers, int[] receive, int[] provide) {
        return new EnergyPort(buffers, receive, provide, null);
    }

    public static EnergyPort of(
            IEnergyHandlerMK2[] buffers, int[] receive, int[] provide, Runnable onChanged) {
        return new EnergyPort(buffers, receive, provide, onChanged);
    }

    @Override
    public <T> @Nullable T as(Class<T> type, NtmCapabilities.CapRole role) {
        if (role == NtmCapabilities.CapRole.POWER_IN && receiverView != null)
            return type.cast(receiverView);
        if (role == NtmCapabilities.CapRole.POWER_OUT && providerView != null)
            return type.cast(providerView);
        return null;
    }

    private void changed() {
        if (onChanged != null) onChanged.run();
    }

    private abstract class Slots implements IEnergyHandlerMK2 {

        abstract int[] slots();

        @Override
        public long getPower() {
            long total = 0;
            for (int slot : slots()) total += buffers[slot].getPower();
            return total;
        }

        @Override
        public void setPower(long power) {
            throw new UnsupportedOperationException(
                    "an EnergyPort aggregates several buffers and has no single "
                            + "power level to set; use transferPower/usePower");
        }

        @Override
        public long getMaxPower() {
            long total = 0;
            for (int slot : slots()) total += buffers[slot].getMaxPower();
            return total;
        }
    }

    private final class Receiver extends Slots implements IEnergyHandlerMK2 {

        @Override
        int[] slots() {
            return receive;
        }

        @Override
        public long getReceiverSpeed() {
            long total = 0;
            for (int slot : receive) {
                total += buffers[slot].getReceiverSpeed();
            }
            return total;
        }

        @Override
        public long transferPower(long power, boolean simulate) {
            long left = power;
            for (int slot : receive) {
                if (left <= 0) break;
                IEnergyHandlerMK2 buffer = buffers[slot];
                long room = buffer.getMaxPower() - buffer.getPower();
                if (room <= 0) continue;
                long take = Math.min(room, left);
                if (!simulate) buffer.setPower(buffer.getPower() + take);
                left -= take;
            }
            if (!simulate && left != power) changed();
            return left;
        }
    }

    private final class Provider extends Slots implements IEnergyHandlerMK2 {

        @Override
        int[] slots() {
            return provide;
        }

        @Override
        public long getProviderSpeed() {
            long total = 0;
            for (int slot : provide) {
                total += buffers[slot].getProviderSpeed();
            }
            return total;
        }

        @Override
        public void usePower(long power) {
            long left = power;
            for (int slot : provide) {
                if (left <= 0) break;
                IEnergyHandlerMK2 buffer = buffers[slot];
                long take = Math.min(buffer.getPower(), left);
                if (take <= 0) continue;
                buffer.setPower(buffer.getPower() - take);
                left -= take;
            }
            if (left != power) changed();
        }
    }
}
