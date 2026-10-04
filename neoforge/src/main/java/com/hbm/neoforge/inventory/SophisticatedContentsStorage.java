// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.inventory;

import java.util.UUID;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import org.jspecify.annotations.Nullable;

public interface SophisticatedContentsStorage {
    @Nullable ContainerContents storedContents(UUID id);
}
