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

package com.hbm.blockentity.door;

public record DoorRow(int x, int y, int z, int length, int width, int axis) {

    public DoorRow {
        if (length == 0 || width < 1 || axis < 0 || axis > 2) {
            throw new IllegalArgumentException(
                    "malformed door row "
                            + x
                            + ","
                            + y
                            + ","
                            + z
                            + " length "
                            + length
                            + " width "
                            + width
                            + " axis "
                            + axis);
        }
    }
}
