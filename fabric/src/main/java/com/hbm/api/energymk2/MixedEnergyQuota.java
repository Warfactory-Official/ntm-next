/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.api.energymk2;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

public final class MixedEnergyQuota {

    private MixedEnergyQuota() {}

    public static long reserve(
            MixedEnergyPlan plan,
            int slot,
            long fe,
            long quantum,
            TransactionContext transaction,
            long[] reserved) {
        long request = Math.min(fe, plan.availableFe(slot));
        request -= request % quantum;
        if (request <= 0) return 0;
        journal(plan).updateSnapshots(transaction);
        reserved[0] = plan.reserveFe(slot, request);
        return request;
    }

    public static void settle(MixedEnergyPlan plan, int slot, long reserved, long acceptedFe) {
        plan.refund(slot, reserved, acceptedFe);
    }

    private static Journal journal(MixedEnergyPlan plan) {
        if (plan.loaderStaging instanceof Journal journal) return journal;
        Journal journal = new Journal(plan);
        plan.loaderStaging = journal;
        return journal;
    }

    private static final class Journal extends SnapshotParticipant<Integer> {
        private final MixedEnergyPlan plan;

        Journal(MixedEnergyPlan plan) {
            this.plan = plan;
        }

        @Override
        protected Integer createSnapshot() {
            return plan.checkpoint();
        }

        @Override
        protected void readSnapshot(Integer snapshot) {
            plan.rollback(snapshot);
        }

        @Override
        protected void onFinalCommit() {
            plan.committed();
        }
    }
}
