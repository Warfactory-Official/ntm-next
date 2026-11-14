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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncSource;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

public final class ConsoleScreen implements SyncSource {

    private static final int[] NO_COLUMNS = new int[0];

    public Readout readout;

    {
        hbm$syncSet$ConsoleScreen$readout(Readout.NONE);
    }

    public int[] columns = NO_COLUMNS;
    public @Nullable String text;

    public enum Readout {
        NONE(0, "", ""),
        COL_TEMP(18, "rbmk.screen.temp", "°C"),
        ROD_EXTRACTION(36, "rbmk.screen.rod", "%"),
        FUEL_DEPLETION(54, "rbmk.screen.depletion", "%"),
        FUEL_POISON(72, "rbmk.screen.xenon", "%"),
        FUEL_TEMP(90, "rbmk.screen.core", "°C");

        public static final Readout[] BY_ORDINAL = values();

        private final int atlasU;
        private final String key;
        private final String unit;
        private final String buttonKey = "rbmk.console." + name().toLowerCase(Locale.ROOT);

        Readout(int atlasU, String key, String unit) {
            this.atlasU = atlasU;
            this.key = key;
            this.unit = unit;
        }

        public int atlasU() {
            return atlasU;
        }

        public String key() {
            return key;
        }

        public String unit() {
            return unit;
        }

        public String buttonKey() {
            return buttonKey;
        }

        public Readout next() {
            return BY_ORDINAL[(ordinal() + 1) % BY_ORDINAL.length];
        }
    }

    public final void hbm$syncSet$ConsoleScreen$readout(Readout next) {
        if (this.readout != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.readout);
            this.readout = next;
            if (bound) bindSyncValue(next, 3, 0L);
            if ((0L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 0L);
        }
    }

    public final void hbm$syncSet$ConsoleScreen$text(String next) {
        if (this.text != next) {
            boolean changed = !java.util.Objects.equals(this.text, next);
            this.text = next;
            if (changed)
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(ConsoleScreen.class, "readout", 3, 0L);
        com.hbm.packet.SyncBindings.field(ConsoleScreen.class, "text", 3, 0L);
    }

    private SyncSource hbm$syncOwner;

    private int hbm$syncMask;

    private int hbm$syncBindings;

    private long hbm$syncUnits;

    public final boolean syncBound() {
        return hbm$syncOwner != null;
    }

    public final void syncChanged(int mask) {
        if (hbm$syncOwner == null) return;
        if (hbm$syncUnits == 0) hbm$syncOwner.syncChanged(hbm$syncMask);
        else hbm$syncOwner.syncUnitsChanged(hbm$syncMask, hbm$syncUnits);
    }

    public final void syncUnitsChanged(int mask, long units) {
        syncChanged(mask);
    }

    public final void bindSync(SyncSource owner, int mask) {
        bindSyncUnits(owner, mask, 0);
    }

    public final void bindSyncUnits(SyncSource owner, int mask, long units) {
        if (hbm$syncOwner != null && hbm$syncOwner != owner) {
            throw new IllegalStateException("Mutable sync state has two owners");
        }
        hbm$syncOwner = owner;
        hbm$syncMask |= mask;
        hbm$syncUnits |= units;
        if (hbm$syncBindings++ == 0) SyncBindings.bindFields(this, owner);
    }

    public final void unbindSync(SyncSource owner) {
        assert hbm$syncOwner == owner;
        if (--hbm$syncBindings != 0) return;
        SyncBindings.unbindFields(this, owner);
        hbm$syncOwner = null;
        hbm$syncMask = 0;
        hbm$syncUnits = 0;
    }
}
