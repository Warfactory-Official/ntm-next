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

package com.hbm.integration.recipeviewer;

import com.hbm.util.RegistryUtil;
import java.util.Locale;
import net.minecraft.resources.Identifier;

public final class PageIds {

    private PageIds() {}

    public static Identifier page(String path) {
        return RegistryUtil.id(path);
    }

    public static Identifier derived(Identifier page, String... segments) {
        return page.withSuffix("/" + String.join("/", segments));
    }

    public static String segment(Identifier id) {
        return id.getNamespace() + "/" + id.getPath();
    }

    public static String segment(Enum<?> constant) {
        return constant.name().toLowerCase(Locale.ROOT);
    }
}
