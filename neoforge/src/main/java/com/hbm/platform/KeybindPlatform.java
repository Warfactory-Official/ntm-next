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

package com.hbm.platform;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.InputQuirks;

public class KeybindPlatform {
    public static boolean sameChord(KeyMapping first, KeyMapping second) {
        return !first.isUnbound()
                && first.getKey().equals(second.getKey())
                && first.getKeyModifier() == second.getKeyModifier();
    }

    public static boolean modifierMatches(KeyMapping mapping, int modifiers) {
        int mask =
                switch (mapping.getKeyModifier()) {
                    case NONE -> 0;
                    case SHIFT -> InputConstants.MOD_SHIFT;
                    case CONTROL -> InputConstants.MOD_CONTROL;
                    case CONTROL_OR_COMMAND ->
                            InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY
                                    ? InputConstants.MOD_SUPER
                                    : InputConstants.MOD_CONTROL;
                    case ALT -> InputConstants.MOD_ALT;
                };
        return mask == 0 || (modifiers & mask) != 0;
    }
}
