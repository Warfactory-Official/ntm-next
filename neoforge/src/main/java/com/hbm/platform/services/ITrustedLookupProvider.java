// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform.services;

import java.lang.invoke.MethodHandles;

public interface ITrustedLookupProvider {
    MethodHandles.Lookup implLookup();
}
