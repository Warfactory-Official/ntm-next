/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.capability.port;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.capability.NtmCapabilities;
import com.hbm.util.BitMasks;
import org.jspecify.annotations.Nullable;

public final class EnergyPort implements PortView {

    private static final EnergyHandler[] NO_BUFFERS = new EnergyHandler[0];
    private static final EnergyPort NONE = new EnergyPort(NO_BUFFERS, 0, 0, null);

    private final EnergyHandler[] buffers;
    private final int receive;
    private final int provide;
    private final @Nullable Runnable onChanged;
    private final @Nullable Receiver receiverView;
    private final @Nullable Provider providerView;

    private EnergyPort(
            EnergyHandler[] buffers, int receive, int provide, @Nullable Runnable onChanged) {
        if (((receive | provide) & ~BitMasks.range32(0, buffers.length)) != 0) {
            throw new IllegalArgumentException("energy port names a missing buffer");
        }
        this.buffers = buffers;
        this.receive = receive;
        this.provide = provide;
        this.onChanged = onChanged;
        this.receiverView = receive != 0 ? new Receiver() : null;
        this.providerView = provide != 0 ? new Provider() : null;
    }

    public static EnergyPort none() {
        return NONE;
    }

    public static EnergyPort of(EnergyHandler[] buffers, int receive, int provide) {
        return new EnergyPort(buffers, receive, provide, null);
    }

    public static EnergyPort of(
            EnergyHandler[] buffers, int receive, int provide, Runnable onChanged) {
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

    private abstract class Slots implements EnergyHandler {

        abstract int slotMask();

        @Override
        public long getPower() {
            long total = 0;
            for (var remaining = slotMask(); remaining != 0; remaining &= remaining - 1) {
                total += buffers[Integer.numberOfTrailingZeros(remaining)].getPower();
            }
            return total;
        }

        @Override
        public void setPower(long power) {
            throw new UnsupportedOperationException(
                    "an EnergyPort aggregates several buffers and has no single "
                            + "power level to set; use insertHe/extractHe");
        }

        @Override
        public long getMaxPower() {
            long total = 0;
            for (var remaining = slotMask(); remaining != 0; remaining &= remaining - 1) {
                total += buffers[Integer.numberOfTrailingZeros(remaining)].getMaxPower();
            }
            return total;
        }
    }

    private final class Receiver extends Slots implements EnergyHandler {

        @Override
        int slotMask() {
            return receive;
        }

        @Override
        public long getReceiveRate() {
            long total = 0;
            for (var remaining = receive; remaining != 0; remaining &= remaining - 1) {
                var slot = Integer.numberOfTrailingZeros(remaining);
                total += buffers[slot].getReceiveRate();
            }
            return total;
        }

        @Override
        public long transferPower(long power) {
            long left = power;
            for (var remaining = receive; remaining != 0 && left > 0; remaining &= remaining - 1) {
                var slot = Integer.numberOfTrailingZeros(remaining);
                EnergyHandler buffer = buffers[slot];
                long room = buffer.getMaxPower() - buffer.getPower();
                if (room <= 0) continue;
                long take = Math.min(room, left);
                buffer.setPower(buffer.getPower() + take);
                left -= take;
            }
            if (left != power) changed();
            return left;
        }
    }

    private final class Provider extends Slots implements EnergyHandler {

        @Override
        int slotMask() {
            return provide;
        }

        @Override
        public long getSendRate() {
            long total = 0;
            for (var remaining = provide; remaining != 0; remaining &= remaining - 1) {
                var slot = Integer.numberOfTrailingZeros(remaining);
                total += buffers[slot].getSendRate();
            }
            return total;
        }

        @Override
        public void usePower(long power) {
            long left = power;
            for (var remaining = provide; remaining != 0 && left > 0; remaining &= remaining - 1) {
                var slot = Integer.numberOfTrailingZeros(remaining);
                EnergyHandler buffer = buffers[slot];
                long take = Math.min(buffer.getPower(), left);
                if (take <= 0) continue;
                buffer.setPower(buffer.getPower() - take);
                left -= take;
            }
            if (left != power) changed();
        }
    }
}
