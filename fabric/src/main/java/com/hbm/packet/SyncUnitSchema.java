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

package com.hbm.packet;

import io.netty.buffer.ByteBuf;

public interface SyncUnitSchema {
    long syncUnitMask();

    default boolean initialMatchesSyncUnits() {
        return true;
    }

    default long syncEventUnits() {
        return 0;
    }

    void writeSyncUnit(int unit, ByteBuf output);

    void readSyncUnit(int unit, ByteBuf input);

    default void writeInitialSyncUnit(int unit, ByteBuf output) {
        writeSyncUnit(unit, output);
    }

    default void writeInitialExtras(ByteBuf output) {}

    default void readInitialExtras(ByteBuf input) {}

    default void afterSyncUnits(long units) {}

    default void afterInitialSyncUnits() {
        afterSyncUnits(syncUnitMask());
    }

    default boolean afterRelayedSyncUnits(long units, boolean initial) {
        return false;
    }
}
