// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces;

public interface NtmDamageContext {
    float hbm$pierceDT();

    float hbm$pierceDR();

    void hbm$setPiercing(float dt, float dr);

    double hbm$knockbackMultiplier();

    void hbm$setKnockbackMultiplier(double multiplier);

    boolean hbm$ignoreEarlyCancellation();

    void hbm$setIgnoreEarlyCancellation(boolean ignore);
}
