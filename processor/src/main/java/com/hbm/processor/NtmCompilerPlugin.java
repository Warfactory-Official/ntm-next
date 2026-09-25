// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.processor;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import mov.movblock.tenon.core.Tenon;

public final class NtmCompilerPlugin implements Plugin {
    @Override
    public String getName() {
        return "hbm-compiler";
    }

    @Override
    public void init(JavacTask task, String... args) {
        String syncResource = null;
        for (String arg : args) {
            if (arg.startsWith("syncTo=")) syncResource = arg.substring("syncTo=".length());
            else throw new IllegalArgumentException("Unknown hbm-compiler argument " + arg);
        }
        Tenon tenon = Tenon.install(task);
        NtmCompilerPasses passes = new NtmCompilerPasses(task, syncResource);
        tenon.listen(-10, passes);
        tenon.transformOutput(10, passes::rewriteSync);
    }
}
