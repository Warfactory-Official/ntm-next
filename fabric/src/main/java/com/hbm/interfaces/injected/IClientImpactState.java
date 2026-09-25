// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

public interface IClientImpactState {
    float hbm$impactFire();

    float hbm$impactDust();

    void hbm$setImpactState(float fire, float dust);
}
