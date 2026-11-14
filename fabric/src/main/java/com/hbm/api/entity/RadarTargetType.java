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

package com.hbm.api.entity;

public enum RadarTargetType {
    TIER0(0),
    TIER1(1),
    TIER2(2),
    TIER3(3),
    TIER4(4),
    TIER10(5),
    TIER10_15(6),
    TIER15(7),
    TIER15_20(8),
    TIER20(9),
    ANTI_BALLISTIC(10),
    PLAYER(11),
    ARTILLERY(12),
    SPECIAL(13);

    private final int id;

    RadarTargetType(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }
}
