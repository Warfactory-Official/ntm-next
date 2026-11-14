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

public interface SyncSource {
    boolean syncBound();

    void syncChanged(int mask);

    default void syncUnitsChanged(int mask, long units) {
        syncChanged(mask);
    }

    default void bindSyncValue(Object value, int mask, long units) {
        SyncBindings.bindUnits(this, value, mask, units);
    }

    default void syncArrayChanged(Object value, int index, int mask, long units) {
        if (index >= 0 && syncBound() && value instanceof Object[] array)
            bindSyncValue(array[index], mask, units);
        if (units == 0) syncChanged(mask);
        else syncUnitsChanged(mask, units);
    }

    void bindSync(SyncSource owner, int mask);

    default void bindSyncUnits(SyncSource owner, int mask, long units) {
        bindSync(owner, mask);
    }

    void unbindSync(SyncSource owner);
}
