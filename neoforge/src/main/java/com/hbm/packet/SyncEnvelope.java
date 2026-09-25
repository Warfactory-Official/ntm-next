// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet;

public interface SyncEnvelope {
    void bindSyncChildren(SyncSource owner, int mask);

    void unbindSyncChildren(SyncSource owner);
}
