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

package com.hbm.api.client;

import com.hbm.platform.Platform;
import net.irisshaders.iris.api.v0.IrisApi;

public final class ShaderPacks {
    private static final boolean IRIS = Platform.isModLoaded("iris");

    private ShaderPacks() {}

    public static boolean inUse() {
        return IRIS && Iris.inUse();
    }

    private static final class Iris {
        private static boolean inUse() {
            return IrisApi.getInstance().isShaderPackInUse();
        }
    }
}
