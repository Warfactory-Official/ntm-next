// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import com.hbm.items.weapon.sedna.GunTimers;
import org.jetbrains.annotations.Nullable;

public interface GunTickState {

    @Nullable
    GunTimers hbm$gunTimers();

    void hbm$setGunTimers(@Nullable GunTimers timers);
}
