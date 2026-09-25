// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability.port;

import com.hbm.capability.NtmCapabilities;
import org.jspecify.annotations.Nullable;

public interface PortView {

    <T> @Nullable T as(Class<T> type, NtmCapabilities.CapRole role);
}
