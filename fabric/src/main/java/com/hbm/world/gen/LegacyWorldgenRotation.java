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

package com.hbm.world.gen;

public final class LegacyWorldgenRotation {
    private LegacyWorldgenRotation() {}

    public static float sine(float angle) {
        return sample((int) (angle * 10430.378F) & 65535);
    }

    public static float cosine(float angle) {
        return sample((int) (angle * 10430.378F + 16384.0F) & 65535);
    }

    private static float sample(int index) {
        return (float) Math.sin((double) index * Math.PI * 2.0D / 65536.0D);
    }
}
