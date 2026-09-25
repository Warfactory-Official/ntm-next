// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

public interface IFlyingCreature {

    int STATE_WALKING = 0;
    int STATE_FLYING = 1;

    int getFlyingState();

    void setFlyingState(int state);
}
