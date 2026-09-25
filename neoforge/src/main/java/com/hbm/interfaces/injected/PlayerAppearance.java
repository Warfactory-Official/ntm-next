// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

public interface PlayerAppearance {

    byte MANLY = 1;
    byte STEALTH = 2;

    byte hbm$appearance();

    void hbm$setAppearance(byte flags);
}
