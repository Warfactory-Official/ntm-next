// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import com.hbm.capability.MachineCaps;
import org.jspecify.annotations.Nullable;

public interface MachineCapsHolder {

    @Nullable MachineCaps hbm$machineCaps();

    void hbm$machineCaps(MachineCaps caps);
}
