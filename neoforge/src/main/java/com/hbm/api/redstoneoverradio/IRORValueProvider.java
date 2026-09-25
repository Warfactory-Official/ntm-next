// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.redstoneoverradio;

public interface IRORValueProvider extends IRORInfo {
    String provideRORValue(String name);
}
