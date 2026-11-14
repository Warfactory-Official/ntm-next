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

package com.hbm.client.ctm;

import com.hbm.platform.Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public enum CtmEngine {
    CONTINUITY("continuity", "Continuity", "ctm_continuity"),
    FUSION("fusion", "Fusion", "ctm_fusion"),
    CTM_LIB("ctm", "CTM Lib", "ctm_lib"),
    NONE("", "", "");

    private final String mod;
    private final String title;
    private final String pack;

    CtmEngine(String mod, String title, String pack) {
        this.mod = mod;
        this.title = title;
        this.pack = pack;
    }

    public static CtmEngine selected() {
        return Selection.ENGINE;
    }

    public Identifier packId() {
        return packId("hbm");
    }

    public Identifier packId(String namespace) {
        return Identifier.fromNamespaceAndPath(namespace, pack);
    }

    public String directory() {
        return "resourcepacks/" + packId().getPath();
    }

    public Component title() {
        return Component.translatable("pack.hbm.ctm", title);
    }

    private static final class Selection {
        private static final CtmEngine ENGINE = select();

        private static CtmEngine select() {
            for (CtmEngine engine : values()) {
                if (engine != NONE && Platform.isModLoaded(engine.mod)) return engine;
            }
            return NONE;
        }
    }
}
