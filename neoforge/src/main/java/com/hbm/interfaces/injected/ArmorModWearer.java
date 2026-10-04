// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

public interface ArmorModWearer {
    void hbm$reconcileArmorMods();

    void hbm$tickArmorMods();

    void hbm$tickClientArmorMods();
}
