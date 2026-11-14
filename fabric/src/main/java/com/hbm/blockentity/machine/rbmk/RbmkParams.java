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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.blocks.machine.rbmk.RBMKBlock;
import com.hbm.config.BalanceConfig;
import com.hbm.data.MachineData;

public final class RbmkParams {
    private static int height = 4;

    private RbmkParams() {}

    public static boolean reasimBoilers() {
        return MachineData.RBMK_REASIM_BOILERS.get() || BalanceConfig.enable528ReasimBoilers;
    }

    public static int columnHeight() {
        return height;
    }

    public static int heightAboveCore() {
        return height - 1;
    }

    public static void publishColumnHeight() {
        int published = MachineData.RBMK_COLUMN_HEIGHT.get();
        if (published == height) return;
        height = published;
        RBMKBlock.rebakeColumns();
    }
}
