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

public final class ClientEventConstants {

    public static final float GAS_FLAME_DEFAULT_SCALE = 6.5F;

    public static final int GLYPHID_PATH_MAX_NODES = 128;
    public static final int FALLING_BLOCK_BATCH_MAX_CHILDREN = 2_048;

    public static final int JETPACK_DNS = 0;
    public static final int JETPACK_BJ = 1;
    public static final int JETPACK_REGULAR = 2;
    public static final int JETPACK_VECTOR = 3;

    public static final int PLAYER_INFORM_ID_DETONATOR = 8;
    public static final int PLAYER_INFORM_ID_TOOL_ABILITY = 11;
    public static final int PLAYER_INFORM_ID_GAS_HAZARD = 12;
    public static final int PLAYER_INFORM_DEFAULT_MILLIS = 1_000;

    private ClientEventConstants() {}
}
