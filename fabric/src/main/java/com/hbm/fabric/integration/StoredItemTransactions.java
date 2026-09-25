// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.integration;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

public final class StoredItemTransactions {
    private StoredItemTransactions() {}

    public static boolean isIdle() {
        return !Transaction.isOpen();
    }
}
