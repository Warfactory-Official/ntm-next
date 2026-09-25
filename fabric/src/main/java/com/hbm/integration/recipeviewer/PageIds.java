// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.lib.Library;
import java.util.Locale;
import net.minecraft.resources.Identifier;

public final class PageIds {

    private PageIds() {}

    public static Identifier page(String path) {
        return Library.id(path);
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
