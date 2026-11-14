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

import it.unimi.dsi.fastutil.longs.LongSet;

public abstract class ForeignEnergyNode {

    public abstract Kind kind();

    public abstract Object identity();

    public long inputLimitFe() {
        return Long.MAX_VALUE;
    }

    public long outputLimitFe() {
        return Long.MAX_VALUE;
    }

    public long stockFe() {
        return 0;
    }

    public long capacityFe() {
        return 0;
    }

    public long demandFe() {
        return -1;
    }

    public long emittedFe() {
        return 0;
    }

    public boolean otherAcceptors(LongSet ntmPositions) {
        return true;
    }

    public long towardLimitFe(long probedInsert) {
        return probedInsert;
    }

    public long fromLimitFe(long probedExtract) {
        return probedExtract;
    }

    public enum Kind {
        CONSUMER,
        TRANSIT,
        STORAGE
    }
}
