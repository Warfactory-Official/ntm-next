// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.module.mses;

import java.util.ArrayList;
import java.util.List;

record MsesTemplate(String source, List<Part> parts) {
    record Part(String text, boolean variable) {}

    static MsesTemplate parse(String source) {
        List<Part> parts = new ArrayList<>();
        int p = 0;
        while (p < source.length()) {
            int a = source.indexOf('$', p);
            if (a < 0) {
                parts.add(new Part(source.substring(p), false));
                break;
            }
            if (a > p) parts.add(new Part(source.substring(p, a), false));
            int b = source.indexOf('$', a + 1);
            if (b < 0) break;
            parts.add(new Part(source.substring(a + 1, b), true));
            p = b + 1;
        }
        return new MsesTemplate(source, List.copyOf(parts));
    }

    boolean constant() {
        return parts.stream().noneMatch(Part::variable);
    }

    String constantValue() {
        assert constant();
        StringBuilder out = new StringBuilder();
        for (Part part : parts) out.append(part.text());
        return out.toString();
    }
}
